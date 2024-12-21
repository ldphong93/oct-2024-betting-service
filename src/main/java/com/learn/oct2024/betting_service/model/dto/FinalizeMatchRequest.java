package com.learn.oct2024.betting_service.model.dto;

import lombok.Data;

@Data
public class FinalizeMatchRequest {

    private String matchId;

    private String result;

}
