package com.seu.seustock.model.dto;

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
    private String transactionType;
    private String memo;
    private LocalDateTime createdAt;
}
