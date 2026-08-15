package com.chatapp.api.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RoomDetailResponse(
    UUID id,
    String name,
    String description,
    boolean isPrivate,
    UUID createdById,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    int memberCount,
    List<RoomMemberResponse> members
) {
    public static RoomDetailResponse from(
            UUID id, String name, String description, boolean isPrivate,
            UUID createdById, LocalDateTime createdAt, LocalDateTime updatedAt,
            int memberCount, List<RoomMemberResponse> members) {
        return new RoomDetailResponse(
            id, name, description, isPrivate, createdById, createdAt, updatedAt,
            memberCount, members
        );
    }
}
