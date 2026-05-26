package com.seu.seustock.service;

import com.seu.seustock.mapper.ImageMapper;
import com.seu.seustock.mapper.UserMapper;
import com.seu.seustock.model.dto.ImageDTO;
import com.seu.seustock.model.dto.UserDTO;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Primary
@Service
@RequiredArgsConstructor
public class MinioImageStorageService implements ImageStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final ImageMapper imageMapper;
    private final UserMapper userMapper;
    private final MinioClient minioClient;

    @Value("${seustock.minio.bucket:seustock-images}")
    private String bucketName;

    private volatile boolean bucketReady;

    @Override
    public ImageDTO loadForUser(UUID externalId, String username) {
        UserDTO user = userMapper.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
        ImageDTO image = imageMapper.findByExternalId(externalId)
                .orElseThrow(() -> new NoSuchElementException("이미지를 찾을 수 없습니다."));
        if (!image.getUserId().equals(user.getId())) {
            throw new SecurityException("접근 권한이 없습니다.");
        }
        return image;
    }

    @Override
    public ImageDTO store(MultipartFile file, UserDTO owner, String contentHash) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String normalizedHash = (contentHash != null && !contentHash.isBlank()) ? contentHash : null;

        if (normalizedHash != null) {
            Optional<ImageDTO> existing = imageMapper.findByUserIdAndContentHash(owner.getId(), normalizedHash);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다.");
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "image" : file.getOriginalFilename()
        );
        String extension = extensionOf(originalFilename);
        String objectKey = "users/%d/%s%s".formatted(owner.getId(), UUID.randomUUID(), extension);

        try (InputStream inputStream = file.getInputStream()) {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .contentType(contentType)
                    .stream(inputStream, file.getSize(), -1L)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("이미지 파일을 MinIO에 저장할 수 없습니다.", e);
        }

        ImageDTO image = new ImageDTO();
        image.setUserId(owner.getId());
        image.setStoragePath(objectKey);
        image.setOriginalFilename(originalFilename);
        image.setContentType(contentType);
        image.setSizeBytes(file.getSize());
        image.setContentHash(normalizedHash);
        try {
            imageMapper.insertImage(image);
        } catch (DataIntegrityViolationException e) {
            if (normalizedHash != null) {
                return imageMapper.findByUserIdAndContentHash(owner.getId(), normalizedHash)
                        .orElseThrow(() -> e);
            }
            throw e;
        }
        return imageMapper.findById(image.getId()).orElseThrow();
    }

    @Override
    public Resource load(ImageDTO image) {
        try {
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(image.getStoragePath())
                    .build());
            return new InputStreamResource(stream);
        } catch (Exception e) {
            throw new NoSuchElementException("이미지 파일을 MinIO에서 찾을 수 없습니다.");
        }
    }

    private String extensionOf(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private void ensureBucket() throws Exception {
        if (bucketReady) {
            return;
        }
        synchronized (this) {
            if (bucketReady) {
                return;
            }
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
            }
            bucketReady = true;
        }
    }
}
