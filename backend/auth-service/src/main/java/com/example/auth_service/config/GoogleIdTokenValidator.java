package com.example.auth_service.config;

import java.util.Set;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;

public final class GoogleIdTokenValidator {
    private static final Set<String> GOOGLE_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");

    private GoogleIdTokenValidator() {
    }

    public static OAuth2TokenValidator<Jwt> forClientId(String clientId) {
        OAuth2TokenValidator<Jwt> issuerValidator = jwt -> GOOGLE_ISSUERS.contains(jwt.getClaimAsString("iss"))
                ? OAuth2TokenValidatorResult.success()
                : failure("Token issuer is not Google.");
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(clientId)
                ? OAuth2TokenValidatorResult.success()
                : failure("Token audience does not match this application.");
        OAuth2TokenValidator<Jwt> profileValidator = jwt -> {
            String email = jwt.getClaimAsString("email");
            String subject = jwt.getSubject();
            Boolean emailVerified = jwt.getClaim("email_verified");
            return Boolean.TRUE.equals(emailVerified) && email != null && !email.isBlank()
                    && subject != null && !subject.isBlank()
                    ? OAuth2TokenValidatorResult.success()
                    : failure("Token does not contain a verified Google profile.");
        };
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(), issuerValidator, audienceValidator, profileValidator);
    }

    private static OAuth2TokenValidatorResult failure(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", description, null));
    }
}
