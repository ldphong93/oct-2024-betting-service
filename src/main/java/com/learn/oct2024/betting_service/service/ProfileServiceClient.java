package com.learn.oct2024.betting_service.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "profile-service", url = "${profile.service.url}")
public interface ProfileServiceClient {

    @PostMapping("/validateToken")
    ResponseEntity<List<String>> validateToken(@RequestBody String token);
}
