package com.chatapp.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageReactionAdd(
    @NotBlank @Size(max = 50) String emoji
) {}
