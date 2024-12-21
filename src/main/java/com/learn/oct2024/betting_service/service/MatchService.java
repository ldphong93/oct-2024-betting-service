package com.learn.oct2024.betting_service.service;

import com.learn.oct2024.common.model.dto.CreateMatchRequest;
import com.learn.oct2024.common.model.dto.CreateMatchResponse;
import com.learn.oct2024.common.model.entity.Match;
import org.springframework.http.ResponseEntity;

import java.util.List;


public interface MatchService {
    ResponseEntity<CreateMatchResponse> addMatch(CreateMatchRequest request);

    ResponseEntity<List<Match>> getAllMatchesFrom(String fromDate);

    ResponseEntity<Match> getMatch(String matchId);

    boolean checkMatchAvailableForBetting(String matchId);
}
