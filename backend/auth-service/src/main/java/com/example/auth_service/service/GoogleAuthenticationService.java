package com.example.auth_service.service;

import com.example.auth_service.models.GoogleProfileResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

@Service
public class GoogleAuthenticationService {
    private final JwtDecoder jwtDecoder;

    public GoogleAuthenticationService(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    public GoogleProfileResponse signIn(String credential) {
        try {
            Jwt jwt = jwtDecoder.decode(credential);
            String email = jwt.getClaimAsString("email");
            Boolean emailVerified = jwt.getClaim("email_verified");
            if (!Boolean.TRUE.equals(emailVerified) || email == null || email.isBlank() || jwt.getSubject() == null) {
                throw new InvalidGoogleIdTokenException();
            }
            return new GoogleProfileResponse(jwt.getSubject(), email, jwt.getClaimAsString("name"),
                    jwt.getClaimAsString("picture"));
        } catch (JwtException exception) {
            throw new InvalidGoogleIdTokenException();
        }
    }
}
