package com.chatapp.websocket.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MessageRepository {

    private static final String INSERT_SQL = """
            INSERT INTO messages (room_id, sender_id, content, message_type, reply_to_id)
            VALUES (?::uuid, ?::uuid, ?, ?, ?::uuid)
            RETURNING id, created_at
            """;

    private final JdbcTemplate jdbcTemplate;

    public MessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public InsertedMessage insertMessage(String roomId, String senderId, String content,
                                          String messageType, String replyToId) {
        return jdbcTemplate.queryForObject(
                INSERT_SQL,
                (rs, rowNum) -> new InsertedMessage(
                        rs.getString("id"),
                        rs.getTimestamp("created_at").toInstant().toString()
                ),
                roomId, senderId, content, messageType, replyToId
        );
    }

    public record InsertedMessage(String id, String createdAt) {
    }
}
