package com.chatapp.websocket.redis;

import com.chatapp.websocket.manager.ConnectionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomEventSubscriberTest {

    @Test
    void forwardsEventToRoomFromPayload() {
        ConnectionManager connectionManager = mock(ConnectionManager.class);
        RoomEventSubscriber subscriber = new RoomEventSubscriber(connectionManager, new ObjectMapper());

        String json = "{\"type\":\"message:updated\",\"payload\":{\"room_id\":\"room-1\",\"id\":\"msg-1\"}}";
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn(json.getBytes(StandardCharsets.UTF_8));

        subscriber.onMessage(message, "room:room-1".getBytes(StandardCharsets.UTF_8));

        verify(connectionManager).broadcastToRoom(eq("room-1"), any(), eq(null));
    }

    @Test
    void ignoresEventWithoutRoomId() {
        ConnectionManager connectionManager = mock(ConnectionManager.class);
        RoomEventSubscriber subscriber = new RoomEventSubscriber(connectionManager, new ObjectMapper());

        String json = "{\"type\":\"presence:online\",\"payload\":{\"user_id\":\"user-1\"}}";
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn(json.getBytes(StandardCharsets.UTF_8));

        subscriber.onMessage(message, "presence".getBytes(StandardCharsets.UTF_8));

        verify(connectionManager, never()).broadcastToRoom(any(), any(), any());
    }
}
