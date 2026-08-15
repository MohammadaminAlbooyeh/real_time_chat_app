package com.chatapp.api.controller;

import com.chatapp.api.dto.*;
import com.chatapp.api.model.User;
import com.chatapp.api.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody RoomCreate payload,
                                                     @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.createRoom(payload, user));
    }

    @GetMapping
    public ResponseEntity<List<RoomResponse>> listRooms(@RequestParam(defaultValue = "50") int limit,
                                                          @RequestParam(defaultValue = "0") int offset,
                                                          @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(roomService.listRooms(user, limit, offset));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomDetailResponse> getRoom(@PathVariable UUID roomId,
                                                       @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(roomService.getRoom(roomId, user));
    }

    @PutMapping("/{roomId}")
    public ResponseEntity<RoomResponse> updateRoom(@PathVariable UUID roomId,
                                                    @Valid @RequestBody RoomUpdate payload,
                                                    @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(roomService.updateRoom(roomId, payload, user));
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable UUID roomId,
                                           @AuthenticationPrincipal User user) {
        roomService.deleteRoom(roomId, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{roomId}/members")
    public ResponseEntity<Map<String, String>> addMember(@PathVariable UUID roomId,
                                                          @Valid @RequestBody AddMemberRequest payload,
                                                          @AuthenticationPrincipal User user) {
        roomService.addMember(roomId, payload.userId(), user);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Member added successfully"));
    }

    @DeleteMapping("/{roomId}/members/{userId}")
    public ResponseEntity<Map<String, String>> removeMember(@PathVariable UUID roomId,
                                                             @PathVariable UUID userId,
                                                             @AuthenticationPrincipal User user) {
        roomService.removeMember(roomId, userId, user);
        return ResponseEntity.ok(Map.of("message", "Member removed successfully"));
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Map<String, String>> leaveRoom(@PathVariable UUID roomId,
                                                          @AuthenticationPrincipal User user) {
        roomService.leaveRoom(roomId, user);
        return ResponseEntity.ok(Map.of("message", "Left room successfully"));
    }
}
