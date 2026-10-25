# WebSocket Protocol Reference

Implemented by **websocket-service** (Spring Boot, `spring-boot-starter-websocket`, plain `TextWebSocketHandler` — not STOMP/SockJS). Core classes: `ChatWebSocketHandler` (connection lifecycle + dispatch), `MessageHandler` (event logic), `ConnectionManager` (in-memory session/room registry), `PresenceTracker`, `RedisPublisher` / `RoomEventSubscriber` (cross-instance fanout).

## Connection

### Endpoint

```
ws://<host>:<websocket-service-port>/ws?token=<jwt_access_token>
```

**Production (via Nginx):**
```
wss://<host>/ws?token=<jwt_access_token>
```

### Authentication

The JWT access token is passed as the `token` query parameter and verified by this service's own `JwtService` (same signing secret as api-service). There is no header-based auth path for the socket handshake.

**Connection close codes** (`ChatWebSocketHandler.afterConnectionEstablished`):
| Code | Reason |
|------|--------|
| 4001 | `token` query parameter missing or blank |
| 4001 | Token failed JWT verification (invalid/expired) |
| 1000 | Normal client-initiated close |
| 1006 | Abnormal disconnect (network/server error) |

On a successful handshake the server extracts `userId` (JWT subject) and `username` (JWT claim, falls back to `"unknown"` if absent), registers the session in `ConnectionManager`, and immediately marks the user **online** via `PresenceTracker` — there is no separate "hello"/ack message.

### Reconnection

Not implemented server-side; this is purely client-side guidance:
1. On an unexpected disconnect, reconnect with exponential backoff (e.g. 1s, 2s, 4s... capped around 30s).
2. On a 4001 close, refresh the JWT before reconnecting — retrying with the same token will just be rejected again.
3. After reconnecting, the client must re-send `room:join` for every room it cares about — the server does not remember previous room subscriptions across a new connection.

---

## Message Envelope

Every inbound and outbound frame is a single JSON text frame shaped as:
```json
{ "type": "<event-name>", "payload": { ... } }
```

## Client → Server Events

Dispatched in `ChatWebSocketHandler.handleTextMessage` by a switch on `type`. Any other/unknown `type` gets back an `error` event (see below) rather than being silently ignored.

### message:send

**Payload:**
```json
{ "room_id": "uuid", "content": "Hello, world!", "message_type": "text", "reply_to_id": null }
```
| Field | Required | Notes |
|-------|----------|-------|
| `room_id` | Yes | Target room |
| `content` | Yes | Message text |
| `message_type` | No | Defaults to `"text"` if omitted |
| `reply_to_id` | No | Message being replied to |

Handling (`MessageHandler.handleSendMessage`): the message is inserted directly into PostgreSQL by websocket-service itself (via its own `MessageRepository`, a separate write path from api-service's JPA layer — **not** a call to the REST API), then broadcast as `message:new` to the room's local subscribers (excluding the sender) and published to Redis channel `room:{room_id}` so other websocket-service instances relay it to their own local subscribers.

### typing:start / typing:stop

**Payload:** `{ "room_id": "uuid" }`

Broadcasts `typing:indicator` to the room (excluding the sender) with `is_typing` true/false respectively. This is in-memory only — not persisted, not published to Redis.

### room:join

**Payload:** `{ "room_id": "uuid" }`

Adds the user to `ConnectionManager`'s in-memory room membership and to the Redis set `room:{room_id}:members`, then broadcasts `room:joined` to the room (excluding the joining user). **The client must send this after connecting** — opening the socket does not subscribe it to any room, so it will not receive `message:new`/`typing:indicator` for a room it hasn't explicitly joined.

### room:leave

**Payload:** `{ "room_id": "uuid" }`

Removes the user from the in-memory room membership and the Redis members set, then broadcasts `room:left` to the room (excluding the leaving user).

---

## Server → Client Events

### message:new
Broadcast to all other sessions currently joined to the room (sender excluded).
```json
{
  "id": "generated-id", "room_id": "uuid", "sender_id": "uuid", "sender_username": "johndoe",
  "content": "Hello, world!", "message_type": "text", "reply_to_id": null,
  "created_at": "2026-06-04T12:00:00", "reactions": [], "attachments": []
}
```
`reactions` and `attachments` are always empty arrays on this event — reactions/attachments added afterward via the REST API are not merged back into a live socket push (see below).

### typing:indicator
```json
{ "room_id": "uuid", "user_id": "uuid", "username": "johndoe", "is_typing": true }
```

### room:joined / room:left
```json
{ "user_id": "uuid", "username": "johndoe", "room_id": "uuid" }
```

### presence:online / presence:offline
Fired automatically on connect/disconnect (not client-triggered), published to the Redis `presence` channel and broadcast to all sessions subscribed to a reserved internal room (`__global__`):
```json
{ "user_id": "uuid", "username": "johndoe", "status": "online" }
```

### error
Sent back to the originating connection only (not broadcast) when an unknown event `type` is received or an exception occurs while handling one:
```json
{ "message": "Unknown event type: bogus_event" }
```

---

## What is NOT pushed over the socket today

REST-driven mutations in api-service — editing a message (`PUT /rooms/{id}/messages/{msgId}`), deleting a message (`DELETE .../messages/{msgId}`), and adding/removing a reaction (`POST`/`DELETE .../reactions`) — do **not** currently trigger any WebSocket broadcast. There is no `message:updated`, `message:deleted`, or `message:reaction` event in the implementation. Clients must re-fetch via REST (e.g. `GET /rooms/{roomId}/messages`) to see those changes reflected; this is a real gap, not a documentation omission — worth flagging as roadmap/follow-up work if live updates for edits/deletes/reactions are wanted.

---

## Event Summary

### Client → Server
| Event | Payload | Description |
|-------|---------|--------------|
| `message:send` | `{room_id, content, message_type?, reply_to_id?}` | Persist + broadcast a new message |
| `typing:start` / `typing:stop` | `{room_id}` | Toggle typing indicator |
| `room:join` | `{room_id}` | Subscribe this connection to a room |
| `room:leave` | `{room_id}` | Unsubscribe from a room |

### Server → Client
| Event | Payload | Trigger |
|-------|---------|---------|
| `message:new` | message object (reactions/attachments always `[]`) | `message:send` |
| `typing:indicator` | `{room_id, user_id, username, is_typing}` | `typing:start`/`typing:stop` |
| `room:joined` | `{user_id, username, room_id}` | `room:join` |
| `room:left` | `{user_id, username, room_id}` | `room:leave` or disconnect cleanup |
| `presence:online` / `presence:offline` | `{user_id, username, status}` | connect / disconnect |
| `error` | `{message}` | unknown event type or handler exception |

---

## Implementation Example (JavaScript)

```javascript
class ChatWebSocket {
  constructor(baseUrl, getToken) {
    this.baseUrl = baseUrl;
    this.getToken = getToken;
    this.listeners = {};
    this.reconnectAttempts = 0;
  }

  connect() {
    const token = this.getToken();
    this.ws = new WebSocket(`${this.baseUrl}/ws?token=${token}`);

    this.ws.onopen = () => { this.reconnectAttempts = 0; };

    this.ws.onmessage = (event) => {
      const { type, payload } = JSON.parse(event.data);
      (this.listeners[type] || []).forEach((handler) => handler(payload));
    };

    this.ws.onclose = (event) => {
      if (event.code === 4001) {
        // token missing/invalid — refresh it before reconnecting
        return;
      }
      const delay = Math.min(1000 * 2 ** this.reconnectAttempts, 30000);
      setTimeout(() => this.connect(), delay);
      this.reconnectAttempts++;
    };
  }

  send(type, payload) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({ type, payload }));
    }
  }

  on(type, handler) {
    (this.listeners[type] ??= []).push(handler);
  }

  joinRoom(roomId) { this.send('room:join', { room_id: roomId }); }
  leaveRoom(roomId) { this.send('room:leave', { room_id: roomId }); }
  sendTyping(roomId, isTyping) {
    this.send(isTyping ? 'typing:start' : 'typing:stop', { room_id: roomId });
  }
}
```

---

## Architecture Notes

### Message flow
```
Client --message:send--> websocket-service --> PostgreSQL (direct write via its own MessageRepository)
                                              --> Redis channel room:{room_id} (publish)
                                              --> in-memory broadcast to local room subscribers (sender excluded)

Other websocket-service instance <--PSUBSCRIBE room:*-- Redis
   --> RoomEventSubscriber --> in-memory broadcast to its own local room subscribers
```
Note: api-service does **not** publish to this Redis channel for message sends — messages created via the REST `POST /rooms/{id}/messages` endpoint are stored by api-service but are not relayed to WebSocket clients. Only messages sent through the WebSocket `message:send` event reach other clients in real time.

### Presence flow
```
Client connects    --> PresenceTracker.setOnline  --> Redis key presence:{userId}=online, publish on "presence" channel, broadcast presence:online to __global__ room
Client disconnects --> PresenceTracker.setOffline --> same, with status=offline
```

### Redis channels
| Channel / pattern | Purpose |
|---|---|
| `room:{room_id}` (subscribed via `room:*` pattern) | Room-scoped events published by websocket-service for cross-instance fanout |
| `presence` | Global online/offline events |
| `room:{room_id}:members` (Set, not pub/sub) | Tracks which users have joined a room, used by `room:join`/`room:leave` |
