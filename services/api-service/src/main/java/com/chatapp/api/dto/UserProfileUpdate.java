package com.chatapp.api.dto;

import jakarta.validation.constraints.Size;

public record UserProfileUpdate(
    @Size(max = 100) String displayName,
    @Size(max = 500) String avatarUrl,
    @Size(max = 500) String bio
) {}
