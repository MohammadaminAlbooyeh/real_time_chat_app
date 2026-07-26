package com.chatapp.websocket.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConnectionManagerTest {

    private ConnectionManager manager;

    @BeforeEach
    void setUp() {
        manager = new ConnectionManager(new ObjectMapper());
    }

    private WebSocketSession mockSession() {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    @Test
    void connectAndDisconnect() {
        manager.connect("user-1", mockSession());
        assertThat(manager.isOnline("user-1")).isTrue();

        manager.disconnect("user-1");
        assertThat(manager.isOnline("user-1")).isFalse();
    }

    @Test
    void joinAndLeaveRoomRemovesEmptyRoom() throws Exception {
        manager.joinRoom("user-1", "room-1");
        manager.leaveRoom("user-1", "room-1");

        WebSocketSession session = mockSession();
        manager.connect("user-1", session);
        manager.broadcastToRoom("room-1", Map.of("type", "test"), null);

        verify(session, never()).sendMessage(any());
    }

    @Test
    void broadcastToRoomExcludesSender() throws Exception {
        WebSocketSession session1 = mockSession();
        WebSocketSession session2 = mockSession();
        manager.connect("user-1", session1);
        manager.connect("user-2", session2);
        manager.joinRoom("user-1", "room-1");
        manager.joinRoom("user-2", "room-1");

        manager.broadcastToRoom("room-1", Map.of("type", "message:new"), "user-1");

        verify(session1, never()).sendMessage(any());
        verify(session2, times(1)).sendMessage(any());
    }

    @Test
    void sendPersonalDeliversMessage() throws Exception {
        WebSocketSession session = mockSession();
        manager.connect("user-1", session);

        manager.sendPersonal("user-1", Map.of("type", "test"));

        verify(session, times(1)).sendMessage(any());
    }
}
