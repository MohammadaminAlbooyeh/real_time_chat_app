package com.chatapp.api.dto;

import com.chatapp.api.model.User;

import java.util.UUID;

public record UserSearchResult(
    UUID id,
    String username,
    String displayName,
    String avatarUrl,
    boolean isOnline
) {
    public static UserSearchResult from(User user) {
        return new UserSearchResult(
            user.getId(),
            user.getUsername(),
            user.getDisplayName(),
            user.getAvatarUrl(),
            user.isOnline()
        );
    }
}
