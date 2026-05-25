package com.seu.seustock.model.dto;

import com.seu.seustock.model.enumeration.TransactionType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@ToString
public class StockTransactionDTO {
    private Long id;
    private UUID externalId;
    private Long stockId;
    private TransactionType transactionType;
    private String memo;
    private LocalDateTime createdAt;
}
