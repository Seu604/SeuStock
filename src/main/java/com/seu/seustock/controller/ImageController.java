package com.seu.seustock.controller;

import com.seu.seustock.model.dto.ImageDTO;
import com.seu.seustock.model.dto.ImageAnalysisDTO;
import com.seu.seustock.service.ai.ImageAnalysisService;
import com.seu.seustock.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Controller
@RequiredArgsConstructor
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

    private final ImageStorageService imageStorageService;
    private final ImageAnalysisService imageAnalysisService;
    @Qualifier("aiAnalysisExecutor")
    private final Executor aiAnalysisExecutor;

    @GetMapping("/images/{externalId}")
    public ResponseEntity<Resource> show(@PathVariable UUID externalId, Principal principal) {
        String username = principal.getName();
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
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic().immutable())
                .eTag(externalId.toString())
                .contentType(contentType)
                .header("Content-Disposition", disposition)
                .body(resource);
    }

    @PostMapping("/images/{externalId}/analyze")
    public CompletableFuture<ResponseEntity<ImageAnalysisDTO>> analyzeStored(
            @PathVariable UUID externalId,
            @RequestParam(defaultValue = "0") int retryAttempt,
            @RequestParam(required = false) String previousName,
            @RequestParam(required = false) String previousDescription,
            Principal principal) {
        String username = principal.getName();
        ImageDTO image = imageStorageService.loadForUser(externalId, username);
        Resource resource = imageStorageService.load(image);
        MultipartFile multipartFile = new StoredImageMultipartFile(image, resource);
        log.info("[analyzeStored] externalId={}, retryAttempt={}", externalId, retryAttempt);
        return CompletableFuture.supplyAsync(() -> {
            ImageAnalysisDTO result = imageAnalysisService.analyze(
                    multipartFile, retryAttempt, previousName, previousDescription);
            log.info("[analyzeStored] 완료 — name={}, description={}", result.getName(), result.getDescription());
            return result;
        }, aiAnalysisExecutor)
        .thenApply(ResponseEntity::ok)
        .exceptionally(ex -> {
            log.error("[analyzeStored] 오류", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        });
    }

    @PostMapping("/images/analyze")
    public CompletableFuture<ResponseEntity<ImageAnalysisDTO>> analyze(
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam(defaultValue = "0") int retryAttempt,
            @RequestParam(required = false) String previousName,
            @RequestParam(required = false) String previousDescription) {
        log.info("[analyze] 요청 수신 — filename={}, contentType={}, size={}, retryAttempt={}",
                imageFile.getOriginalFilename(), imageFile.getContentType(), imageFile.getSize(), retryAttempt);
        return CompletableFuture.supplyAsync(() -> {
            ImageAnalysisDTO result = imageAnalysisService.analyze(
                    imageFile, retryAttempt, previousName, previousDescription);
            log.info("[analyze] 분석 완료 — name={}, description={}",
                    result.getName(), result.getDescription());
            return result;
        }, aiAnalysisExecutor)
        .thenApply(ResponseEntity::ok)
        .exceptionally(ex -> {
            log.error("[analyze] 분석 중 오류 발생", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        });
    }

    private record StoredImageMultipartFile(ImageDTO image, Resource resource) implements MultipartFile {
        @Override public String getName() { return "imageFile"; }
        @Override public String getOriginalFilename() { return image.getOriginalFilename(); }
        @Override public String getContentType() { return image.getContentType(); }
        @Override public boolean isEmpty() {
            try { return resource.contentLength() == 0; } catch (IOException e) { return false; }
        }
        @Override public long getSize() {
            try { return resource.contentLength(); } catch (IOException e) { return 0; }
        }
        @Override public byte[] getBytes() throws IOException {
            return resource.getInputStream().readAllBytes();
        }
        @Override public InputStream getInputStream() throws IOException {
            return resource.getInputStream();
        }
        @Override public void transferTo(java.io.File dest) throws IOException {
            throw new UnsupportedOperationException();
        }
    }
}
