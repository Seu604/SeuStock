package com.seu.seustock.mapper;

import com.seu.seustock.model.dto.ImageDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;
import java.util.UUID;

@Mapper
public interface ImageMapper {
    void insertImage(ImageDTO image);
    Optional<ImageDTO> findById(Long id);
    Optional<ImageDTO> findByExternalId(UUID externalId);
    Optional<ImageDTO> findPrimaryByItemId(Long itemId);
    Optional<ImageDTO> findPrimaryByStockId(Long stockId);
    Optional<ImageDTO> findByUserIdAndContentHash(Long userId, String contentHash);
}
