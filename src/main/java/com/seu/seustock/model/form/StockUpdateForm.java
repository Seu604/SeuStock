package com.seu.seustock.model.form;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class StockUpdateForm {

    @Size(max = 255, message = "시리얼 번호는 255자 이하여야 합니다.")
    private String serialNumber;

    @Size(max = 255, message = "로트 번호는 255자 이하여야 합니다.")
    private String lotNumber;

    private LocalDate expirationDate;
}
