package com.seu.seustock.mapper;

import com.seu.seustock.model.dto.ShelfDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ShelfMapper {
    void insertShelf(ShelfDTO shelf);
    Optional<ShelfDTO> findById(Long id);
    List<ShelfDTO> findBySpaceId(Long spaceId);
    void updateShelf(ShelfDTO shelf);
    void deleteById(Long id);
}
