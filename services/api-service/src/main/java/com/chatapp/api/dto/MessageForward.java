package com.chatapp.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MessageForward(
    @NotNull UUID targetRoomId
) {
}
