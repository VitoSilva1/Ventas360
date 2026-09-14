package com.example.auth_service.controller;

import com.example.auth_service.models.GoogleProfileResponse;
import com.example.auth_service.service.GoogleAuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private final GoogleAuthenticationService authenticationService;

    public AuthenticationController(GoogleAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/google/sign-in")
    public ResponseEntity<GoogleProfileResponse> signIn(@Valid @RequestBody GoogleSignInRequest request) {
        return ResponseEntity.ok(authenticationService.signIn(request.credential()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
