package com.seu.seustock.model.enumeration;

import java.util.Arrays;
import java.util.List;

public enum TransactionMemoMaster {
    PURCHASE_IN(TransactionType.IN, "구매 입고"),
    RETURN_IN(TransactionType.IN, "반품 입고"),
    FOUND_IN(TransactionType.IN, "재고 발견"),
    ADJUSTMENT_IN(TransactionType.IN, "수량 보정"),

    USE_OUT(TransactionType.OUT, "사용 출고"),
    SALES_OUT(TransactionType.OUT, "판매 출고"),
    DISPOSAL_OUT(TransactionType.OUT, "폐기 출고"),
    LOST_OUT(TransactionType.OUT, "분실 처리");

    private final TransactionType transactionType;
    private final String memo;

    TransactionMemoMaster(TransactionType transactionType, String memo) {
        this.transactionType = transactionType;
        this.memo = memo;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public String getMemo() {
        return memo;
    }

    public static List<String> memosFor(TransactionType transactionType) {
        return Arrays.stream(values())
                .filter(master -> master.transactionType == transactionType)
                .map(TransactionMemoMaster::getMemo)
                .toList();
    }
}
