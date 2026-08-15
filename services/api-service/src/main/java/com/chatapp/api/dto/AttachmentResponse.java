package com.chatapp.api.dto;

import com.chatapp.api.model.MessageAttachment;

import java.time.LocalDateTime;
import java.util.UUID;

public record AttachmentResponse(
    UUID id,
    String fileName,
    String fileUrl,
    String fileType,
    long fileSize,
    LocalDateTime createdAt
) {
    public static AttachmentResponse from(MessageAttachment attachment) {
        return new AttachmentResponse(
            attachment.getId(),
            attachment.getFileName(),
            attachment.getFileUrl(),
            attachment.getFileType(),
            attachment.getFileSize(),
            attachment.getCreatedAt()
        );
    }
}
