package com.seu.seustock.mapper;

import com.seu.seustock.model.dto.StockTransactionDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface StockTransactionMapper {
    void insertTransaction(StockTransactionDTO transaction);
    Optional<StockTransactionDTO> findById(Long id);
    List<StockTransactionDTO> findByStockId(Long stockId);
}
