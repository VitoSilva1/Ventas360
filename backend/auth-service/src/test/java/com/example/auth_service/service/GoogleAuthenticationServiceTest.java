package com.example.auth_service.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleAuthenticationServiceTest {
    @Test
    void returnsProfileOnlyForVerifiedEmail() {
        GoogleAuthenticationService service = new GoogleAuthenticationService(decoder(Map.of(
                "valid", jwt(Map.of("email", "user@example.com", "email_verified", true, "name", "User")))));
        var profile = service.signIn("valid");
        assertThat(profile.subject()).isEqualTo("google-subject");
        assertThat(profile.email()).isEqualTo("user@example.com");
    }

    @Test
    void rejectsUnverifiedOrMissingEmailAndInvalidTokens() {
        GoogleAuthenticationService service = new GoogleAuthenticationService(decoder(Map.of(
                "unverified", jwt(Map.of("email", "user@example.com", "email_verified", false)),
                "missing-email", jwt(Map.of("email_verified", true)))));
        assertThatThrownBy(() -> service.signIn("unverified")).isInstanceOf(InvalidGoogleIdTokenException.class);
        assertThatThrownBy(() -> service.signIn("missing-email")).isInstanceOf(InvalidGoogleIdTokenException.class);
        assertThatThrownBy(() -> service.signIn("invalid")).isInstanceOf(InvalidGoogleIdTokenException.class);
    }

    private JwtDecoder decoder(Map<String, Jwt> tokens) {
        return token -> {
            Jwt jwt = tokens.get(token);
            if (jwt == null) throw new JwtException("invalid token");
            return jwt;
        };
    }

    private Jwt jwt(Map<String, Object> claims) {
        Instant now = Instant.now();
        var allClaims = new HashMap<String, Object>();
        allClaims.put("sub", "google-subject");
        allClaims.put("iss", "https://accounts.google.com");
        allClaims.put("aud", "client-id");
        allClaims.putAll(claims);
        return new Jwt("token", now, now.plusSeconds(300), Map.of("alg", "RS256"), allClaims);
    }
}
