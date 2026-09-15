package com.example.auth_service.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleIdTokenValidatorTest {
    private static final String CLIENT_ID = "client-id.apps.googleusercontent.com";
    private KeyPair trustedKeyPair;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() throws Exception {
        trustedKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) trustedKeyPair.getPublic()).build();
        jwtDecoder.setJwtValidator(GoogleIdTokenValidator.forClientId(CLIENT_ID));
        decoder = jwtDecoder;
    }

    @Test
    void acceptsValidGoogleIdToken() throws Exception {
        assertThatCode(() -> decoder.decode(token(trustedKeyPair, "https://accounts.google.com", CLIENT_ID,
                Instant.now().plusSeconds(300), Instant.now().minusSeconds(10)))).doesNotThrowAnyException();
    }

    @Test
    void acceptsGoogleAlternativeIssuer() throws Exception {
        assertThatCode(() -> decoder.decode(token(trustedKeyPair, "accounts.google.com", CLIENT_ID,
                Instant.now().plusSeconds(300), Instant.now().minusSeconds(10)))).doesNotThrowAnyException();
    }

    @Test
    void rejectsUnexpectedIssuerAudienceExpiryNotBeforeAndSignature() throws Exception {
        assertRejected(token(trustedKeyPair, "https://evil.example", CLIENT_ID, Instant.now().plusSeconds(300), Instant.now().minusSeconds(10), java.util.Map.of()));
        assertRejected(token(trustedKeyPair, "https://accounts.google.com", "other-client", Instant.now().plusSeconds(300), Instant.now().minusSeconds(10), java.util.Map.of()));
        assertRejected(token(trustedKeyPair, "https://accounts.google.com", CLIENT_ID, Instant.now().minusSeconds(120), Instant.now().minusSeconds(240), java.util.Map.of()));
        assertRejected(token(trustedKeyPair, "https://accounts.google.com", CLIENT_ID, Instant.now().plusSeconds(300), Instant.now().plusSeconds(120), java.util.Map.of()));
        KeyPair untrusted = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        assertRejected(token(untrusted, "https://accounts.google.com", CLIENT_ID, Instant.now().plusSeconds(300), Instant.now().minusSeconds(10), java.util.Map.of()));
    }

    @Test
    void rejectsTokensWithoutVerifiedEmailEmailOrSubject() throws Exception {
        assertRejected(token(trustedKeyPair, "https://accounts.google.com", CLIENT_ID, Instant.now().plusSeconds(300), Instant.now().minusSeconds(10), java.util.Map.of("email_verified", false)));
        assertRejected(token(trustedKeyPair, "https://accounts.google.com", CLIENT_ID, Instant.now().plusSeconds(300), Instant.now().minusSeconds(10), java.util.Map.of("email", "")));
        assertRejected(token(trustedKeyPair, "https://accounts.google.com", CLIENT_ID, Instant.now().plusSeconds(300), Instant.now().minusSeconds(10), java.util.Map.of("sub", "")));
    }

    private void assertRejected(String token) {
        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    private String token(KeyPair keyPair, String issuer, String audience, Instant expiresAt, Instant notBefore) throws Exception {
        return token(keyPair, issuer, audience, expiresAt, notBefore, java.util.Map.of());
    }

    private String token(KeyPair keyPair, String issuer, String audience, Instant expiresAt, Instant notBefore,
            java.util.Map<String, Object> overrides) throws Exception {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder().issuer(issuer).audience(audience).subject("google-subject")
                .issueTime(new Date()).notBeforeTime(Date.from(notBefore)).expirationTime(Date.from(expiresAt))
                .claim("email", "user@example.com").claim("email_verified", true);
        overrides.forEach(builder::claim);
        JWTClaimsSet claims = builder.build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner(keyPair.getPrivate()));
        return jwt.serialize();
    }
}
