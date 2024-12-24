package com.learn.oct2024.betting_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BetResponse {

    private boolean successful;

    private String message;

    public BetResponse(String message) {
        this.successful = false;
        this.message = message;
    }
}
