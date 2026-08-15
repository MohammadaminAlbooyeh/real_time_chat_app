package com.chatapp.api.dto;

import jakarta.validation.constraints.Size;

public record RoomUpdate(
    @Size(max = 100) String name,
    @Size(max = 500) String description,
    Boolean isPrivate
) {}
