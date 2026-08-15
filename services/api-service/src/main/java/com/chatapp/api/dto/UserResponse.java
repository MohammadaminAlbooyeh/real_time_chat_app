package com.chatapp.api.dto;

import com.chatapp.api.model.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String username,
    String email,
    String displayName,
    String avatarUrl,
    String bio,
    boolean isOnline,
    LocalDateTime lastSeen,
    LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getDisplayName(),
            user.getAvatarUrl(),
            user.getBio(),
            user.isOnline(),
            user.getLastSeen(),
            user.getCreatedAt()
        );
    }
}
