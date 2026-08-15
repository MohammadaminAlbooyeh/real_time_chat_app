package com.chatapp.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChange(
    @NotBlank String currentPassword,
    @NotBlank @Size(min = 8, max = 128) String newPassword
) {}
