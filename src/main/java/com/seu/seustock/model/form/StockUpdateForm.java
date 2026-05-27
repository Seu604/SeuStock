package com.seu.seustock.model.form;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class StockUpdateForm {

    @Size(max = 255, message = "{valid.stockUpdate.serialNumber.size}")
    private String serialNumber;

    @Size(max = 255, message = "{valid.stockUpdate.lotNumber.size}")
    private String lotNumber;

    private LocalDate expirationDate;

    private String memo;
}
