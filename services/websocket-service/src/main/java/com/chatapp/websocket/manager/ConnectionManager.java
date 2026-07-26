package com.chatapp.websocket.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Equivalent of connection_manager.py: tracks live sessions per user and
 * room membership, and fans out messages to them. Sessions are wrapped in
 * ConcurrentWebSocketSessionDecorator because the same session may be
 * written to concurrently from the WS handler thread and the Redis
 * subscriber thread.
 */
@Component
public class ConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);

    private final Map<String, WebSocketSession> activeConnections = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> roomUsers = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public ConnectionManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void connect(String userId, WebSocketSession session) {
        activeConnections.put(userId, new ConcurrentWebSocketSessionDecorator(session, 5000, 65536));
    }

    public void disconnect(String userId) {
        activeConnections.remove(userId);
        for (Set<String> users : roomUsers.values()) {
            users.remove(userId);
        }
        roomUsers.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public void sendPersonal(String userId, Object message) {
        WebSocketSession session = activeConnections.get(userId);
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (IOException e) {
            log.warn("Failed to send message to user {}: {}", userId, e.getMessage());
        }
    }

    public void broadcastToRoom(String roomId, Object message, String exclude) {
        Set<String> users = roomUsers.getOrDefault(roomId, Set.of());
        for (String userId : users) {
            if (userId.equals(exclude)) {
                continue;
            }
            sendPersonal(userId, message);
        }
    }

    public void joinRoom(String userId, String roomId) {
        roomUsers.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(userId);
    }

    public void leaveRoom(String userId, String roomId) {
        Set<String> users = roomUsers.get(roomId);
        if (users == null) {
            return;
        }
        users.remove(userId);
        if (users.isEmpty()) {
            roomUsers.remove(roomId);
        }
    }

    public List<String> getOnlineUsers() {
        return new ArrayList<>(activeConnections.keySet());
    }

    public boolean isOnline(String userId) {
        return activeConnections.containsKey(userId);
    }
}
