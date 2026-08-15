package com.chatapp.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserLogin(
    @Email @NotBlank String email,
    @NotBlank String password
) {}
