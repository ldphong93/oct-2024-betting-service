package com.learn.oct2024.betting_service.service.Impl;

import com.learn.oct2024.betting_service.repository.BetDetailRepository;
import com.learn.oct2024.betting_service.repository.MatchRepository;
import com.learn.oct2024.betting_service.service.MatchService;
import com.learn.oct2024.common.model.dto.CreateMatchRequest;
import com.learn.oct2024.common.model.dto.CreateMatchResponse;
import com.learn.oct2024.common.model.entity.BetDetail;
import com.learn.oct2024.common.model.entity.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class MatchServiceImpl implements MatchService {

    private Logger LOGGER = LoggerFactory.getLogger(MatchServiceImpl.class);

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd"); // Define the format

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private BetDetailRepository betDetailRepository;

    @Override
    public ResponseEntity<List<Match>> getAllMatchesFrom(String fromDate) {
        LocalDate date = fromDate != null ? LocalDate.parse(fromDate) : LocalDate.now();

        List<Match> matches = matchRepository.findByMatchDateFrom(date);
        LOGGER.info("Fetch matches list successfully.");
        return ResponseEntity.ok(matches);
    }

    @Override
    public ResponseEntity<Match> getMatch(String matchId) {
        LOGGER.info("MatchService gets called: getMatch()");
        Optional<Match> optionalMatch = matchRepository.findById(matchId);
        if (optionalMatch.isPresent()) {
            return ResponseEntity.ok(optionalMatch.get());
        } else {
            return ResponseEntity.ok().build();
        }
    }

    @Override
    public ResponseEntity<CreateMatchResponse> addMatch(CreateMatchRequest request) {
        LOGGER.info("Match creation request received.");

        CreateMatchResponse response = null;
        try {
            Match newMatch = Match.builder()
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .matchDate(dateFormat.parse(request.getMatchDate()))
                    .createdBy(request.getCreatedBy())
                    .creationDate(Date.from(Instant.now()))
                    .build();

            newMatch = matchRepository.save(newMatch);
            response = CreateMatchResponse.builder()
                    .successful(true)
                    .message("Match creation successful.")
                    .match(newMatch)
                    .build();
        } catch (Exception e) {
            LOGGER.info("Exception when creating match", e.toString());
        }

        return ResponseEntity.ok(response);
    }

    @Override
    public boolean checkMatchAvailableForBetting(String matchId) {
        //check if match already finish
        Optional<Match> optionalMatch = matchRepository.findById(matchId);
        if (optionalMatch.isEmpty()) {
            LOGGER.info("Match not found.", matchId);
            return false;
        } else {
            Match match = optionalMatch.get();
            Date currentDate = new Date(System.currentTimeMillis());
            if (currentDate.compareTo(match.getMatchDate()) >= 0) {
                LOGGER.info("Match already finished.", matchId);
                return false;
            }
        }

        //check if match already finalized
        Optional<BetDetail> optionalCheckBetDetail = betDetailRepository.findByMatchId(matchId);
        if (optionalCheckBetDetail.isPresent()) {
            BetDetail checkBetDetail = optionalCheckBetDetail.get();
            if (checkBetDetail.isFinalized()) {
                LOGGER.info("Match already finalized.");
                return false;
            }
        }
        return true;
    }
}
