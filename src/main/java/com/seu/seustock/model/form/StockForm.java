package com.seu.seustock.model.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class StockForm {

    @NotNull(message = "{valid.stock.itemExternalId.notNull}")
    private UUID itemExternalId;

    @NotNull(message = "{valid.stock.spaceExternalId.notNull}")
    private UUID spaceExternalId;

    private UUID shelfExternalId;

    private UUID boxExternalId;

    @Min(value = 1, message = "{valid.stock.count.min}")
    @Max(value = 50, message = "{valid.stock.count.max}")
    private int count = 1;

    private String serialNumber;

    private String lotNumber;

    private LocalDate expirationDate;

    private String memo;
}
