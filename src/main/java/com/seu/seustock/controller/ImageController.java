package com.seu.seustock.controller;

import com.seu.seustock.model.dto.ImageDTO;
import com.seu.seustock.model.dto.ImageAnalysisDTO;
import com.seu.seustock.service.ImageAnalysisService;
import com.seu.seustock.service.ImageStorageService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    private final ImageStorageService imageStorageService;
    private final ImageAnalysisService imageAnalysisService;

    @GetMapping("/images/{externalId}")
    public ResponseEntity<Resource> show(@PathVariable UUID externalId, HttpSession session) {
        String username = (String) session.getAttribute("loginUser");
        ImageDTO image = imageStorageService.loadForUser(externalId, username);
        Resource resource = imageStorageService.load(image);
        MediaType contentType = image.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(image.getContentType());
        String disposition = ContentDisposition.inline()
                .filename(image.getOriginalFilename() == null ? "image" : image.getOriginalFilename(),
                        StandardCharsets.UTF_8)
                .build()
                .toString();
        return ResponseEntity.ok()
                .contentType(contentType)
                .header("Content-Disposition", disposition)
                .body(resource);
    }

    @PostMapping("/images/analyze")
    public ResponseEntity<ImageAnalysisDTO> analyze(@RequestParam("imageFile") MultipartFile imageFile,
                                                    @RequestParam(defaultValue = "0") int retryAttempt,
                                                    @RequestParam(required = false) String previousName,
                                                    @RequestParam(required = false) String previousDescription) {
        log.info("[analyze] 요청 수신 — filename={}, contentType={}, size={}, retryAttempt={}",
                imageFile.getOriginalFilename(), imageFile.getContentType(), imageFile.getSize(), retryAttempt);
        ImageAnalysisDTO result = imageAnalysisService.analyze(
                imageFile, retryAttempt, previousName, previousDescription);
        log.info("[analyze] 분석 완료 — name={}, description={}",
                result.getName(), result.getDescription());
        return ResponseEntity.ok(result);
    }
}
