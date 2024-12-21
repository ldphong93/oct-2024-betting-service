package com.learn.oct2024.betting_service.repository;

import com.learn.oct2024.common.model.entity.BetDetail;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BetDetailRepository extends MongoRepository<BetDetail, String> {

    Optional<BetDetail> findByMatchId(String matchId);
}
