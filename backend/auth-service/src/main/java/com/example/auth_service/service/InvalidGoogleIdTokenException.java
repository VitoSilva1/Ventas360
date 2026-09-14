package com.example.auth_service.service;

public class InvalidGoogleIdTokenException extends RuntimeException {
    public InvalidGoogleIdTokenException() {
        super("The Google ID token is invalid or the email is not verified.");
    }
}
