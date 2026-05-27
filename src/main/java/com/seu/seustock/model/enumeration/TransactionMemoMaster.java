package com.seu.seustock.model.enumeration;

import java.util.Arrays;
import java.util.List;

public enum TransactionMemoMaster {
    PURCHASE_IN(TransactionType.IN, "enum.TransactionMemoMaster.PURCHASE_IN"),
    RETURN_IN(TransactionType.IN, "enum.TransactionMemoMaster.RETURN_IN"),
    FOUND_IN(TransactionType.IN, "enum.TransactionMemoMaster.FOUND_IN"),
    ADJUSTMENT_IN(TransactionType.IN, "enum.TransactionMemoMaster.ADJUSTMENT_IN"),

    USE_OUT(TransactionType.OUT, "enum.TransactionMemoMaster.USE_OUT"),
    SALES_OUT(TransactionType.OUT, "enum.TransactionMemoMaster.SALES_OUT"),
    DISPOSAL_OUT(TransactionType.OUT, "enum.TransactionMemoMaster.DISPOSAL_OUT"),
    LOST_OUT(TransactionType.OUT, "enum.TransactionMemoMaster.LOST_OUT");

    private final TransactionType transactionType;
    private final String messageKey;

    TransactionMemoMaster(TransactionType transactionType, String messageKey) {
        this.transactionType = transactionType;
        this.messageKey = messageKey;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public static List<String> messageKeysFor(TransactionType transactionType) {
        return Arrays.stream(values())
                .filter(master -> master.transactionType == transactionType)
                .map(TransactionMemoMaster::getMessageKey)
                .toList();
    }
}
