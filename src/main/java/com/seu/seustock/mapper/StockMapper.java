package com.seu.seustock.mapper;

import com.seu.seustock.model.StockStatus;
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
    int updateStatusIfInStock(@Param("id") Long id, @Param("status") StockStatus status);
    void deleteById(Long id);
    void deleteInStockByItemAndBox(@Param("itemId") Long itemId, @Param("boxId") Long boxId);
    void deleteInStockByItemAndShelf(@Param("itemId") Long itemId, @Param("shelfId") Long shelfId);
    void deleteInStockByItemAndSpace(@Param("itemId") Long itemId, @Param("spaceId") Long spaceId);

    List<StockDTO> findInStockByItemAndBox(@Param("itemId") Long itemId, @Param("boxId") Long boxId);
    List<StockDTO> findInStockByItemAndShelf(@Param("itemId") Long itemId, @Param("shelfId") Long shelfId);
    List<StockDTO> findInStockByItemAndSpace(@Param("itemId") Long itemId, @Param("spaceId") Long spaceId);

    List<StockPanelDTO> findPanelByBoxId(Long boxId);
    List<StockPanelDTO> findPanelByShelfDirectOnly(Long shelfId);
    List<StockPanelDTO> findPanelBySpaceDirectOnly(Long spaceId);
}
