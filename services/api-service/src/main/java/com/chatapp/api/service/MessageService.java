package com.chatapp.api.service;

import com.chatapp.api.dto.*;
import com.chatapp.api.model.*;
import com.chatapp.api.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final MessageReactionRepository messageReactionRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RedisPublisherService redisPublisherService;

    public MessageService(MessageRepository messageRepository,
                          MessageReactionRepository messageReactionRepository,
                          MessageAttachmentRepository messageAttachmentRepository,
                          RoomMemberRepository roomMemberRepository,
                          RedisPublisherService redisPublisherService) {
        this.messageRepository = messageRepository;
        this.messageReactionRepository = messageReactionRepository;
        this.messageAttachmentRepository = messageAttachmentRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.redisPublisherService = redisPublisherService;
    }

    private void verifyMembership(UUID roomId, UUID userId) {
        if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this room");
        }
    }

    public PaginatedResponse<MessageResponse> getMessages(UUID roomId, User user, int limit, int offset,
                                                            LocalDateTime before) {
        verifyMembership(roomId, user.getId());

        Pageable pageable = PageRequest.of(offset / limit, limit);
        Page<Message> page = messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable);
        long total = page.getTotalElements();

        List<MessageResponse> items = page.getContent().stream()
            .map(this::toMessageResponse)
            .toList();

        return PaginatedResponse.of(items, total, limit, offset);
    }

    @Transactional
    public MessageResponse sendMessage(UUID roomId, MessageSend payload, User user) {
        verifyMembership(roomId, user.getId());

        if (payload.replyToId() != null) {
            Message replyMsg = messageRepository.findByIdAndRoomId(payload.replyToId(), roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reply message"));
        }

        Message msg = new Message();
        msg.setRoomId(roomId);
        msg.setSenderId(user.getId());
        msg.setContent(payload.content());
        msg.setMessageType(payload.messageType());
        msg.setReplyToId(payload.replyToId());
        msg = messageRepository.save(msg);
        msg.setSender(user);

        return toMessageResponse(msg);
    }

    @Transactional
    public MessageResponse editMessage(UUID roomId, UUID messageId, MessageEdit payload, User user) {
        Message msg = messageRepository.findByIdAndRoomId(messageId, roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if (!msg.getSenderId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Can only edit your own messages");
        }
        if (msg.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot edit a deleted message");
        }

        msg.setContent(payload.content());
        msg.setEditedAt(LocalDateTime.now());
        msg = messageRepository.save(msg);

        MessageResponse response = toMessageResponse(msg);
        redisPublisherService.publish("room:" + roomId, Map.of(
            "type", "message:updated",
            "payload", response
        ));
        return response;
    }

    @Transactional
    public void deleteMessage(UUID roomId, UUID messageId, User user) {
        Message msg = messageRepository.findByIdAndRoomId(messageId, roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if (!msg.getSenderId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Can only delete your own messages");
        }

        msg.setDeleted(true);
        messageRepository.save(msg);

        redisPublisherService.publish("room:" + roomId, Map.of(
            "type", "message:deleted",
            "payload", Map.of("roomId", roomId.toString(), "id", messageId.toString())
        ));
    }

    @Transactional
    public ReactionResponse addReaction(UUID roomId, UUID messageId, MessageReactionAdd payload, User user) {
        verifyMembership(roomId, user.getId());

        Message msg = messageRepository.findByIdAndRoomId(messageId, roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        var existing = messageReactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, user.getId(), payload.emoji());
        if (existing.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reaction already exists");
        }

        MessageReaction reaction = new MessageReaction();
        reaction.setMessageId(messageId);
        reaction.setUserId(user.getId());
        reaction.setEmoji(payload.emoji());
        reaction = messageReactionRepository.save(reaction);
        reaction.setUser(user);

        ReactionResponse reactionResp = ReactionResponse.from(reaction);

        redisPublisherService.publish("room:" + roomId, Map.of(
            "type", "message:reaction",
            "payload", Map.of(
                "id", reaction.getId().toString(),
                "roomId", roomId.toString(),
                "messageId", messageId.toString(),
                "userId", user.getId().toString(),
                "username", user.getUsername(),
                "emoji", reaction.getEmoji(),
                "createdAt", reaction.getCreatedAt().toString()
            )
        ));

        return reactionResp;
    }

    @Transactional
    public void removeReaction(UUID roomId, UUID messageId, UUID reactionId, User user) {
        MessageReaction reaction = messageReactionRepository.findById(reactionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reaction not found"));

        if (!reaction.getMessageId().equals(messageId) || !reaction.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reaction not found");
        }

        messageReactionRepository.delete(reaction);

        redisPublisherService.publish("room:" + roomId, Map.of(
            "type", "message:reaction",
            "payload", Map.of(
                "roomId", roomId.toString(),
                "messageId", messageId.toString(),
                "reactionId", reactionId.toString(),
                "userId", user.getId().toString()
            )
        ));
    }

    @Transactional
    public MessageResponse pinMessage(UUID roomId, UUID messageId, User user) {
        verifyMembership(roomId, user.getId());

        Message msg = messageRepository.findByIdAndRoomId(messageId, roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if (msg.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot pin a deleted message");
        }

        msg.setPinned(true);
        msg.setPinnedAt(LocalDateTime.now());
        msg.setPinnedBy(user.getId());
        msg = messageRepository.save(msg);

        MessageResponse response = toMessageResponse(msg);
        redisPublisherService.publish("room:" + roomId, Map.of(
            "type", "message:pinned",
            "payload", response
        ));
        return response;
    }

    @Transactional
    public void unpinMessage(UUID roomId, UUID messageId, User user) {
        verifyMembership(roomId, user.getId());

        Message msg = messageRepository.findByIdAndRoomId(messageId, roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        msg.setPinned(false);
        msg.setPinnedAt(null);
        msg.setPinnedBy(null);
        messageRepository.save(msg);

        redisPublisherService.publish("room:" + roomId, Map.of(
            "type", "message:unpinned",
            "payload", Map.of("roomId", roomId.toString(), "id", messageId.toString())
        ));
    }

    public List<MessageResponse> getPinnedMessages(UUID roomId, User user) {
        verifyMembership(roomId, user.getId());
        return messageRepository.findByRoomIdAndIsPinnedTrueOrderByPinnedAtDesc(roomId)
            .stream()
            .map(this::toMessageResponse)
            .toList();
    }

    @Transactional
    public MessageResponse forwardMessage(UUID sourceRoomId, UUID messageId, UUID targetRoomId, User user) {
        verifyMembership(sourceRoomId, user.getId());
        verifyMembership(targetRoomId, user.getId());

        Message original = messageRepository.findByIdAndRoomId(messageId, sourceRoomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if (original.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot forward a deleted message");
        }

        Message forwarded = new Message();
        forwarded.setRoomId(targetRoomId);
        forwarded.setSenderId(user.getId());
        forwarded.setContent(original.getContent());
        forwarded.setMessageType(original.getMessageType());
        forwarded.setForwardedFromId(original.getId());
        forwarded = messageRepository.save(forwarded);
        forwarded.setSender(user);

        MessageResponse response = toMessageResponse(forwarded);
        redisPublisherService.publish("room:" + targetRoomId, Map.of(
            "type", "message:new",
            "payload", response
        ));
        return response;
    }

    public List<MessageResponse> searchMessages(String query, UUID roomId, User user, int limit) {
        List<UUID> roomIds;
        if (roomId != null) {
            roomIds = List.of(roomId);
        } else {
            roomIds = roomMemberRepository.findByUserId(user.getId())
                .stream()
                .map(RoomMember::getRoomId)
                .toList();
        }

        return messageRepository.searchMessages(query, roomIds)
            .stream()
            .limit(limit)
            .map(this::toMessageResponse)
            .toList();
    }

    private MessageResponse toMessageResponse(Message msg) {
        return MessageResponse.from(msg);
    }
}
