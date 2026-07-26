package com.chatapp.websocket.presence;

import com.chatapp.websocket.manager.ConnectionManager;
import com.chatapp.websocket.redis.RedisPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PresenceTracker {

    private static final String GLOBAL_ROOM = "__global__";

    private final ConnectionManager connectionManager;
    private final RedisPublisher redisPublisher;
    private final StringRedisTemplate redisTemplate;

    public PresenceTracker(ConnectionManager connectionManager,
                            RedisPublisher redisPublisher,
                            StringRedisTemplate redisTemplate) {
        this.connectionManager = connectionManager;
        this.redisPublisher = redisPublisher;
        this.redisTemplate = redisTemplate;
    }

    public void setOnline(String userId, String username) {
        broadcastPresence(userId, username, "online");
    }

    public void setOffline(String userId, String username) {
        broadcastPresence(userId, username, "offline");
    }

    private void broadcastPresence(String userId, String username, String status) {
        redisTemplate.opsForValue().set("presence:" + userId, status);

        Map<String, Object> payload = Map.of(
                "user_id", userId,
                "username", username,
                "status", status
        );
        Map<String, Object> event = Map.of("type", "presence:" + status, "payload", payload);

        redisPublisher.publish("presence", event);
        connectionManager.broadcastToRoom(GLOBAL_ROOM, event, null);
    }
}
