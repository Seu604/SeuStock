package com.seu.seustock.model.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Getter
@Setter
@ToString
public class StockPanelDTO {
    private Long id;
    private UUID externalId;
    private String itemName;
    private Integer quantity;
}
