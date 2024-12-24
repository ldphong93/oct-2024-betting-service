package com.learn.oct2024.betting_service.controller;

import com.learn.oct2024.betting_service.service.MatchService;
import com.learn.oct2024.betting_service.utils.RequiredRoles;
import com.learn.oct2024.common.model.dto.CreateMatchRequest;
import com.learn.oct2024.common.model.dto.CreateMatchResponse;
import com.learn.oct2024.common.model.entity.Match;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/match")
@RequiredArgsConstructor
public class MatchController {

    @Autowired
    private MatchService matchService;

    @GetMapping()
    public ResponseEntity<List<Match>> getAllMatchesFrom(@Param(value = "fromDate") String fromDate) {
        return matchService.getAllMatchesFrom(fromDate);
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<Match> getMatch(@PathVariable("id") String matchId) {
        return matchService.getMatch(matchId);
    }

    @PostMapping(value = "/add")
    @RequiredRoles({"admin"})
    public ResponseEntity<CreateMatchResponse> createNewMatch(@RequestBody CreateMatchRequest request) {
        return matchService.addMatch(request);
    }
}
