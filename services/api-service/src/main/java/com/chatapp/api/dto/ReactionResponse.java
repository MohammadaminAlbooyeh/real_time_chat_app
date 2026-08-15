package com.chatapp.api.dto;

import com.chatapp.api.model.MessageReaction;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReactionResponse(
    UUID id,
    UUID userId,
    String username,
    String emoji,
    LocalDateTime createdAt
) {
    public static ReactionResponse from(MessageReaction reaction) {
        return new ReactionResponse(
            reaction.getId(),
            reaction.getUserId(),
            reaction.getUser().getUsername(),
            reaction.getEmoji(),
            reaction.getCreatedAt()
        );
    }
}
