package com.chatapp.api.dto;

import java.util.List;

public record PaginatedResponse<T>(
    List<T> items,
    long total,
    int limit,
    int offset,
    boolean hasMore
) {
    public static <T> PaginatedResponse<T> of(List<T> items, long total, int limit, int offset) {
        return new PaginatedResponse<>(items, total, limit, offset, (offset + limit) < total);
    }
}
