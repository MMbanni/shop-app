package com.mbanni.shop.security;

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
        problem.setDetail("Account suspended until "+suspendedUntil);
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
        problem.setDetail("Account is Banned");


        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(
                MediaType.APPLICATION_PROBLEM_JSON_VALUE
        );
        objectMapper.writeValue(
                response.getOutputStream(),
                problem
        );

    }
    public void writeUnauthorized(HttpServletResponse response)
            throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");

        writeProblem(
                response,
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED",
                "Please log in to continue."
        );
    }

    public void writeAccessDenied(HttpServletResponse response)
            throws IOException {
        writeProblem(
                response,
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                "You do not have permission to do this."
        );
    }

    private void writeProblem(
            HttpServletResponse response,
            HttpStatus status,
            String title,
            String detail
    ) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);
        problem.setDetail(detail);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        objectMapper.writeValue(response.getOutputStream(), problem);
    }

}
