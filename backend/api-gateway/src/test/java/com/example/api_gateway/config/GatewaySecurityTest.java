package com.example.api_gateway.config;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GatewaySecurityTest.ProbeController.class,
        properties = "google.oauth.client-id=client-id.apps.googleusercontent.com")
@Import({SecurityConfig.class, GatewaySecurityTest.TestConfiguration.class, GatewaySecurityTest.ProbeController.class})
class GatewaySecurityTest {
    private static final String CLIENT_ID = "client-id.apps.googleusercontent.com";
    @Autowired MockMvc mockMvc;

    @RestController
    static class ProbeController {
        @RequestMapping({"/products", "/sales", "/auth/logout", "/future-route",
                "/auth/google/sign-in", "/actuator/health"})
        String probe() { return "ok"; }
    }

    @Test
    void allServicesAndFutureRoutesRequireToken() throws Exception {
        for (String path : java.util.List.of("/products", "/sales", "/auth/logout", "/future-route")) {
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
            mockMvc.perform(get(path).header("Authorization", "Bearer " + TestConfiguration.token(Map.of())))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void publicEndpointsAndCorsPreflightRemainAccessible() throws Exception {
        mockMvc.perform(post("/auth/google/sign-in")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/auth/google/sign-in")).andExpect(status().isUnauthorized());
        mockMvc.perform(options("/products").header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk());
    }

    @Test
    void invalidTokensAreRejected() throws Exception {
        mockMvc.perform(get("/products").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        for (Map<String, ?> claims : java.util.List.<Map<String, ?>>of(
                Map.of("iss", "https://attacker.example"), Map.of("aud", "another-client"),
                Map.of("exp", Date.from(Instant.now().minusSeconds(300))),
                Map.of("email_verified", false), Map.of("email", ""), Map.of("sub", ""))) {
            mockMvc.perform(get("/products").header("Authorization", "Bearer " + TestConfiguration.token(claims)))
                    .andExpect(status().isUnauthorized());
        }
        String token = TestConfiguration.token(Map.of());
        SignedJWT parsed = SignedJWT.parse(token);
        SignedJWT forged = new SignedJWT(parsed.getHeader(), parsed.getJWTClaimsSet());
        forged.sign(new RSASSASigner(TestConfiguration.keyPair().getPrivate()));
        mockMvc.perform(get("/products").header("Authorization", "Bearer " + forged.serialize()))
                .andExpect(status().isUnauthorized());
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
