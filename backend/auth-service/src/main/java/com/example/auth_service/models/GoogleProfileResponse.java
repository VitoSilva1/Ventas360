package com.example.auth_service.models;

/** Profile derived from a validated Google ID token. Subject is the stable application identity. */
public record GoogleProfileResponse(String subject, String email, String name, String picture) {
}
