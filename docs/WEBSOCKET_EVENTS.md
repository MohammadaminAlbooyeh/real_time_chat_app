# WebSocket Protocol Reference

## Connection

### Endpoint

```
ws://<host>:8001/ws?token=<jwt_access_token>
```

**Production (via Nginx):**
```
wss://<host>/ws?token=<jwt_access_token>
```

### Authentication

The access token must be passed as a query parameter. Only `access` type tokens are accepted (not `refresh` tokens).

**Connection states:**
| Code | Reason | Description |
|------|--------|-------------|
| 1000 | Normal | Clean disconnect |
| 4001 | Missing token | No `token` query parameter |
| 4001 | Invalid/expired token | JWT validation failed |
| 1006 | Abnormal | Unexpected disconnect (network/server error) |

### Reconnection

Clients should implement exponential backoff reconnection:

1. On disconnect (non-4001), wait 1s and retry
2. Double wait time on each failure (1s, 2s, 4s, 8s... max 30s)
3. On 4001 disconnect, refresh the JWT token, then reconnect
4. After reconnecting, re-join rooms and restore state

---

## Client → Server Events

### message:send

Send a message to a room via WebSocket.

**Payload:**
```json
{
  "room_id": "uuid",
  "content": "Hello, world!",
  "message_type": "text",
  "reply_to_id": null
}
```

**Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `room_id` | string (UUID) | Yes | Target room ID |
| `content` | string | Yes | Message text (1-5000 chars) |
| `message_type` | string | No | `text`, `image`, `file`, `system` (default: `text`) |
| `reply_to_id` | string (UUID) | No | Message ID being replied to |

**Server response:** The message is broadcast to all room members (including via `message:new` event). The sender does NOT receive their own broadcast.

**Note:** This writes directly to PostgreSQL. The REST API's `POST /rooms/{id}/messages` endpoint is the alternative method.

### typing:start / typing:stop

Notify room members that the user is typing.

**Payload:**
```json
{
  "room_id": "uuid"
}
```

**Server response:** Broadcasts `typing:indicator` event with `is_typing: true` or `false` to all other room members.

**Implementation guidance:**
- Send `typing:start` when user starts typing
- Send `typing:stop` after 3 seconds of inactivity or when message is sent
- Client should debounce typing events (send at most once per 2 seconds)

### room:join

Subscribe to events in a specific room.

**Payload:**
```json
{
  "room_id": "uuid"
}
```

**Server response:**
- Adds user to the room's subscriber list in `ConnectionManager`
- Adds user to Redis room member set
- Broadcasts `room:joined` event to other room members

**Important:** The client MUST send `room:join` after connecting to receive room events. Simply opening a WebSocket does not subscribe to any room.

### room:leave

Unsubscribe from a room.

**Payload:**
```json
{
  "room_id": "uuid"
}
```

**Server response:**
- Removes user from the room's subscriber list
- Removes user from Redis room member set
- Broadcasts `room:left` event to remaining room members

---

## Server → Client Events

### message:new

A new message was sent to a room the user is subscribed to.

**Payload:**
```json
{
  "id": "uuid",
  "room_id": "uuid",
  "sender_id": "uuid",
  "sender_username": "johndoe",
  "content": "Hello, world!",
  "message_type": "text",
  "reply_to_id": null,
  "created_at": "2025-06-04T12:00:00Z",
  "reactions": [],
  "attachments": []
}
```

**Triggered by:** WebSocket `message:send` event, REST `POST /rooms/{id}/messages`.

### message:updated

A message was edited.

**Payload:**
```json
{
  "id": "uuid",
  "room_id": "uuid",
  "sender_id": "uuid",
  "sender_username": "johndoe",
  "content": "Updated content",
  "message_type": "text",
  "reply_to_id": null,
  "is_deleted": false,
  "edited_at": "2025-06-04T12:30:00Z",
  "created_at": "2025-06-04T12:00:00Z",
  "reactions": [],
  "attachments": []
}
```

**Triggered by:** REST `PUT /rooms/{id}/messages/{msgId}`.

### message:deleted

A message was soft-deleted.

**Payload:**
```json
{
  "room_id": "uuid",
  "id": "uuid"
}
```

**Triggered by:** REST `DELETE /rooms/{id}/messages/{msgId}`.

### message:reaction

A reaction was added or removed from a message.

**Payload (added):**
```json
{
  "id": "uuid",
  "room_id": "uuid",
  "message_id": "uuid",
  "user_id": "uuid",
  "username": "johndoe",
  "emoji": "👍",
  "created_at": "2025-06-04T12:00:00Z"
}
```

**Payload (removed):**
```json
{
  "room_id": "uuid",
  "message_id": "uuid",
  "reaction_id": "uuid",
  "user_id": "uuid"
}
```

**Triggered by:** REST `POST/DELETE /rooms/{id}/messages/{msgId}/reactions`.

### typing:indicator

A user started or stopped typing in a room.

**Payload:**
```json
{
  "room_id": "uuid",
  "user_id": "uuid",
  "username": "johndoe",
  "is_typing": true
}
```

**Usage:** Display "User is typing..." indicator when `is_typing: true`. Hide after receiving `is_typing: false` or after a 5-second timeout.

### room:joined

A user joined a room subscription.

**Payload:**
```json
{
  "user_id": "uuid",
  "username": "johndoe",
  "room_id": "uuid"
}
```

**Triggered by:** WebSocket `room:join` event.

### room:left

A user left a room subscription.

**Payload:**
```json
{
  "user_id": "uuid",
  "username": "johndoe",
  "room_id": "uuid"
}
```

**Triggered by:** WebSocket `room:leave` event or user disconnection.

### presence:online

A user came online.

**Payload:**
```json
{
  "user_id": "uuid",
  "username": "johndoe",
  "status": "online"
}
```

**Triggered by:** WebSocket connection established.

### presence:offline

A user went offline.

**Payload:**
```json
{
  "user_id": "uuid",
  "username": "johndoe",
  "status": "offline"
}
```

**Triggered by:** WebSocket disconnection.

### error

An error occurred processing a client event.

**Payload:**
```json
{
  "message": "Unknown event type: invalid_event"
}
```

---

## Event Summary

### Client → Server

| Event | Payload | Description |
|-------|---------|-------------|
| `message:send` | `{room_id, content, message_type?, reply_to_id?}` | Send a message |
| `typing:start` | `{room_id}` | Indicate typing started |
| `typing:stop` | `{room_id}` | Indicate typing stopped |
| `room:join` | `{room_id}` | Subscribe to room events |
| `room:leave` | `{room_id}` | Unsubscribe from room |

### Server → Client

| Event | Payload | Description |
|-------|---------|-------------|
| `message:new` | Full message object | New message in room |
| `message:updated` | Full message object | Message edited |
| `message:deleted` | `{room_id, id}` | Message deleted |
| `message:reaction` | Reaction details | Reaction added/removed |
| `typing:indicator` | `{room_id, user_id, username, is_typing}` | Typing status |
| `room:joined` | `{user_id, username, room_id}` | User joined room |
| `room:left` | `{user_id, username, room_id}` | User left room |
| `presence:online` | `{user_id, username, status}` | User came online |
| `presence:offline` | `{user_id, username, status}` | User went offline |
| `error` | `{message}` | Error processing event |

---

## Implementation Examples

### WebSocket Connection (JavaScript)

```javascript
class ChatWebSocket {
    constructor(baseUrl, getToken) {
        this.baseUrl = baseUrl;
        this.getToken = getToken;
        this.ws = null;
        this.listeners = {};
        this.reconnectAttempts = 0;
        this.maxReconnectDelay = 30000;
    }

    connect() {
        const token = this.getToken();
        this.ws = new WebSocket(`${this.baseUrl}/ws?token=${token}`);

        this.ws.onopen = () => {
            console.log('WebSocket connected');
            this.reconnectAttempts = 0;
        };

        this.ws.onmessage = (event) => {
            const data = JSON.parse(event.data);
            const handlers = this.listeners[data.type] || [];
            handlers.forEach(handler => handler(data.payload));
        };

        this.ws.onclose = (event) => {
            if (event.code === 4001) {
                // Token expired, need to re-authenticate
                console.log('Token invalid, refreshing...');
                // Token refresh logic
            } else {
                // Reconnect with exponential backoff
                const delay = Math.min(
                    1000 * Math.pow(2, this.reconnectAttempts),
                    this.maxReconnectDelay
                );
                setTimeout(() => this.connect(), delay);
                this.reconnectAttempts++;
            }
        };
    }

    send(type, payload) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify({ type, payload }));
        }
    }

    on(eventType, handler) {
        if (!this.listeners[eventType]) {
            this.listeners[eventType] = [];
        }
        this.listeners[eventType].push(handler);
    }

    joinRoom(roomId) {
        this.send('room:join', { room_id: roomId });
    }

    leaveRoom(roomId) {
        this.send('room:leave', { room_id: roomId });
    }

    sendTyping(roomId, isTyping) {
        this.send(isTyping ? 'typing:start' : 'typing:stop', { room_id: roomId });
    }

    disconnect() {
        if (this.ws) {
            this.ws.close(1000, 'Client disconnect');
        }
    }
}
```

### Usage

```javascript
const ws = new ChatWebSocket(
    'wss://chat.example.com',
    () => localStorage.getItem('access_token')
);

// Listen for events
ws.on('message:new', (msg) => {
    displayMessage(msg);
});

ws.on('typing:indicator', (data) => {
    updateTypingIndicator(data.username, data.is_typing);
});

ws.on('presence:online', (data) => {
    setUserOnline(data.user_id);
});

ws.on('presence:offline', (data) => {
    setUserOffline(data.user_id);
});

// Connect and join rooms
ws.connect();
ws.joinRoom('room-uuid-here');
```

---

## Architecture Notes

### Message Flow

```
Client → WebSocket Service → PostgreSQL (direct write)
                            → Redis pub/sub (event)
                            → In-memory broadcast (exclude sender)

API Service → PostgreSQL (write)
            → Redis pub/sub (event)
            → WebSocket Service (receives via Redis subscription)
            → In-memory broadcast to room
```

### Presence Flow

```
Client connects → WebSocket Service → Mark online in Redis
                                     → Broadcast presence:online
                                     
Client disconnects → WebSocket Service → Mark offline in Redis
                                       → Broadcast presence:offline
```

### Redis Channels

| Channel Pattern | Purpose |
|----------------|---------|
| `room:{room_id}` | Room-scoped events (message updates, deletion, reactions) |
| `presence` | Global presence events (online/offline) |

The WebSocket service subscribes to `room:*` using Redis `PSUBSCRIBE` to receive events published by the API service for message mutations.
