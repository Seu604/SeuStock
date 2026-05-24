package com.seu.seustock.service;

import com.seu.seustock.model.dto.ImageAnalysisDTO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@Service
public class ImageAnalysisService {

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
            ByteArrayResource resource = imageResource(imageFile);
            ImageAnalysisDTO result = chatClient.prompt()
                    .system("""
                            You analyze product or inventory images for a stock management application.
                            Return concise Korean text suitable for item registration fields.
                            Do not invent brand, model, or quantity details that are not visible.
                            """)
                    .user(user -> user
                            .text("""
                                    Analyze this image and fill these fields:
                                    - name: short item name
                                    - description: visible features, condition, color, shape, packaging, or likely category
                                    - memo: useful storage or handling notes, or an empty string when there is nothing notable

                                    %s
                                    """.formatted(outputConverter.getFormat()))
                            .media(MimeType.valueOf(imageFile.getContentType()), resource))
                    .call()
                    .entity(outputConverter);

            if (result == null) {
                throw new IllegalStateException("이미지 분석 결과가 비어 있습니다.");
            }
            return result;
        } catch (IOException e) {
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

    private ByteArrayResource imageResource(MultipartFile imageFile) throws IOException {
        return new ByteArrayResource(imageFile.getBytes()) {
            @Override
            public String getFilename() {
                return imageFile.getOriginalFilename();
            }
        };
    }
}
