package com.chatapp.api.repository;

import com.chatapp.api.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    Page<Message> findByRoomIdOrderByCreatedAtDesc(UUID roomId, Pageable pageable);
    Optional<Message> findByIdAndRoomId(UUID id, UUID roomId);
    List<Message> findByRoomIdAndIsPinnedTrueOrderByPinnedAtDesc(UUID roomId);

    @Query("SELECT m FROM Message m WHERE m.roomId IN :roomIds AND m.isDeleted = false AND LOWER(m.content) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY m.createdAt DESC")
    List<Message> searchMessages(@Param("q") String q, @Param("roomIds") List<UUID> roomIds);
}
