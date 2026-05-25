package com.seu.seustock.model.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class StockMoveForm {

    @NotNull(message = "출발 공간 정보가 누락되었습니다.")
    private UUID sourceSpaceExternalId;

    private UUID sourceShelfExternalId;

    private UUID sourceBoxExternalId;

    @NotNull(message = "도착 공간 정보가 누락되었습니다.")
    private UUID targetSpaceExternalId;

    private UUID targetShelfExternalId;

    private UUID targetBoxExternalId;

    @Valid
    @NotEmpty(message = "이동할 물품을 선택해주세요.")
    private List<Item> items = new ArrayList<>();

    private String memo;

    @Getter
    @Setter
    public static class Item {
        @NotNull(message = "품목 정보가 누락되었습니다.")
        private UUID itemExternalId;

        @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
        private int count = 1;
    }
}
