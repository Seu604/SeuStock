package com.seu.seustock.mapper;

import com.seu.seustock.model.dto.StockDTO;
import com.seu.seustock.model.dto.StockPanelDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
public interface StockMapper {
    void insertStock(StockDTO stock);
    Optional<StockDTO> findById(Long id);
    Optional<StockDTO> findByExternalId(UUID externalId);
    List<StockDTO> findByItemId(Long itemId);
    List<StockDTO> findBySpaceId(Long spaceId);
    List<StockDTO> findByBoxId(Long boxId);
    List<StockDTO> findByShelfIdDirectOnly(Long shelfId);
    List<StockDTO> findBySpaceIdDirectOnly(Long spaceId);
    void updateStock(StockDTO stock);
    void updateQuantity(@Param("id") Long id, @Param("quantity") int quantity);
    void deleteById(Long id);

    List<StockPanelDTO> findPanelByBoxId(Long boxId);
    List<StockPanelDTO> findPanelByShelfDirectOnly(Long shelfId);
    List<StockPanelDTO> findPanelBySpaceDirectOnly(Long spaceId);
}
