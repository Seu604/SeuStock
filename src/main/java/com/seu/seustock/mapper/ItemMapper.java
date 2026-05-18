package com.seu.seustock.mapper;

import com.seu.seustock.model.dto.ItemDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ItemMapper {
    void insertItem(ItemDTO item);
    Optional<ItemDTO> findById(Long id);
    List<ItemDTO> findByUserId(Long userId);
    void updateItem(ItemDTO item);
    void deleteById(Long id);
}
