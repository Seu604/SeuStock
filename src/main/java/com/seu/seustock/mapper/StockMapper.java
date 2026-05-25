package com.seu.seustock.mapper;

import com.seu.seustock.model.enumeration.StockStatus;
import com.seu.seustock.model.dto.StockDTO;
import com.seu.seustock.model.dto.ItemSpaceStockDTO;
import com.seu.seustock.model.dto.StockDetailDTO;
import com.seu.seustock.model.dto.StockPanelDTO;
import com.seu.seustock.model.form.StockUpdateForm;
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
    int countByItemId(Long itemId);
    int countInStockByItemId(Long itemId);
    List<StockDTO> findBySpaceId(Long spaceId);
    List<StockDTO> findByBoxId(Long boxId);
    List<StockDTO> findByShelfIdDirectOnly(Long shelfId);
    List<StockDTO> findBySpaceIdDirectOnly(Long spaceId);
    int updateStatusIfInStock(@Param("id") Long id, @Param("status") StockStatus status);
    int updateLocationIfInStock(@Param("ids") List<Long> ids,
                                @Param("spaceId") Long spaceId,
                                @Param("shelfId") Long shelfId,
                                @Param("boxId") Long boxId);
    void deleteById(Long id);
    int deleteInStockByExternalIdAndUserId(@Param("externalId") UUID externalId,
                                           @Param("userId") Long userId);
    void deleteInStockByItemAndBox(@Param("itemId") Long itemId, @Param("boxId") Long boxId);
    void deleteInStockByItemAndShelf(@Param("itemId") Long itemId, @Param("shelfId") Long shelfId);
    void deleteInStockByItemAndSpace(@Param("itemId") Long itemId, @Param("spaceId") Long spaceId);

    List<StockDTO> findInStockByItemAndBox(@Param("itemId") Long itemId, @Param("boxId") Long boxId);
    List<StockDTO> findInStockByItemAndShelf(@Param("itemId") Long itemId, @Param("shelfId") Long shelfId);
    List<StockDTO> findInStockByItemAndSpace(@Param("itemId") Long itemId, @Param("spaceId") Long spaceId);

    List<StockPanelDTO> findPanelByBoxId(Long boxId);
    List<StockPanelDTO> findPanelByShelfDirectOnly(Long shelfId);
    List<StockPanelDTO> findPanelBySpaceDirectOnly(Long spaceId);

    List<ItemSpaceStockDTO> findSpaceStockByItem(@Param("itemExternalId") UUID itemExternalId,
                                                 @Param("userId") Long userId);

    List<StockDetailDTO> searchDetails(@Param("userId") Long userId,
                                       @Param("itemExternalId") UUID itemExternalId,
                                       @Param("spaceExternalId") UUID spaceExternalId,
                                       @Param("shelfExternalId") UUID shelfExternalId,
                                       @Param("boxExternalId") UUID boxExternalId);

    Optional<StockDetailDTO> findDetailByExternalId(@Param("externalId") UUID externalId,
                                                    @Param("userId") Long userId);

    int updateDetails(@Param("externalId") UUID externalId,
                      @Param("userId") Long userId,
                      @Param("form") StockUpdateForm form);
}
