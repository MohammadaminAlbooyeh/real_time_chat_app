package com.chatapp.api.repository;

import com.chatapp.api.model.Relationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RelationshipRepository extends JpaRepository<Relationship, UUID> {
    Optional<Relationship> findByRequesterIdAndAddresseeId(UUID requesterId, UUID addresseeId);

    @Query("SELECT r FROM Relationship r WHERE (r.requesterId = :userId OR r.addresseeId = :userId) AND r.status = :status")
    List<Relationship> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") String status);

    List<Relationship> findByAddresseeIdAndStatus(UUID addresseeId, String status);
    List<Relationship> findByRequesterIdAndStatus(UUID requesterId, String status);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Relationship r " +
           "WHERE r.requesterId = :blockerId AND r.addresseeId = :blockedId AND r.status = 'blocked'")
    boolean existsBlock(@Param("blockerId") UUID blockerId, @Param("blockedId") UUID blockedId);
}
