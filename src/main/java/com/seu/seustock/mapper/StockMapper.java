package com.seu.seustock.mapper;

import com.seu.seustock.model.dto.StockDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface StockMapper {
    void insertStock(StockDTO stock);
    Optional<StockDTO> findById(Long id);
    List<StockDTO> findByItemId(Long itemId);
    List<StockDTO> findBySpaceId(Long spaceId);
    void updateStock(StockDTO stock);
    void deleteById(Long id);
}
