package com.learn.oct2024.betting_service.controller;

import com.learn.oct2024.betting_service.BettingServiceApplication;
import com.learn.oct2024.betting_service.model.dto.BetRequest;
import com.learn.oct2024.betting_service.model.dto.BetResponse;
import com.learn.oct2024.betting_service.model.dto.FinalizeMatchRequest;
import com.learn.oct2024.betting_service.service.BettingService;
import com.learn.oct2024.betting_service.utils.RequiredRoles;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/bet")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class BettingController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BettingServiceApplication.class);

    private final BettingService bettingService;

    @GetMapping("movies/with/{keywords}")
    Collection<Document> getMoviesWithKeywords(@PathVariable String keywords,
                                               @RequestParam(value = "limit", defaultValue = "5") int limit) {
        return bettingService.searchKeywords(keywords, limit);
    }

    @RequiredRoles({"bettor"})
    @PostMapping(value = "/lay")
    public ResponseEntity<BetResponse> betOnMatch(@RequestBody BetRequest request) throws ExecutionException, InterruptedException, TimeoutException {
        return bettingService.layBet(request);
    }

    @RequiredRoles({"admin"})
    @PostMapping(value = "/finalize")
    public ResponseEntity<String> finalizeOnMatch(@RequestBody FinalizeMatchRequest request) {
        return bettingService.finalizeBet(request);
    }

}
