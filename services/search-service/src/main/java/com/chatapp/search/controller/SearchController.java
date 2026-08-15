package com.chatapp.search.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chatapp.search.client.SearchService;
import com.chatapp.search.dto.IndexMessageRequest;
import com.chatapp.search.dto.SearchResult;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping("/index")
    public ResponseEntity<Map<String, String>> indexMessage(@Valid @RequestBody IndexMessageRequest request) {
        searchService.indexMessage(
            request.getId(),
            request.getContent(),
            request.getRoomId(),
            request.getSenderId(),
            request.getSenderUsername(),
            request.getMessageType(),
            request.getCreatedAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("status", "indexed", "id", request.getId()));
    }

    @GetMapping("/messages")
    public SearchResult searchMessages(
            @RequestParam String q,
            @RequestParam(required = false) String roomId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        if (limit > 200) limit = 200;
        return searchService.searchMessages(q, roomId, limit, offset);
    }

    @DeleteMapping("/messages/{messageId}")
    public Map<String, String> deleteMessage(@PathVariable String messageId) {
        searchService.deleteMessage(messageId);
        return Map.of("status", "deleted", "id", messageId);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "search-service");
    }
}
