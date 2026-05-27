package com.seu.seustock.service;

import com.seu.seustock.mapper.ImageMapper;
import com.seu.seustock.mapper.UserMapper;
import com.seu.seustock.model.dto.ImageDTO;
import com.seu.seustock.model.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@ConditionalOnProperty(
        name = "seustock.image-storage.type",
        havingValue = "local"
)
@RequiredArgsConstructor
public class LocalImageStorageService implements ImageStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final ImageMapper imageMapper;
    private final UserMapper userMapper;

    @Value("${seustock.upload-dir:uploads/images}")
    private String uploadDir;

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
        String storedFilename = UUID.randomUUID() + extension;
        Path uploadPath = Path.of(uploadDir).toAbsolutePath().normalize();
        Path storedPath = uploadPath.resolve(storedFilename).normalize();

        if (!storedPath.startsWith(uploadPath)) {
            throw new IllegalArgumentException("잘못된 파일 경로입니다.");
        }

        try {
            Files.createDirectories(uploadPath);
            file.transferTo(storedPath);
        } catch (IOException e) {
            throw new IllegalStateException("이미지 파일을 저장할 수 없습니다.", e);
        }

        ImageDTO image = new ImageDTO();
        image.setUserId(owner.getId());
        image.setStoragePath(storedFilename);
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
        Path uploadPath = Path.of(uploadDir).toAbsolutePath().normalize();
        Path path = uploadPath.resolve(image.getStoragePath()).normalize();
        if (!path.startsWith(uploadPath)) {
            throw new IllegalArgumentException("잘못된 파일 경로입니다.");
        }
        Resource resource = new FileSystemResource(path);
        if (!resource.exists() || !resource.isReadable()) {
            throw new NoSuchElementException("이미지 파일을 찾을 수 없습니다.");
        }
        return resource;
    }

    private String extensionOf(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex).toLowerCase(Locale.ROOT);
    }
}
