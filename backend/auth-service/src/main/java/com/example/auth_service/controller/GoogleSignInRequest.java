package com.example.auth_service.controller;

import jakarta.validation.constraints.NotBlank;

/** Payload received from Google Identity Services after the user selects a Google account. */
public record GoogleSignInRequest(@NotBlank String credential) {
}
