package com.learn.oct2024.betting_service.repository;

import com.learn.oct2024.common.model.entity.Match;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MatchRepository extends MongoRepository<Match, String> {

    @Query(value = "{title: '?0'}", fields = "{'title' :1, 'description': 1}")
    List<Match> findByTitle(String title);

    @Query(value = "{ 'matchDate': { '$gt': ?0 } }")
    List<Match> findByMatchDateFrom(LocalDate fromDate);
}
