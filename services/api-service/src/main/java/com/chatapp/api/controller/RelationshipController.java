package com.chatapp.api.controller;

import com.chatapp.api.model.Relationship;
import com.chatapp.api.model.User;
import com.chatapp.api.repository.RelationshipRepository;
import com.chatapp.api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/relationships")
public class RelationshipController {

    private final RelationshipRepository relationshipRepository;
    private final UserRepository userRepository;

    public RelationshipController(RelationshipRepository relationshipRepository,
                                   UserRepository userRepository) {
        this.relationshipRepository = relationshipRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/request/{userId}")
    public ResponseEntity<Map<String, String>> sendRequest(@PathVariable UUID userId,
                                                            @AuthenticationPrincipal User user) {
        if (userId.equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot send friend request to yourself");
        }

        userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        var existing = relationshipRepository.findByRequesterIdAndAddresseeId(user.getId(), userId);
        if (existing.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Relationship already exists");
        }

        var reverse = relationshipRepository.findByRequesterIdAndAddresseeId(userId, user.getId());
        if (reverse.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Relationship already exists");
        }

        if (relationshipRepository.existsBlock(userId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot send a request to this user");
        }

        Relationship rel = new Relationship();
        rel.setRequesterId(user.getId());
        rel.setAddresseeId(userId);
        rel.setStatus("pending");
        relationshipRepository.save(rel);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Friend request sent"));
    }

    @PostMapping("/respond/{relationshipId}")
    public ResponseEntity<Map<String, String>> respondRequest(@PathVariable UUID relationshipId,
                                                               @RequestParam String action,
                                                               @AuthenticationPrincipal User user) {
        Relationship rel = relationshipRepository.findById(relationshipId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Relationship not found"));

        if (!rel.getAddresseeId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to respond to this request");
        }
        if (!"pending".equals(rel.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request already responded to");
        }

        if ("accept".equals(action)) {
            rel.setStatus("accepted");
            relationshipRepository.save(rel);
            return ResponseEntity.ok(Map.of("message", "Friend request accepted"));
        } else if ("reject".equals(action)) {
            relationshipRepository.delete(rel);
            return ResponseEntity.ok(Map.of("message", "Friend request rejected"));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Action must be 'accept' or 'reject'");
        }
    }

    @GetMapping("/friends")
    public ResponseEntity<List<Map<String, Object>>> listFriends(@AuthenticationPrincipal User user) {
        List<Relationship> rels = relationshipRepository.findByUserIdAndStatus(user.getId(), "accepted");

        List<Map<String, Object>> friends = rels.stream().map(rel -> {
            UUID friendId = rel.getRequesterId().equals(user.getId())
                ? rel.getAddresseeId() : rel.getRequesterId();
            var friend = userRepository.findById(friendId).orElse(null);
            if (friend == null) return null;
            return Map.<String, Object>of(
                "id", friend.getId(),
                "username", friend.getUsername(),
                "displayName", friend.getDisplayName(),
                "avatarUrl", friend.getAvatarUrl(),
                "isOnline", friend.isOnline()
            );
        }).filter(f -> f != null).toList();

        return ResponseEntity.ok(friends);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Map<String, Object>>> listPending(@AuthenticationPrincipal User user) {
        List<Relationship> rels = relationshipRepository.findByAddresseeIdAndStatus(user.getId(), "pending");

        List<Map<String, Object>> requests = rels.stream().map(rel -> {
            var requester = userRepository.findById(rel.getRequesterId()).orElse(null);
            if (requester == null) return null;
            return Map.<String, Object>of(
                "id", rel.getId(),
                "user", Map.of(
                    "id", requester.getId(),
                    "username", requester.getUsername(),
                    "displayName", requester.getDisplayName(),
                    "avatarUrl", requester.getAvatarUrl()
                ),
                "createdAt", rel.getCreatedAt()
            );
        }).filter(r -> r != null).toList();

        return ResponseEntity.ok(requests);
    }

    @PostMapping("/block/{userId}")
    public ResponseEntity<Map<String, String>> blockUser(@PathVariable UUID userId,
                                                           @AuthenticationPrincipal User user) {
        if (userId.equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot block yourself");
        }
        userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Remove any existing relationship between the two users (friend/pending) before blocking.
        relationshipRepository.findByRequesterIdAndAddresseeId(user.getId(), userId)
            .ifPresent(relationshipRepository::delete);
        relationshipRepository.findByRequesterIdAndAddresseeId(userId, user.getId())
            .ifPresent(relationshipRepository::delete);

        Relationship rel = new Relationship();
        rel.setRequesterId(user.getId());
        rel.setAddresseeId(userId);
        rel.setStatus("blocked");
        relationshipRepository.save(rel);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "User blocked"));
    }

    @DeleteMapping("/block/{userId}")
    public ResponseEntity<Map<String, String>> unblockUser(@PathVariable UUID userId,
                                                             @AuthenticationPrincipal User user) {
        Relationship rel = relationshipRepository.findByRequesterIdAndAddresseeId(user.getId(), userId)
            .filter(r -> "blocked".equals(r.getStatus()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Block not found"));

        relationshipRepository.delete(rel);
        return ResponseEntity.ok(Map.of("message", "User unblocked"));
    }

    @GetMapping("/blocked")
    public ResponseEntity<List<Map<String, Object>>> listBlocked(@AuthenticationPrincipal User user) {
        List<Relationship> rels = relationshipRepository.findByRequesterIdAndStatus(user.getId(), "blocked");

        List<Map<String, Object>> blocked = rels.stream().map(rel -> {
            var blockedUser = userRepository.findById(rel.getAddresseeId()).orElse(null);
            if (blockedUser == null) return null;
            return Map.<String, Object>of(
                "id", blockedUser.getId(),
                "username", blockedUser.getUsername(),
                "displayName", blockedUser.getDisplayName(),
                "avatarUrl", blockedUser.getAvatarUrl(),
                "blockedAt", rel.getCreatedAt()
            );
        }).filter(b -> b != null).toList();

        return ResponseEntity.ok(blocked);
    }

    @DeleteMapping("/{relationshipId}")
    public ResponseEntity<Map<String, String>> removeFriend(@PathVariable UUID relationshipId,
                                                             @AuthenticationPrincipal User user) {
        Relationship rel = relationshipRepository.findById(relationshipId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Relationship not found"));

        if (!rel.getRequesterId().equals(user.getId()) && !rel.getAddresseeId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized");
        }

        relationshipRepository.delete(rel);
        return ResponseEntity.ok(Map.of("message", "Friend removed"));
    }
}
