package com.seu.seustock.model.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class StockForm {

    @NotNull(message = "품목을 선택해주세요.")
    private UUID itemExternalId;

    @NotNull(message = "공간 정보가 누락되었습니다.")
    private UUID spaceExternalId;

    private UUID shelfExternalId;

    private UUID boxExternalId;

    @Min(value = 0, message = "수량은 0 이상이어야 합니다.")
    private int quantity = 0;
}
