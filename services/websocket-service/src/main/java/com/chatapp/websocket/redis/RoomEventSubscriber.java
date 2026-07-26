package com.chatapp.websocket.redis;

import com.chatapp.websocket.manager.ConnectionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Equivalent of main.py's redis_event_forwarder: subscribes to the "room:*"
 * pattern and rebroadcasts each event to the local sessions in that room,
 * so that events published by other service instances still reach clients
 * connected to this one.
 */
@Component
public class RoomEventSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RoomEventSubscriber.class);

    private final ConnectionManager connectionManager;
    private final ObjectMapper objectMapper;

    public RoomEventSubscriber(ConnectionManager connectionManager, ObjectMapper objectMapper) {
        this.connectionManager = connectionManager;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            JsonNode event = objectMapper.readTree(message.getBody());
            String roomId = event.path("payload").path("room_id").asText(null);
            if (roomId != null) {
                connectionManager.broadcastToRoom(roomId, objectMapper.treeToValue(event, Object.class), null);
            }
        } catch (Exception e) {
            log.warn("Failed to process room event: {}", e.getMessage());
        }
    }
}
