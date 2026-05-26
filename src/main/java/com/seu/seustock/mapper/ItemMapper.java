package com.seu.seustock.mapper;

import com.seu.seustock.model.dto.ItemDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
public interface ItemMapper {
    void insertItem(ItemDTO item);
    Optional<ItemDTO> findById(Long id);
    Optional<ItemDTO> findByExternalId(UUID externalId);
    List<ItemDTO> findByUserId(Long userId);
    List<ItemDTO> findByUserIdWithOptions(@Param("userId") Long userId,
                                          @Param("keyword") String keyword,
                                          @Param("sortBy") String sortBy);
    void updateItem(ItemDTO item);
    void deactivateById(Long id);
    void deleteById(Long id);
}
