package com.mbanni.shop.security;

import jakarta.persistence.Column;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
@Component
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void writeSuspended(HttpServletResponse response, Instant suspendedUntil) throws IOException {

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("USER_SUSPENDED");
        problem.setDetail("Your account is currently suspended.");
        problem.setProperty("suspendedUntil", suspendedUntil);

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(
                MediaType.APPLICATION_PROBLEM_JSON_VALUE
        );
        objectMapper.writeValue(
                response.getOutputStream(),
                problem
        );

    }
    public void writeBanned(HttpServletResponse response) throws IOException {

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("USER_BANNED");
        problem.setDetail("User is Banned");


        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(
                MediaType.APPLICATION_PROBLEM_JSON_VALUE
        );
        objectMapper.writeValue(
                response.getOutputStream(),
                problem
        );

    }

}
