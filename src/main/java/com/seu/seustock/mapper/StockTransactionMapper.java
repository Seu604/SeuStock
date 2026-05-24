package com.seu.seustock.mapper;

import com.seu.seustock.model.dto.ItemTransactionHistoryDTO;
import com.seu.seustock.model.dto.StockTransactionDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
public interface StockTransactionMapper {
    void insertTransaction(StockTransactionDTO transaction);
    Optional<StockTransactionDTO> findById(Long id);
    List<StockTransactionDTO> findByStockId(Long stockId);
    List<ItemTransactionHistoryDTO> findHistoryByItemExternalId(
            @Param("itemExternalId") UUID itemExternalId,
            @Param("userId") Long userId);
}
