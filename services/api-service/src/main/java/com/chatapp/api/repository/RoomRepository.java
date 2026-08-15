package com.chatapp.api.repository;

import com.chatapp.api.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    @Query("SELECT r FROM Room r JOIN r.members m WHERE m.userId = :userId ORDER BY r.updatedAt DESC")
    List<Room> findByUserId(@Param("userId") UUID userId);
}
