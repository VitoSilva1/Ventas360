package com.example.auth_service.controller;

import com.example.auth_service.config.GoogleIdTokenValidator;
import com.example.auth_service.config.SecurityConfig;
import com.example.auth_service.service.GoogleAuthenticationService;
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
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AuthenticationController.class, properties = "google.oauth.client-id=client-id.apps.googleusercontent.com")
@Import({SecurityConfig.class, AuthenticationExceptionHandler.class, AuthenticationControllerTest.TestConfiguration.class})
class AuthenticationControllerTest {
    private static final String CLIENT_ID = "client-id.apps.googleusercontent.com";
    @Autowired MockMvc mockMvc;

    @Test
    void googleSignInIsPublicAndReturnsVerifiedProfile() throws Exception {
        mockMvc.perform(post("/auth/google/sign-in").contentType(APPLICATION_JSON)
                        .content("{\"credential\":\"" + TestConfiguration.token(Map.of()) + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.subject").value("subject"))
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    void googleSignInRejectsMissingAndInvalidCredentials() throws Exception {
        mockMvc.perform(post("/auth/google/sign-in").contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/auth/google/sign-in").contentType(APPLICATION_JSON).content("{\"credential\":\"bad\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutIsProtectedAndStatelessForAValidGoogleToken() throws Exception {
        mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer " + TestConfiguration.token(Map.of())))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/auth/logout")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoutesRejectTokensWithoutVerifiedEmailEmailOrSubject() throws Exception {
        for (Map<String, Object> omittedOrInvalidClaim : java.util.List.<Map<String, Object>>of(
                Map.<String, Object>of("email_verified", false), Map.<String, Object>of("email", ""),
                Map.<String, Object>of("sub", ""))) {
            mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer " + TestConfiguration.token(omittedOrInvalidClaim)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void everyNonPublicRouteRequiresGoogleBearerToken() throws Exception {
        mockMvc.perform(get("/internal/anything")).andExpect(status().isUnauthorized());
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfiguration {
        private static final KeyPair KEY_PAIR = keyPair();

        @Bean @Primary
        JwtDecoder testJwtDecoder() {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) KEY_PAIR.getPublic()).build();
            decoder.setJwtValidator(GoogleIdTokenValidator.forClientId(CLIENT_ID));
            return decoder;
        }

        @Bean
        GoogleAuthenticationService googleAuthenticationService(JwtDecoder jwtDecoder) {
            return new GoogleAuthenticationService(jwtDecoder);
        }

        static String token(Map<String, ?> overrides) throws Exception {
            Instant now = Instant.now();
            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                    .issuer("https://accounts.google.com").audience(CLIENT_ID).subject("subject")
                    .issueTime(new Date()).notBeforeTime(Date.from(now.minusSeconds(10)))
                    .expirationTime(Date.from(now.plusSeconds(300))).claim("email", "user@example.com")
                    .claim("email_verified", true).claim("name", "User");
            overrides.forEach(claims::claim);
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims.build());
            jwt.sign(new RSASSASigner(KEY_PAIR.getPrivate()));
            return jwt.serialize();
        }

        private static KeyPair keyPair() {
            try {
                return KeyPairGenerator.getInstance("RSA").generateKeyPair();
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }
    }
}
