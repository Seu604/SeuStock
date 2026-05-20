package com.seu.seustock.model.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class StockInOutForm {

    @NotNull(message = "품목 정보가 누락되었습니다.")
    private UUID itemExternalId;

    @NotNull(message = "공간 정보가 누락되었습니다.")
    private UUID spaceExternalId;

    private UUID shelfExternalId;

    private UUID boxExternalId;

    @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
    private int count = 1;

    private String memo;
}
