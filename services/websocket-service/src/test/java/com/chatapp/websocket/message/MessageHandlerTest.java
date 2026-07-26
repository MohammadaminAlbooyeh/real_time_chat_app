package com.chatapp.websocket.message;

import com.chatapp.websocket.manager.ConnectionManager;
import com.chatapp.websocket.redis.RedisPublisher;
import com.chatapp.websocket.repository.MessageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageHandlerTest {

    private ConnectionManager connectionManager;
    private RedisPublisher redisPublisher;
    private StringRedisTemplate redisTemplate;
    private MessageRepository messageRepository;
    private MessageHandler handler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        connectionManager = mock(ConnectionManager.class);
        redisPublisher = mock(RedisPublisher.class);
        redisTemplate = mock(StringRedisTemplate.class);
        messageRepository = mock(MessageRepository.class);
        handler = new MessageHandler(connectionManager, redisPublisher, redisTemplate, messageRepository);
    }

    private JsonNode payload(String json) throws Exception {
        return objectMapper.readTree(json);
    }

    @Test
    void handleSendMessagePersistsBroadcastsAndPublishes() throws Exception {
        when(messageRepository.insertMessage("room-1", "user-1", "Hello!", "text", null))
                .thenReturn(new MessageRepository.InsertedMessage("msg-1", "2024-01-01T00:00:00Z"));

        handler.handleSendMessage(payload("{\"room_id\":\"room-1\",\"content\":\"Hello!\"}"), "user-1", "alice");

        verify(messageRepository).insertMessage("room-1", "user-1", "Hello!", "text", null);
        verify(connectionManager).broadcastToRoom(eq("room-1"), any(), eq("user-1"));
        verify(redisPublisher).publish(eq("room:room-1"), any());
    }

    @Test
    void handleTypingBroadcastsExcludingSender() throws Exception {
        handler.handleTyping(payload("{\"room_id\":\"room-1\"}"), "user-1", "alice", true);

        verify(connectionManager).broadcastToRoom(eq("room-1"), any(), eq("user-1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleJoinRoomAddsToRedisSetAndBroadcasts() throws Exception {
        SetOperations<String, String> setOps = mock(SetOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(setOps);

        handler.handleJoinRoom(payload("{\"room_id\":\"room-1\"}"), "user-1", "alice");

        verify(connectionManager).joinRoom("user-1", "room-1");
        verify(setOps).add("room:room-1:members", "user-1");
        verify(connectionManager).broadcastToRoom(eq("room-1"), any(), eq("user-1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleLeaveRoomRemovesFromRedisSetAndBroadcasts() throws Exception {
        SetOperations<String, String> setOps = mock(SetOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(setOps);

        handler.handleLeaveRoom(payload("{\"room_id\":\"room-1\"}"), "user-1", "alice");

        verify(connectionManager).leaveRoom("user-1", "room-1");
        verify(setOps).remove("room:room-1:members", "user-1");
        verify(connectionManager).broadcastToRoom(eq("room-1"), any(), eq("user-1"));
    }
}
