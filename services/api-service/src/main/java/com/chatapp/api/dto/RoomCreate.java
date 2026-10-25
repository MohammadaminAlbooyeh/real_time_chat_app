package com.chatapp.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoomCreate(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 500) String description,
    boolean isPrivate
) {
}
