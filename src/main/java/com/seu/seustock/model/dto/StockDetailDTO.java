package com.seu.seustock.model.dto;

import com.seu.seustock.model.StockStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@ToString
public class StockDetailDTO {
    private UUID externalId;
    private UUID itemExternalId;
    private String itemName;
    private UUID displayImageExternalId;
    private UUID spaceExternalId;
    private String spaceName;
    private UUID shelfExternalId;
    private String shelfName;
    private UUID boxExternalId;
    private String boxName;
    private String serialNumber;
    private String lotNumber;
    private LocalDate expirationDate;
    private StockStatus status;
    private LocalDateTime createdAt;
}
