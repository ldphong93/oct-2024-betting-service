package com.learn.oct2024.betting_service.service;

public interface KafkaService {

    public void sendMessage(String topicName, String message);
}
