package com.chatapp.api.controller;

import com.chatapp.api.dto.MessageResponse;
import com.chatapp.api.model.User;
import com.chatapp.api.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final MessageService messageService;

    public SearchController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/messages")
    public ResponseEntity<List<MessageResponse>> searchMessages(
            @RequestParam String q,
            @RequestParam(required = false) UUID roomId,
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(messageService.searchMessages(q, roomId, user, limit));
    }
}
