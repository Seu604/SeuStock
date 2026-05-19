package com.seu.seustock.model.form;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockInOutForm {

    @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
    private int quantity;

    private String memo;
}
