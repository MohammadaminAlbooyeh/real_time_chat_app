package com.chatapp.api.dto;

import com.chatapp.api.model.Room;

import java.time.LocalDateTime;
import java.util.UUID;

public record RoomResponse(
    UUID id,
    String name,
    String description,
    boolean isPrivate,
    UUID createdById,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    int memberCount,
    String lastMessage,
    LocalDateTime lastMessageAt
) {
    public static RoomResponse from(Room room, int memberCount, String lastMessage, LocalDateTime lastMessageAt) {
        return new RoomResponse(
            room.getId(),
            room.getName(),
            room.getDescription(),
            room.isPrivate(),
            room.getCreatedById(),
            room.getCreatedAt(),
            room.getUpdatedAt(),
            memberCount,
            lastMessage,
            lastMessageAt
        );
    }
}
