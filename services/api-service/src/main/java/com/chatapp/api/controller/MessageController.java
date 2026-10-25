package com.chatapp.api.controller;

import com.chatapp.api.dto.*;
import com.chatapp.api.model.User;
import com.chatapp.api.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms/{roomId}/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<MessageResponse>> getMessages(
            @PathVariable UUID roomId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) LocalDateTime before,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(messageService.getMessages(roomId, user, limit, offset, before));
    }

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable UUID roomId,
            @Valid @RequestBody MessageSend payload,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(messageService.sendMessage(roomId, payload, user));
    }

    @PutMapping("/{messageId}")
    public ResponseEntity<MessageResponse> editMessage(
            @PathVariable UUID roomId,
            @PathVariable UUID messageId,
            @Valid @RequestBody MessageEdit payload,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(messageService.editMessage(roomId, messageId, payload, user));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable UUID roomId,
            @PathVariable UUID messageId,
            @AuthenticationPrincipal User user) {
        messageService.deleteMessage(roomId, messageId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pinned")
    public ResponseEntity<java.util.List<MessageResponse>> getPinnedMessages(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(messageService.getPinnedMessages(roomId, user));
    }

    @PostMapping("/{messageId}/pin")
    public ResponseEntity<MessageResponse> pinMessage(
            @PathVariable UUID roomId,
            @PathVariable UUID messageId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(messageService.pinMessage(roomId, messageId, user));
    }

    @DeleteMapping("/{messageId}/pin")
    public ResponseEntity<Void> unpinMessage(
            @PathVariable UUID roomId,
            @PathVariable UUID messageId,
            @AuthenticationPrincipal User user) {
        messageService.unpinMessage(roomId, messageId, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{messageId}/forward")
    public ResponseEntity<MessageResponse> forwardMessage(
            @PathVariable UUID roomId,
            @PathVariable UUID messageId,
            @Valid @RequestBody MessageForward payload,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(messageService.forwardMessage(roomId, messageId, payload.targetRoomId(), user));
    }

    @PostMapping("/{messageId}/reactions")
    public ResponseEntity<ReactionResponse> addReaction(
            @PathVariable UUID roomId,
            @PathVariable UUID messageId,
            @Valid @RequestBody MessageReactionAdd payload,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(messageService.addReaction(roomId, messageId, payload, user));
    }

    @DeleteMapping("/{messageId}/reactions/{reactionId}")
    public ResponseEntity<Void> removeReaction(
            @PathVariable UUID roomId,
            @PathVariable UUID messageId,
            @PathVariable UUID reactionId,
            @AuthenticationPrincipal User user) {
        messageService.removeReaction(roomId, messageId, reactionId, user);
        return ResponseEntity.noContent().build();
    }
}
