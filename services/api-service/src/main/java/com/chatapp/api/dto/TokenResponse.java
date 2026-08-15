package com.chatapp.api.dto;

public record TokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    UserResponse user
) {
    public TokenResponse(String accessToken, String refreshToken, UserResponse user) {
        this(accessToken, refreshToken, "bearer", user);
    }
}
