package com.chatapp.api.service;

import com.chatapp.api.config.properties.ApiProperties;
import com.chatapp.api.dto.*;
import com.chatapp.api.model.*;
import com.chatapp.api.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final MessageRepository messageRepository;
    private final ApiProperties apiProperties;

    public RoomService(RoomRepository roomRepository, RoomMemberRepository roomMemberRepository,
                       MessageRepository messageRepository, ApiProperties apiProperties) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.messageRepository = messageRepository;
        this.apiProperties = apiProperties;
    }

    @Transactional
    public RoomResponse createRoom(RoomCreate payload, User user) {
        long roomCount = roomMemberRepository.countByUserId(user.getId());
        if (roomCount >= 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum rooms limit reached");
        }

        Room room = new Room();
        room.setName(payload.name());
        room.setDescription(payload.description());
        room.setPrivate(payload.isPrivate());
        room.setCreatedById(user.getId());
        room = roomRepository.save(room);

        RoomMember member = new RoomMember();
        member.setRoomId(room.getId());
        member.setUserId(user.getId());
        member.setRole("admin");
        roomMemberRepository.save(member);

        return RoomResponse.from(room, 1, null, null);
    }

    public List<RoomResponse> listRooms(User user, int limit, int offset) {
        List<Room> rooms = roomRepository.findByUserId(user.getId());
        return rooms.stream()
            .skip(offset)
            .limit(limit)
            .map(room -> {
                int memberCount = room.getMembers().size();
                String lastMessage = null;
                LocalDateTime lastMessageAt = null;
                var messages = room.getMessages().stream()
                    .filter(m -> !m.isDeleted())
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .toList();
                if (!messages.isEmpty()) {
                    lastMessage = messages.get(0).getContent();
                    lastMessageAt = messages.get(0).getCreatedAt();
                }
                return RoomResponse.from(room, memberCount, lastMessage, lastMessageAt);
            })
            .toList();
    }

    public RoomDetailResponse getRoom(UUID roomId, User user) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        boolean isMember = roomMemberRepository.existsByRoomIdAndUserId(roomId, user.getId());
        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this room");
        }

        List<RoomMemberResponse> members = room.getMembers().stream()
            .map(RoomMemberResponse::from)
            .toList();

        return RoomDetailResponse.from(
            room.getId(), room.getName(), room.getDescription(), room.isPrivate(),
            room.getCreatedById(), room.getCreatedAt(), room.getUpdatedAt(),
            members.size(), members
        );
    }

    @Transactional
    public RoomResponse updateRoom(UUID roomId, RoomUpdate payload, User user) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, user.getId())
            .orElse(null);
        if (member == null || !"admin".equals(member.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only room admins can update the room");
        }

        if (payload.name() != null) {
            room.setName(payload.name());
        }
        if (payload.description() != null) {
            room.setDescription(payload.description());
        }
        if (payload.isPrivate() != null) {
            room.setPrivate(payload.isPrivate());
        }
        room.setUpdatedAt(LocalDateTime.now());
        room = roomRepository.save(room);

        return RoomResponse.from(room, room.getMembers().size(), null, null);
    }

    @Transactional
    public void deleteRoom(UUID roomId, User user) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        if (!room.getCreatedById().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the room creator can delete the room");
        }
        roomRepository.delete(room);
    }

    @Transactional
    public void addMember(UUID roomId, UUID targetUserId, User user) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, user.getId())
            .orElse(null);
        if (member == null || !"admin".equals(member.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can add members");
        }

        if (roomMemberRepository.existsByRoomIdAndUserId(roomId, targetUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member");
        }

        RoomMember newMember = new RoomMember();
        newMember.setRoomId(roomId);
        newMember.setUserId(targetUserId);
        roomMemberRepository.save(newMember);
    }

    @Transactional
    public void removeMember(UUID roomId, UUID targetUserId, User user) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, user.getId())
            .orElse(null);
        if (member == null || !"admin".equals(member.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can remove members");
        }

        RoomMember target = roomMemberRepository.findByRoomIdAndUserId(roomId, targetUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not a member of this room"));

        roomMemberRepository.delete(target);
    }

    @Transactional
    public void leaveRoom(UUID roomId, User user) {
        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "You are not a member of this room"));

        roomMemberRepository.delete(member);
    }
}
