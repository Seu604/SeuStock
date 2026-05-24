package com.seu.seustock.model.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Getter @Setter @ToString
public class ItemSpaceStockDTO {
    private UUID spaceExternalId;
    private String spaceName;
    private int count;
}
