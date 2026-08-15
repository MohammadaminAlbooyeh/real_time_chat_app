package com.chatapp.api.repository;

import com.chatapp.api.model.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomMemberRepository extends JpaRepository<RoomMember, UUID> {
    List<RoomMember> findByUserId(UUID userId);
    Optional<RoomMember> findByRoomIdAndUserId(UUID roomId, UUID userId);
    boolean existsByRoomIdAndUserId(UUID roomId, UUID userId);
    long countByUserId(UUID userId);
    void deleteByRoomIdAndUserId(UUID roomId, UUID userId);
}
