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

    @NotNull(message = "품목을 선택해주세요.")
    private UUID itemExternalId;

    @NotNull(message = "공간 정보가 누락되었습니다.")
    private UUID spaceExternalId;

    private UUID shelfExternalId;

    private UUID boxExternalId;

    @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
    @Max(value = 50, message = "한 번에 최대 50개까지 등록할 수 있습니다.")
    private int count = 1;

    private String serialNumber;

    private String lotNumber;

    private LocalDate expirationDate;

    private String memo;
}
