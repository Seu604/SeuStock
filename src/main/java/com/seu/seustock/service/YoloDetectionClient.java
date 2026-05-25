package com.seu.seustock.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class YoloDetectionClient {

    private static final Logger log = LoggerFactory.getLogger(YoloDetectionClient.class);

    private final RestClient restClient;
    private final boolean enabled;
    private final int imageSize;
    private final double confidenceThreshold;
    private final double iouThreshold;

    public YoloDetectionClient(RestClient.Builder restClientBuilder,
                               @Value("${seustock.ai.yolo.base-url:http://localhost:8000}") String baseUrl,
                               @Value("${seustock.ai.yolo.enabled:false}") boolean enabled,
                               @Value("${seustock.ai.yolo.image-size:640}") int imageSize,
                               @Value("${seustock.ai.yolo.confidence-threshold:0.25}") double confidenceThreshold,
                               @Value("${seustock.ai.yolo.iou-threshold:0.7}") double iouThreshold) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.enabled = enabled;
        this.imageSize = imageSize;
        this.confidenceThreshold = confidenceThreshold;
        this.iouThreshold = iouThreshold;
    }

    public List<YoloDetection> detect(byte[] imageBytes, String mimeType) {
        if (!enabled) {
            return List.of();
        }

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("imageFile", new NamedByteArrayResource(imageBytes, "analysis-image.jpg"));
            body.add("imageSize", imageSize);
            body.add("confidenceThreshold", confidenceThreshold);
            body.add("iouThreshold", iouThreshold);

            YoloDetectionResponse response = restClient.post()
                    .uri("/detect")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(YoloDetectionResponse.class);

            if (response == null || response.detections() == null) {
                return List.of();
            }
            return response.detections().stream()
                    .map(YoloDetectionPayload::toDetection)
                    .toList();
        } catch (Exception e) {
            log.warn("[YoloDetectionClient] YOLO 호출 실패, Gemma 단독 분석으로 fallback합니다.", e);
            return List.of();
        }
    }

    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record YoloDetectionResponse(List<YoloDetectionPayload> detections) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record YoloDetectionPayload(String label,
                                        Double confidence,
                                        Double x1,
                                        Double y1,
                                        Double x2,
                                        Double y2) {
        private YoloDetection toDetection() {
            return new YoloDetection(label, confidence, x1, y1, x2, y2);
        }
    }
}
