package com.learn.oct2024.betting_service.configuration;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.learn.oct2024.betting_service.service")
public class FeignConfig {
}
