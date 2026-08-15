package com.chatapp.api.controller;

import com.chatapp.api.dto.*;
import com.chatapp.api.model.User;
import com.chatapp.api.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody UserCreate payload) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(payload));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody UserLogin payload) {
        return ResponseEntity.ok(userService.login(payload));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest payload) {
        return ResponseEntity.ok(userService.refresh(payload));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
