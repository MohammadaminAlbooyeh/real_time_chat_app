package com.chatapp.websocket.presence;

import com.chatapp.websocket.manager.ConnectionManager;
import com.chatapp.websocket.redis.RedisPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PresenceTrackerTest {

    private ConnectionManager connectionManager;
    private RedisPublisher redisPublisher;
    private StringRedisTemplate redisTemplate;
    private PresenceTracker presenceTracker;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        connectionManager = mock(ConnectionManager.class);
        redisPublisher = mock(RedisPublisher.class);
        redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        presenceTracker = new PresenceTracker(connectionManager, redisPublisher, redisTemplate);
    }

    @Test
    void setOnlinePublishesAndBroadcastsToGlobalRoom() {
        presenceTracker.setOnline("user-1", "alice");

        verify(redisTemplate.opsForValue()).set("presence:user-1", "online");
        verify(redisPublisher).publish(eq("presence"), any());
        verify(connectionManager).broadcastToRoom(eq("__global__"), any(), eq(null));
    }

    @Test
    void setOfflinePublishesAndBroadcastsToGlobalRoom() {
        presenceTracker.setOffline("user-1", "alice");

        verify(redisTemplate.opsForValue()).set("presence:user-1", "offline");
        verify(redisPublisher).publish(eq("presence"), any());
        verify(connectionManager).broadcastToRoom(eq("__global__"), any(), eq(null));
    }
}
