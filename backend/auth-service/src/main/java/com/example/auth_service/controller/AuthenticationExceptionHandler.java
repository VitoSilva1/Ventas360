package com.example.auth_service.controller;

import com.example.auth_service.service.InvalidGoogleIdTokenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthenticationExceptionHandler {
    @ExceptionHandler(InvalidGoogleIdTokenException.class)
    ProblemDetail invalidGoogleIdToken() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "The Google ID token is invalid or the email is not verified.");
    }
}
