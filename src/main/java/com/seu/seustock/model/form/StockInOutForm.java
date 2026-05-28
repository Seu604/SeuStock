package com.seu.seustock.model.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class StockInOutForm {

    @NotNull(message = "{valid.stockInOut.itemExternalId.notNull}")
    private UUID itemExternalId;

    @NotNull(message = "{valid.stockInOut.spaceExternalId.notNull}")
    private UUID spaceExternalId;

    private UUID shelfExternalId;

    private UUID boxExternalId;

    @Min(value = 1, message = "{valid.stockInOut.count.min}")
    private int count = 1;

    private String memo;

    private boolean includeKept = false;
}
