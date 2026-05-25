package com.seu.seustock.model.dto;

import com.seu.seustock.model.enumeration.StockStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
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
    private String serialNumber;
    private String lotNumber;
    private LocalDate expirationDate;
    private String memo;
    private StockStatus status;
    private LocalDateTime createdAt;
}
