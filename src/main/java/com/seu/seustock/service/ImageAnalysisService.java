package com.seu.seustock.service;

import com.seu.seustock.model.dto.ImageAnalysisDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ImageAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ImageAnalysisService.class);

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final int MAX_RETRY_ATTEMPT = 3;

    private final ChatClient chatClient;

    public ImageAnalysisService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public ImageAnalysisDTO analyze(MultipartFile imageFile) {
        return analyze(imageFile, 0, null, null);
    }

    public ImageAnalysisDTO analyze(MultipartFile imageFile,
                                    int retryAttempt,
                                    String previousName,
                                    String previousDescription) {
        validate(imageFile);
        int normalizedRetryAttempt = Math.clamp(retryAttempt, 0, MAX_RETRY_ATTEMPT);

        BeanOutputConverter<ImageAnalysisDTO> outputConverter =
                new BeanOutputConverter<>(ImageAnalysisDTO.class);

        try {
            log.info("[ImageAnalysisService] Ollama 호출 시작 — retryAttempt={}", normalizedRetryAttempt);
            ResizedImage ri = resizeForAnalysis(imageFile.getBytes(), imageFile.getContentType());
            ByteArrayResource resource = new ByteArrayResource(ri.bytes());
            OllamaChatOptions.Builder optionsBuilder = OllamaChatOptions.builder()
                    .temperature(temperatureFor(normalizedRetryAttempt));
            if (normalizedRetryAttempt > 0) {
                optionsBuilder.seed(ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE));
            }

            ImageAnalysisDTO result = chatClient.prompt()
                    .options(optionsBuilder)
                    .system(systemPrompt(normalizedRetryAttempt))
                    .user(user -> user
                            .text(userPrompt(
                                    normalizedRetryAttempt, previousName, previousDescription, outputConverter))
                            .media(MimeType.valueOf(ri.mimeType()), resource))
                    .call()
                    .entity(outputConverter);

            log.info("[ImageAnalysisService] Ollama 응답 수신 — result={}", result);
            if (result == null) {
                throw new IllegalStateException("이미지 분석 결과가 비어 있습니다.");
            }
            return result;
        } catch (IOException e) {
            log.error("[ImageAnalysisService] 이미지 파일 읽기 실패", e);
            throw new IllegalStateException("이미지 파일을 읽을 수 없습니다.", e);
        }
    }

    private String systemPrompt(int retryAttempt) {
        String base = """
                당신은 재고 관리 앱의 이미지 분석 도우미입니다.
                이미지를 분석하여 요청한 JSON 형식으로만 응답하세요.
                모든 텍스트 값은 반드시 한국어로 작성하세요.
                이미지에서 확인되지 않는 브랜드·모델·수량은 절대 추측하지 마세요.
                """;
        if (retryAttempt == 0) {
            return base;
        }
        return base + """
                이 요청은 사용자가 이전 분석 결과가 마음에 들지 않아 다시 요청한 retry입니다.
                같은 물품이라는 판단은 유지하되, 이전 결과와 같은 이름·설명 표현을 반복하지 마세요.
                더 일반적인 명칭, 더 구체적인 명칭, 다른 시각적 특징 중심 설명 중 하나를 선택하세요.
                """;
    }

    private String userPrompt(int retryAttempt,
                              String previousName,
                              String previousDescription,
                              BeanOutputConverter<ImageAnalysisDTO> outputConverter) {
        String previousResult = "";
        if (retryAttempt > 0) {
            previousResult = """

                    이전 분석 결과:
                    - name: %s
                    - description: %s

                    위 표현을 그대로 반복하지 말고, 이미지에서 확인되는 범위 안에서 다른 후보를 제안해주세요.
                    """.formatted(blankToDash(previousName), blankToDash(previousDescription));
        }

        return """
                이미지의 물품을 분석하여 아래 필드를 한국어로 채워주세요.

                - name: 물품을 나타내는 짧은 이름 (5단어 이내, 예: USB 충전기, 드라이버 세트)
                - description: 색상·형태·소재·상태·포장 등 눈으로 확인되는 특징
                %s

                %s
                """.formatted(previousResult, outputConverter.getFormat());
    }

    private double temperatureFor(int retryAttempt) {
        return switch (retryAttempt) {
            case 0 -> 0.1;
            case 1 -> 0.35;
            case 2 -> 0.55;
            default -> 0.7;
        };
    }

    private String blankToDash(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.strip();
    }

    private void validate(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("분석할 이미지 파일이 없습니다.");
        }

        String contentType = imageFile.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다.");
        }
    }

    private record ResizedImage(byte[] bytes, String mimeType) {}

    private static final int MAX_SIDE = 1024;

    private ResizedImage resizeForAnalysis(byte[] original, String originalMimeType) throws IOException {
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(original));
        if (src == null) {
            log.warn("[ImageAnalysisService] 리사이즈 불가 포맷({}), 원본 전송", originalMimeType);
            return new ResizedImage(original, originalMimeType);
        }

        int w = src.getWidth();
        int h = src.getHeight();

        BufferedImage dst;
        if (w <= MAX_SIDE && h <= MAX_SIDE) {
            log.debug("[ImageAnalysisService] 리사이즈 생략, JPEG 변환만 수행 ({}x{})", w, h);
            dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = dst.createGraphics();
            g.drawImage(src, 0, 0, null);
            g.dispose();
        } else {
            double scale = (double) MAX_SIDE / Math.max(w, h);
            int nw = (int) Math.round(w * scale);
            int nh = (int) Math.round(h * scale);
            log.debug("[ImageAnalysisService] 리사이즈 {}x{} → {}x{}", w, h, nw, nh);
            dst = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = dst.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, nw, nh, null);
            g.dispose();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(dst, "jpeg", baos);
        return new ResizedImage(baos.toByteArray(), "image/jpeg");
    }
}
