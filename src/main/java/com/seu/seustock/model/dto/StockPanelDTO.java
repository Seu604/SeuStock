package com.seu.seustock.model.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Getter
@Setter
@ToString
public class StockPanelDTO {
    private UUID itemExternalId;
    private String itemName;
    private UUID displayImageExternalId;
    private Integer count;
}
