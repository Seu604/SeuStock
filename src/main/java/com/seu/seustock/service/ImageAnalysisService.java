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

@Service
public class ImageAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ImageAnalysisService.class);

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final ChatClient chatClient;

    public ImageAnalysisService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public ImageAnalysisDTO analyze(MultipartFile imageFile) {
        validate(imageFile);

        BeanOutputConverter<ImageAnalysisDTO> outputConverter =
                new BeanOutputConverter<>(ImageAnalysisDTO.class);

        try {
            log.info("[ImageAnalysisService] Ollama 호출 시작");
            ResizedImage ri = resizeForAnalysis(imageFile.getBytes(), imageFile.getContentType());
            ByteArrayResource resource = new ByteArrayResource(ri.bytes());
            ImageAnalysisDTO result = chatClient.prompt()
                    .options(OllamaChatOptions.builder().temperature(0.1))
                    .system("""
                            당신은 재고 관리 앱의 이미지 분석 도우미입니다.
                            이미지를 분석하여 요청한 JSON 형식으로만 응답하세요.
                            모든 텍스트 값은 반드시 한국어로 작성하세요.
                            이미지에서 확인되지 않는 브랜드·모델·수량은 절대 추측하지 마세요.
                            """)
                    .user(user -> user
                            .text("""
                                    이미지의 물품을 분석하여 아래 필드를 한국어로 채워주세요.

                                    - name: 물품을 나타내는 짧은 이름 (5단어 이내, 예: USB 충전기, 드라이버 세트)
                                    - description: 색상·형태·소재·상태·포장 등 눈으로 확인되는 특징

                                    %s
                                    """.formatted(outputConverter.getFormat()))
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
        if (w <= MAX_SIDE && h <= MAX_SIDE) {
            log.debug("[ImageAnalysisService] 리사이즈 생략 ({}x{})", w, h);
            return new ResizedImage(original, originalMimeType);
        }

        double scale = (double) MAX_SIDE / Math.max(w, h);
        int nw = (int) Math.round(w * scale);
        int nh = (int) Math.round(h * scale);
        log.debug("[ImageAnalysisService] 리사이즈 {}x{} → {}x{}", w, h, nw, nh);

        BufferedImage dst = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(dst, "jpeg", baos);
        return new ResizedImage(baos.toByteArray(), "image/jpeg");
    }
}
