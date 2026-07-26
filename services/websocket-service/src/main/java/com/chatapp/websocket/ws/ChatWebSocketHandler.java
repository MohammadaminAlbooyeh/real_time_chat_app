package com.chatapp.websocket.ws;

import com.chatapp.websocket.manager.ConnectionManager;
import com.chatapp.websocket.message.MessageHandler;
import com.chatapp.websocket.presence.PresenceTracker;
import com.chatapp.websocket.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * Equivalent of routes.py's /ws endpoint: authenticates the connection via
 * a "token" query param, then dispatches inbound events to MessageHandler.
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private static final String ATTR_USER_ID = "userId";
    private static final String ATTR_USERNAME = "username";

    private final ConnectionManager connectionManager;
    private final PresenceTracker presenceTracker;
    private final MessageHandler messageHandler;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public ChatWebSocketHandler(ConnectionManager connectionManager,
                                 PresenceTracker presenceTracker,
                                 MessageHandler messageHandler,
                                 JwtService jwtService,
                                 ObjectMapper objectMapper) {
        this.connectionManager = connectionManager;
        this.presenceTracker = presenceTracker;
        this.messageHandler = messageHandler;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = extractToken(session);
        if (token == null || token.isBlank()) {
            session.close(new CloseStatus(4001, "Missing token"));
            return;
        }

        Claims claims = jwtService.verifyToken(token);
        if (claims == null) {
            session.close(new CloseStatus(4001, "Invalid or expired token"));
            return;
        }

        String userId = claims.getSubject();
        String username = claims.get("username", String.class);
        if (username == null) {
            username = "unknown";
        }

        session.getAttributes().put(ATTR_USER_ID, userId);
        session.getAttributes().put(ATTR_USERNAME, username);

        connectionManager.connect(userId, session);
        presenceTracker.setOnline(userId, username);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String userId = (String) session.getAttributes().get(ATTR_USER_ID);
        if (userId == null) {
            return;
        }
        String username = (String) session.getAttributes().get(ATTR_USERNAME);

        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String eventType = root.path("type").asText(null);
            JsonNode payload = root.path("payload");

            switch (eventType == null ? "" : eventType) {
                case "message:send" -> messageHandler.handleSendMessage(payload, userId, username);
                case "typing:start" -> messageHandler.handleTyping(payload, userId, username, true);
                case "typing:stop" -> messageHandler.handleTyping(payload, userId, username, false);
                case "room:join" -> messageHandler.handleJoinRoom(payload, userId, username);
                case "room:leave" -> messageHandler.handleLeaveRoom(payload, userId, username);
                default -> connectionManager.sendPersonal(userId, Map.of(
                        "type", "error",
                        "payload", Map.of("message", "Unknown event type: " + eventType)
                ));
            }
        } catch (Exception e) {
            log.warn("Error handling message from {}: {}", userId, e.getMessage());
            connectionManager.sendPersonal(userId, Map.of(
                    "type", "error",
                    "payload", Map.of("message", String.valueOf(e.getMessage()))
            ));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = (String) session.getAttributes().get(ATTR_USER_ID);
        if (userId == null) {
            return;
        }
        String username = (String) session.getAttributes().get(ATTR_USERNAME);
        connectionManager.disconnect(userId);
        presenceTracker.setOffline(userId, username == null ? "unknown" : username);
    }

    private String extractToken(WebSocketSession session) {
        if (session.getUri() == null) {
            return null;
        }
        List<String> values = UriComponentsBuilder.fromUri(session.getUri())
                .build()
                .getQueryParams()
                .get("token");
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }
}
