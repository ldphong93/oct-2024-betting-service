package com.learn.oct2024.betting_service.interceptor;

import com.learn.oct2024.betting_service.service.ProfileServiceClient;
import com.learn.oct2024.betting_service.utils.RequiredRoles;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j(topic = "TokenValidationInterceptor")
public class TokenValidationInterceptor implements HandlerInterceptor {

    @Autowired
    @Lazy
    private ProfileServiceClient profileServiceClient;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //Check if preflight request
        if (isPreflightRequest(request)) {
            log.info("Preflight request received, bypass Authentication.");
            return true;
        }


        String tok = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getHeader("Authorization");

        //validate token and get user's role
        log.info("Receive request to: " + request.getRequestURI());
        String token = request.getHeader("Authorization");
        if (token == null) {
            log.info("Http token header not present.");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Http token header not present.");
            return false;
        }
        List<String> userRoles = validateTokenAndGetRoles(token.substring(7));
        if (userRoles.isEmpty()) {
            log.info("User role is empty or token not valid, request unauthorized.");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Request Unauthorized");
            return false;
        }

        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            RequiredRoles requiredRoles = handlerMethod.getMethodAnnotation(RequiredRoles.class);
            if (requiredRoles != null) {
                String[] rolesNeeded = requiredRoles.value();
                if (Arrays.stream(rolesNeeded).allMatch(userRoles::contains)) {
                    return true;
                } else {
                    log.info("User role not enough authority.");
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "User role not enough authority");
                    return false;
                }
            }
        }
        return true;
    }

    private List<String> validateTokenAndGetRoles(String token) {
        try {
            ResponseEntity<List<String>> response = profileServiceClient.validateToken(token);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
            return List.of();
        } catch (FeignException e) {
            log.info(e.toString());
            log.info(e.getMessage());
            return List.of();
        }
    }

    private boolean isPreflightRequest(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod()) &&
                request.getHeader("Access-Control-Request-Method") != null;
    }
}
