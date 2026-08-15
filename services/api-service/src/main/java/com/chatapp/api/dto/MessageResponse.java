package com.chatapp.api.dto;

import com.chatapp.api.model.Message;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MessageResponse(
    UUID id,
    UUID roomId,
    UUID senderId,
    String senderUsername,
    String senderAvatar,
    String content,
    String messageType,
    UUID replyToId,
    String replyPreview,
    boolean isDeleted,
    LocalDateTime editedAt,
    LocalDateTime createdAt,
    List<ReactionResponse> reactions,
    List<AttachmentResponse> attachments
) {
    public static MessageResponse from(Message msg) {
        String content = msg.isDeleted() ? "[deleted]" : msg.getContent();
        String replyPreview = null;
        if (msg.getReplyTo() != null && !msg.getReplyTo().isDeleted()) {
            String rc = msg.getReplyTo().getContent();
            replyPreview = rc.length() > 100 ? rc.substring(0, 100) : rc;
        }
        List<ReactionResponse> reactions = List.of();
        if (msg.getReactions() != null) {
            reactions = msg.getReactions().stream().map(ReactionResponse::from).toList();
        }
        List<AttachmentResponse> attachments = List.of();
        if (msg.getAttachments() != null) {
            attachments = msg.getAttachments().stream().map(AttachmentResponse::from).toList();
        }
        return new MessageResponse(
            msg.getId(),
            msg.getRoomId(),
            msg.getSenderId(),
            msg.getSender() != null ? msg.getSender().getUsername() : null,
            msg.getSender() != null ? msg.getSender().getAvatarUrl() : null,
            content,
            msg.getMessageType(),
            msg.getReplyToId(),
            replyPreview,
            msg.isDeleted(),
            msg.getEditedAt(),
            msg.getCreatedAt(),
            reactions,
            attachments
        );
    }
}
