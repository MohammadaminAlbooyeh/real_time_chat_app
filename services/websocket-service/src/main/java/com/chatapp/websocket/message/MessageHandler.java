package com.chatapp.websocket.message;

import com.chatapp.websocket.manager.ConnectionManager;
import com.chatapp.websocket.redis.RedisPublisher;
import com.chatapp.websocket.repository.MessageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Equivalent of message_handler.py: handles the "message:send", "typing:*"
 * and "room:join" / "room:leave" events coming off the WebSocket.
 */
@Component
public class MessageHandler {

    private final ConnectionManager connectionManager;
    private final RedisPublisher redisPublisher;
    private final StringRedisTemplate redisTemplate;
    private final MessageRepository messageRepository;

    public MessageHandler(ConnectionManager connectionManager,
                           RedisPublisher redisPublisher,
                           StringRedisTemplate redisTemplate,
                           MessageRepository messageRepository) {
        this.connectionManager = connectionManager;
        this.redisPublisher = redisPublisher;
        this.redisTemplate = redisTemplate;
        this.messageRepository = messageRepository;
    }

    public void handleSendMessage(JsonNode data, String userId, String username) {
        String roomId = text(data, "room_id");
        String content = data.path("content").asText("");
        String messageType = data.path("message_type").asText("text");
        String replyToId = text(data, "reply_to_id");

        MessageRepository.InsertedMessage inserted =
                messageRepository.insertMessage(roomId, userId, content, messageType, replyToId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", inserted.id());
        payload.put("room_id", roomId);
        payload.put("sender_id", userId);
        payload.put("sender_username", username);
        payload.put("content", content);
        payload.put("message_type", messageType);
        payload.put("reply_to_id", replyToId);
        payload.put("created_at", inserted.createdAt());
        payload.put("reactions", List.of());
        payload.put("attachments", List.of());

        Map<String, Object> event = Map.of("type", "message:new", "payload", payload);

        connectionManager.broadcastToRoom(roomId, event, userId);
        redisPublisher.publish("room:" + roomId, event);
    }

    public void handleTyping(JsonNode data, String userId, String username, boolean isTyping) {
        String roomId = text(data, "room_id");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("room_id", roomId);
        payload.put("user_id", userId);
        payload.put("username", username);
        payload.put("is_typing", isTyping);

        Map<String, Object> event = Map.of("type", "typing:indicator", "payload", payload);
        connectionManager.broadcastToRoom(roomId, event, userId);
    }

    public void handleJoinRoom(JsonNode data, String userId, String username) {
        String roomId = text(data, "room_id");
        connectionManager.joinRoom(userId, roomId);
        redisTemplate.opsForSet().add("room:" + roomId + ":members", userId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("user_id", userId);
        payload.put("username", username);
        payload.put("room_id", roomId);

        Map<String, Object> event = Map.of("type", "room:joined", "payload", payload);
        connectionManager.broadcastToRoom(roomId, event, userId);
    }

    public void handleLeaveRoom(JsonNode data, String userId, String username) {
        String roomId = text(data, "room_id");
        connectionManager.leaveRoom(userId, roomId);
        redisTemplate.opsForSet().remove("room:" + roomId + ":members", userId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("user_id", userId);
        payload.put("username", username);
        payload.put("room_id", roomId);

        Map<String, Object> event = Map.of("type", "room:left", "payload", payload);
        connectionManager.broadcastToRoom(roomId, event, userId);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value == null || value.isNull()) ? null : value.asText();
    }
}
