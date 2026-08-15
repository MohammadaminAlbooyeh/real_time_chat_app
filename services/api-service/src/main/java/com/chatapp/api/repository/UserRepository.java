package com.chatapp.api.repository;

import com.chatapp.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u WHERE (LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :q, '%'))) AND u.id <> :excludeId")
    List<User> searchByUsernameOrDisplayName(@Param("q") String q, @Param("excludeId") UUID excludeId);
}
