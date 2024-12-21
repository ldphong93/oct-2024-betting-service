package com.learn.oct2024.betting_service.service;

import com.learn.oct2024.betting_service.model.dto.BetRequest;
import com.learn.oct2024.betting_service.model.dto.BetResponse;
import com.learn.oct2024.betting_service.model.dto.FinalizeMatchRequest;
import org.bson.Document;
import org.springframework.http.ResponseEntity;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface BettingService {
    public Collection<Document> searchKeywords(String keywords, int limit);

    ResponseEntity<BetResponse> layBet(BetRequest request) throws ExecutionException, InterruptedException, TimeoutException;

    ResponseEntity<String> finalizeBet(FinalizeMatchRequest request);

}
