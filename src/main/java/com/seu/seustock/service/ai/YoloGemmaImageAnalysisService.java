package com.seu.seustock.service.ai;

import com.seu.seustock.model.dto.ImageAnalysisDTO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class YoloGemmaImageAnalysisService implements ImageAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(YoloGemmaImageAnalysisService.class);

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final ImageResizeService imageResizeService;
    private final YoloDetectionClient yoloDetectionClient;
    private final GemmaVisionClient gemmaVisionClient;

    @Override
    public ImageAnalysisDTO analyze(MultipartFile imageFile) {
        return analyze(imageFile, 0, null, null);
    }

    @Override
    public ImageAnalysisDTO analyze(MultipartFile imageFile,
                                    int retryAttempt,
                                    String previousName,
                                    String previousDescription) {
        validate(imageFile);

        try {
            log.info("[YoloGemmaImageAnalysisService] 분석 시작 - filename={}, contentType={}, size={}",
                    imageFile.getOriginalFilename(), imageFile.getContentType(), imageFile.getSize());
            ImageResizeService.ResizedImage resized =
                    imageResizeService.resizeForAnalysis(imageFile.getBytes(), imageFile.getContentType());
            List<YoloDetection> detections = yoloDetectionClient.detect(resized.bytes(), resized.mimeType());
            return gemmaVisionClient.analyze(resized.bytes(),
                    resized.mimeType(),
                    retryAttempt,
                    previousName,
                    previousDescription,
                    detections);
        } catch (IOException e) {
            log.error("[YoloGemmaImageAnalysisService] 이미지 파일 읽기 실패", e);
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
}
