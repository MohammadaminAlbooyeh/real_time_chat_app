package com.chatapp.api.dto;

import com.chatapp.api.model.RoomMember;

import java.time.LocalDateTime;
import java.util.UUID;

public record RoomMemberResponse(
    UUID id,
    UUID userId,
    String username,
    String displayName,
    String avatarUrl,
    String role,
    LocalDateTime joinedAt
) {
    public static RoomMemberResponse from(RoomMember member) {
        return new RoomMemberResponse(
            member.getId(),
            member.getUserId(),
            member.getUser().getUsername(),
            member.getUser().getDisplayName(),
            member.getUser().getAvatarUrl(),
            member.getRole(),
            member.getCreatedAt()
        );
    }
}
