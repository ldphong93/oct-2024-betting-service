package com.learn.oct2024.betting_service.model.dto;

import lombok.Data;

@Data
public class BetResponse {

    private boolean successful;

    private String message;

    private Integer balance;

    public BetResponse(String message) {
        this.successful = false;
        this.message = message;
        this.balance = null;
    }
}
