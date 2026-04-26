package com.joaolucas.finance_tracker.entity;


import com.fasterxml.jackson.annotation.JsonCreator;


public enum TransactionType {
    INCOME,
    EXPENSE;

    @JsonCreator
    public static TransactionType from(String value) {
        return TransactionType.valueOf(value.toUpperCase());
    }
}