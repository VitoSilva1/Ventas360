package com.example.api_gateway.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "google.oauth")
public record GoogleOAuthProperties(@NotBlank String clientId) {
}
