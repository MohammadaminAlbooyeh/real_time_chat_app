package com.chatapp.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MessageSend(
    @NotBlank @Size(max = 5000) String content,
    String messageType,
    UUID replyToId
) {
    public MessageSend {
        messageType = messageType != null ? messageType : "text";
    }
}
