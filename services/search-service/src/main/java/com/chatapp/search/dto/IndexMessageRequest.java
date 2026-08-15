package com.chatapp.search.dto;

import jakarta.validation.constraints.NotBlank;

public class IndexMessageRequest {
    @NotBlank
    private String id;

    @NotBlank
    private String content;

    @NotBlank
    private String roomId;

    @NotBlank
    private String senderId;

    private String senderUsername = "";
    private String messageType = "text";
    private String createdAt;

    public @NotBlank String getId() { return id; }
    public void setId(@NotBlank String id) { this.id = id; }
    public @NotBlank String getContent() { return content; }
    public void setContent(@NotBlank String content) { this.content = content; }
    public @NotBlank String getRoomId() { return roomId; }
    public void setRoomId(@NotBlank String roomId) { this.roomId = roomId; }
    public @NotBlank String getSenderId() { return senderId; }
    public void setSenderId(@NotBlank String senderId) { this.senderId = senderId; }
    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
