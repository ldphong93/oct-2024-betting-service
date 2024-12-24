package com.learn.oct2024.betting_service.service.Impl;

import com.learn.oct2024.betting_service.model.dto.BetRequest;
import com.learn.oct2024.betting_service.model.dto.BetResponse;
import com.learn.oct2024.betting_service.model.dto.FinalizeMatchRequest;
import com.learn.oct2024.betting_service.repository.BetDetailRepository;
import com.learn.oct2024.betting_service.service.BettingService;
import com.learn.oct2024.betting_service.service.KafkaService;
import com.learn.oct2024.betting_service.service.MatchService;
import com.learn.oct2024.common.model.dto.UserActionRequest;
import com.learn.oct2024.common.model.dto.UserInfoResponse;
import com.learn.oct2024.common.model.entity.BetDetail;
import com.learn.oct2024.common.model.enums.MatchResult;
import com.learn.oct2024.common.model.enums.UserAction;
import com.learn.oct2024.common.model.exception.EntityNotFoundException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.search.SearchOptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.search.SearchOperator.text;
import static com.mongodb.client.model.search.SearchPath.fieldPath;

@Service
@Slf4j(topic = "BettingServiceImpl")
public class BettingServiceImpl implements BettingService {

    private final MongoCollection<Document> collection;

    @Autowired
    private KafkaService kafkaService;

    @Autowired
    private KafkaTemplate userActionKafkaTemplate;

    @Autowired
    private MatchService matchService;

    @Autowired
    private BetDetailRepository betDetailRepository;

    private final BlockingQueue<UserInfoResponse> replyQueue = new ArrayBlockingQueue<>(1);

    private final ConcurrentHashMap<String, BlockingQueue<UserInfoResponse>> responseQueues = new ConcurrentHashMap<>();

    @Value("dynamicIdxName")
    private String index;

    public BettingServiceImpl(MongoTemplate mongoTemplate) {
        this.collection = mongoTemplate.getCollection("movies");
    }

    @Override
    @Transactional
    public ResponseEntity<BetResponse> layBet(BetRequest request) throws InterruptedException {
        log.info("Received betting request with userId id [{}]." + request.getUserId());

        UserInfoResponse userInfo = getUserInfo(request.getUserId());

        if (userInfo.getId() == null) {
            log.info("Betting request with user not found.");
            return ResponseEntity.ok(new BetResponse("User not found."));
        }

        //check match available for betting
        if (!matchService.checkMatchAvailableForBetting(request.getMatchId())) {
            log.info("Match not available for betting.");
            return ResponseEntity.ok(new BetResponse("Match not available for betting."));
        }

        //check user balance
        if (userInfo.getBalance() < request.getAmount()) {
            log.info("Balance not sufficient.");
            return ResponseEntity.ok(new BetResponse(String.format("Balance not sufficient [%s], please lay bet again.", userInfo.getBalance())));
        }

        //debit bettor account and add to booker's account
        adjustBalance(userInfo.getId(), -request.getAmount());
        log.info("Bettor's account deduction sent with amount: " + request.getAmount());

        //save detail into betting table
        Map<String, Object> userBetInfo = new HashMap<>();
        userBetInfo.put("result", request.getResult());
        userBetInfo.put("userId", request.getUserId());
        userBetInfo.put("amount", request.getAmount());

        Optional<BetDetail> optionalBetDetail = betDetailRepository.findByMatchId(request.getMatchId());
        BetDetail betDetail;
        if (optionalBetDetail.isEmpty()) {
            betDetail = BetDetail.builder()
                    .matchId(request.getMatchId())
                    .betInfo(List.of(userBetInfo))
                    .isFinalized(false)
                    .build();
        } else {
            betDetail = optionalBetDetail.get();
            List<Map<String, Object>> bets = betDetail.getBetInfo();
            bets.add(userBetInfo);
            betDetail.setBetInfo(bets);
        }
        BetDetail savedBetDetails = betDetailRepository.save(betDetail);

        log.info("Betting request saved successfully." + savedBetDetails);
        return ResponseEntity.ok(BetResponse.builder()
                .successful(true)
                .message("Betting request saved successfully." + savedBetDetails)
                .build());
    }

    private void adjustBalance(String userId, Integer amount) {
        String correlationID = UUID.randomUUID().toString();
        UserActionRequest userActionRequest = UserActionRequest.builder()
                .id(userId)
                .action(UserAction.ADJUST_BALANCE)
                .amount(amount)
                .build();
        Message<UserActionRequest> message = MessageBuilder.withPayload(userActionRequest)
                .setHeader(KafkaHeaders.TOPIC, "request-topic")
                .setHeader(KafkaHeaders.CORRELATION_ID, correlationID)
                .build();
        userActionKafkaTemplate.send(message);
        System.out.println("Send user action request with correlation id: " + correlationID);
    }

    private UserInfoResponse getUserInfo(String userId) throws InterruptedException {
        String correlationId = UUID.randomUUID().toString();
        UserActionRequest userActionRequest = UserActionRequest.builder()
                .id(userId)
                .action(UserAction.GET_INFO)
                .build();
        Message<UserActionRequest> message = MessageBuilder.withPayload(userActionRequest)
                .setHeader(KafkaHeaders.TOPIC, "request-topic")
                .setHeader(KafkaHeaders.CORRELATION_ID, correlationId)
                .build();
        userActionKafkaTemplate.send(message);
        System.out.println("Send user info request with correlation id: " + correlationId);

        UserInfoResponse res = waitForResponse(correlationId);
        System.out.println("Received user info response: " + res);
        return res;
    }

    private UserInfoResponse waitForResponse(String correlationId) throws InterruptedException {
        BlockingQueue<UserInfoResponse> queue = new ArrayBlockingQueue<>(1);
        responseQueues.put(correlationId, queue);
        try {
            // Wait for the response with a timeout
            UserInfoResponse response = queue.poll(5, TimeUnit.SECONDS);
            if (response == null) {
                throw new RuntimeException("Timeout waiting for response");
            }
            return response;
        } finally {
            // Clean up
            responseQueues.remove(correlationId);
        }
    }

    @KafkaListener(topics = "response-topic", containerFactory = "userActionContainerFactory")
    public void listenForReply(ConsumerRecord<String, UserInfoResponse> record) {
        Header correlationIdHeader = record.headers().lastHeader(KafkaHeaders.CORRELATION_ID);
        String correlationId = new String(correlationIdHeader.value(), StandardCharsets.UTF_8);
        System.out.println("Received response with correlation id: " + correlationId);

        BlockingQueue<UserInfoResponse> queue = responseQueues.get(correlationId);
        if (queue != null) {
            queue.offer(record.value());
        }
    }

    @Override
    public Collection<Document> searchKeywords(String keywords, int limit) {
        log.info("Searching movies by keywords: {}, with limit {}", keywords, limit);
        Bson searchStage = search(text(fieldPath("fullplot"), keywords), SearchOptions.searchOptions().index(index));
        Bson projectStage = project(fields(excludeId(), include("title", "year", "fullplot", "imdb.rating")));
        Bson limitStage = limit(limit);

        List<Bson> pipeline = List.of(searchStage, projectStage, limitStage);
        List<Document> docs = collection.aggregate(pipeline).into(new ArrayList<>());

        if (docs.isEmpty()) {
            throw new EntityNotFoundException("MoviesByKeyWords", keywords);
        }

        return docs;
    }

    @Override
    @Transactional
    public ResponseEntity<String> finalizeBet(FinalizeMatchRequest request) {
        log.info("Receive match finalization for match id [{}]", request.getMatchId());

        Optional<BetDetail> optionalBetDetail = betDetailRepository.findByMatchId(request.getMatchId());
        if (optionalBetDetail.isEmpty()) {
            log.info("Bet detail finalized for match [{}]", request.getMatchId());
            BetDetail betDetail = BetDetail.builder()
                    .matchId(request.getMatchId())
                    .result(MatchResult.valueOf(request.getResult()))
                    .isFinalized(true)
                    .build();
            betDetailRepository.save((betDetail));
            return ResponseEntity.ok(String.format("Bet detail finalized for match [%s]", request.getMatchId()));
        }

        BetDetail betDetail = optionalBetDetail.get();
        if (betDetail.isFinalized()) {
            log.info("Match with id [{}] already finalized.", request.getMatchId());
            return ResponseEntity.ok(String.format("Match with id [%s] already finalized.", request.getMatchId()));
        }

        List<Map<String, Object>> betInfos = betDetail.getBetInfo();
        for (Map<String, Object> info : betInfos) {
            if (info.get("result").toString().equals(request.getResult())) {
                String userId = info.get("userId").toString();
                int amount = Integer.parseInt((info.get("amount").toString()));

                //credit bettor account and minus booker's account
                adjustBalance(userId, 2 * amount);
                log.info("Bettor's account credit sent with amount: " + 2 * amount);
            }
        }

        //set bet detail finalized
        betDetail.setResult(MatchResult.valueOf(request.getResult()));
        betDetail.setFinalized(true);
        betDetailRepository.save(betDetail);

        log.info("Bet detail finalized for match [{}]", request.getMatchId());
        return ResponseEntity.ok(String.format("Bet detail finalized for match [%s]", request.getMatchId()));
    }
}
