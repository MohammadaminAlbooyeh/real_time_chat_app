package com.chatapp.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageEdit(
    @NotBlank @Size(max = 5000) String content
) {}
