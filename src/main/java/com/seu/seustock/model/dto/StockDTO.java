package com.seu.seustock.model.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@ToString
public class StockDTO {
    private Long id;
    private UUID externalId;
    private Long itemId;
    private Long spaceId;
    private Long shelfId;
    private Long boxId;
    private Integer quantity;
    private LocalDateTime createdAt;
}
