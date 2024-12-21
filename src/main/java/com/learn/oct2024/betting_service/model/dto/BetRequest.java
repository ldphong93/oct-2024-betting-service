package com.learn.oct2024.betting_service.model.dto;

import lombok.Data;

@Data
public class BetRequest {

    private String matchId;

    private String userId;

    private String result;

    private Integer amount;
}
