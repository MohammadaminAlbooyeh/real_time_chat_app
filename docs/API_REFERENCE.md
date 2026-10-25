# API Reference

Base URL: `https://yourdomain.com/api/v1` (production, via Nginx) / `http://localhost:8000/api/v1` (api-service default local port).

The REST API is served by **api-service** (Spring Boot 3.5, Java 21). JSON field names are **camelCase** (plain Jackson defaults — there is no snake_case naming strategy configured), which differs from the old Python/FastAPI API this replaced.

A separate internal microservice, **search-service**, exposes its own `/api/v1/search` API backed by Elasticsearch; it is documented at the end of this file.

## Authentication

Protected endpoints require a Bearer JWT, issued/verified by api-service's `JwtService` (`jjwt` library) and checked by `JwtAuthFilter` (Spring Security, stateless sessions, CSRF disabled):

```
Authorization: Bearer <access_token>
```

Public (no-auth) paths, per `SecurityConfig`:
- `/health`, `/health/**`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`

Every other endpoint requires a valid access token. Default expiry (see `application.yml`): access token 30 minutes, refresh token 7 days (`JWT_ACCESS_TOKEN_EXPIRE_MINUTES`, `JWT_REFRESH_TOKEN_EXPIRE_DAYS`).

---

## 1. Auth (`AuthController`)

### POST /auth/register

**Request body** (`UserCreate`):
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "securePassword123!",
  "displayName": "John Doe"
}
```

**Validation:** `username` 3-50 chars; `email` valid format; `password` 8-128 chars; `displayName` optional, max 100 chars.

**Response** `201 Created` (`TokenResponse`):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "bearer",
  "user": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "username": "johndoe",
    "email": "john@example.com",
    "displayName": "John Doe",
    "avatarUrl": null,
    "bio": null,
    "isOnline": false,
    "lastSeen": "2026-06-04T12:00:00",
    "createdAt": "2026-06-04T12:00:00"
  }
}
```

### POST /auth/login

**Request body** (`UserLogin`): `{"email": "...", "password": "..."}`

**Response** `200 OK`: same `TokenResponse` shape as register.

### POST /auth/refresh

**Request body** (`RefreshTokenRequest`): `{"refreshToken": "..."}`

**Response** `200 OK`: new `TokenResponse`.

### POST /auth/logout

Authenticated. No request body.

**Response** `200 OK`: `{"message": "Logged out successfully"}`

Note: this is a stateless, client-side logout — the server does not maintain a token blocklist/session store, so it simply confirms the call; the client is responsible for discarding its tokens.

---

## 2. Users (`UserController`, base `/api/v1/users`)

### GET /users/me
Returns the authenticated user's `UserResponse` (same shape as the `user` object above).

### PATCH /users/me

**Request body** (`UserProfileUpdate`, all optional):
```json
{ "displayName": "New Name", "avatarUrl": "https://cdn.example.com/a.jpg", "bio": "..." }
```
`displayName` max 100 chars, `avatarUrl`/`bio` max 500 chars.

**Response** `200 OK`: updated `UserResponse`.

### POST /users/me/change-password

**Request body** (`PasswordChange`): `{"currentPassword": "...", "newPassword": "..."}` (`newPassword` 8-128 chars).

**Response** `200 OK`: `{"message": "Password changed successfully"}`

### GET /users/search?q={query}

**Response** `200 OK` (`UserSearchResult[]`):
```json
[{ "id": "...", "username": "johndoe", "displayName": "John Doe", "avatarUrl": null, "isOnline": true }]
```

### GET /users/{userId}
Returns a `UserResponse` for any user (public profile fields; no membership check in current code).

---

## 3. Rooms (`RoomController`, base `/api/v1/rooms`)

### POST /rooms

**Request body** (`RoomCreate`): `{"name": "Project Alpha", "description": "...", "isPrivate": false}` (`name` required, max 100 chars; `description` optional, max 500 chars).

**Response** `201 Created` (`RoomResponse`):
```json
{
  "id": "...", "name": "Project Alpha", "description": "...", "isPrivate": false,
  "createdById": "...", "createdAt": "...", "updatedAt": "...",
  "memberCount": 1, "lastMessage": null, "lastMessageAt": null
}
```

### GET /rooms?limit=50&offset=0
Lists rooms the user is a member of. `limit`/`offset` optional, default 50/0. Returns `RoomResponse[]`.

### GET /rooms/{roomId}

**Response** `200 OK` (`RoomDetailResponse`): same fields as `RoomResponse` plus a `members: RoomMemberResponse[]` array:
```json
{ "id": "roomId", "userId": "...", "username": "...", "displayName": "...", "avatarUrl": null, "role": "admin", "joinedAt": "..." }
```

### PUT /rooms/{roomId}

**Request body** (`RoomUpdate`, all optional): `{"name": "...", "description": "...", "isPrivate": true}`.

**Response** `200 OK`: updated `RoomResponse`.

### DELETE /rooms/{roomId}
`204 No Content`.

### POST /rooms/{roomId}/members

**Request body** (`AddMemberRequest`): `{"userId": "..."}`.

**Response** `201 Created`: `{"message": "Member added successfully"}`

### DELETE /rooms/{roomId}/members/{userId}
**Response** `200 OK`: `{"message": "Member removed successfully"}`

### POST /rooms/{roomId}/leave
**Response** `200 OK`: `{"message": "Left room successfully"}`

---

## 4. Messages (`MessageController`, base `/api/v1/rooms/{roomId}/messages`)

### GET /rooms/{roomId}/messages?limit=50&offset=0&before={ISO LocalDateTime}

**Response** `200 OK` (`PaginatedResponse<MessageResponse>`):
```json
{
  "items": [
    {
      "id": "...", "roomId": "...", "senderId": "...", "senderUsername": "johndoe", "senderAvatar": null,
      "content": "Hello everyone!", "messageType": "text", "replyToId": null, "replyPreview": null,
      "isDeleted": false, "editedAt": null, "createdAt": "...",
      "reactions": [{ "id": "...", "userId": "...", "username": "janedoe", "emoji": "👍", "createdAt": "..." }],
      "attachments": [{ "id": "...", "fileName": "presentation.pdf", "fileUrl": "/uploads/2026/06/04/uuid.pdf", "fileType": "other", "fileSize": 2048000, "createdAt": "..." }]
    }
  ],
  "total": 150,
  "limit": 50,
  "offset": 0,
  "hasMore": true
}
```

### POST /rooms/{roomId}/messages

**Request body** (`MessageSend`): `{"content": "Hello, world!", "messageType": "text", "replyToId": null}`. `content` required, 1-5000 chars; `messageType` defaults to `"text"` if omitted.

**Response** `201 Created`: full `MessageResponse`.

### PUT /rooms/{roomId}/messages/{messageId}

**Request body** (`MessageEdit`): `{"content": "Updated message content"}` (1-5000 chars).

**Response** `200 OK`: updated `MessageResponse` with `editedAt` set.

### DELETE /rooms/{roomId}/messages/{messageId}
`204 No Content`.

### POST /rooms/{roomId}/messages/{messageId}/reactions

**Request body** (`MessageReactionAdd`): `{"emoji": "🎉"}` (max 50 chars).

**Response** `201 Created` (`ReactionResponse`):
```json
{ "id": "...", "userId": "...", "username": "johndoe", "emoji": "🎉", "createdAt": "..." }
```

### DELETE /rooms/{roomId}/messages/{messageId}/reactions/{reactionId}
`204 No Content`.

> Note: creating/editing/deleting a message or reaction over REST does **not** push a live event to connected WebSocket clients today — see `docs/WEBSOCKET_EVENTS.md`. Only messages sent through the WebSocket's `message:send` event are broadcast in real time.

---

## 5. Relationships / Friends (`RelationshipController`, base `/api/v1/relationships`)

### POST /relationships/request/{userId}
**Response** `201 Created`: `{"message": "Friend request sent"}`
Errors: `400` if targeting yourself, `404` if user not found, `409` if a relationship already exists either direction.

### POST /relationships/respond/{relationshipId}?action=accept|reject
**Response** `200 OK`: `{"message": "Friend request accepted"}` or `{"message": "Friend request rejected"}`.
Errors: `403` if you're not the addressee, `400` if already responded to, `400` if `action` isn't `accept`/`reject`.

### GET /relationships/friends
**Response** `200 OK`: array of `{id, username, displayName, avatarUrl, isOnline}` for all `accepted` relationships.

### GET /relationships/pending
**Response** `200 OK`: array of `{id, user: {id, username, displayName, avatarUrl}, createdAt}` for pending requests addressed to you.

### DELETE /relationships/{relationshipId}
Removes a friendship or cancels/rejects a request you're party to. **Response** `200 OK`: `{"message": "Friend removed"}`. `403` if you're neither party.

---

## 6. Search in api-service (`SearchController`, base `/api/v1/search`)

### GET /search/messages?q={query}&roomId={roomId}&limit=50

Delegates to `MessageService.searchMessages` (i.e. this endpoint queries the primary datastore via api-service itself; it does **not** proxy to the separate `search-service`/Elasticsearch). `roomId` optional to scope to one room.

**Response** `200 OK`: `MessageResponse[]` (same shape as section 4).

---

## 7. Upload (`UploadController`, `/api/v1/upload`)

### POST /upload?roomId={roomId}&messageId={messageId}

`multipart/form-data` with a `file` part. `roomId` (UUID) required — caller must be a member of that room (`403` otherwise). `messageId` (UUID) optional — if given, must reference an existing message in that room (`404` otherwise), and the created attachment is persisted against it.

**Allowed content types** (exact allow-list checked against `file.getContentType()`):
`image/jpeg`, `image/png`, `image/gif`, `image/webp`, `application/pdf`, `text/plain`, `application/zip`, `application/x-rar-compressed`, `application/x-7z-compressed`, `application/msword`, `application/vnd.openxmlformats-officedocument.wordprocessingml.document`. Anything else → `400`.

**Max size:** 50 MB (52,428,800 bytes checked in code; Spring's multipart limits in `application.yml` are `max-file-size: 50MB` / `max-request-size: 55MB`).

**Storage:** written to the local filesystem at `uploads/yyyy/MM/dd/<uuid>.<ext>` and served back statically at `/uploads/...` (see `WebConfig` resource handler) — there is no S3/CDN integration.

**Response** `201 Created`, without `messageId`:
```json
{ "id": "...", "fileName": "photo.jpg", "fileUrl": "/uploads/2026/06/04/uuid.jpg", "fileType": "image", "fileSize": 1024000, "mimeType": "image/jpeg" }
```
With `messageId`, the body additionally includes `"messageId": "..."` and the attachment is saved via `MessageAttachmentRepository`. `fileType` is only ever `"image"` or `"other"` in the current code (there is no per-document-type classification).

---

## 8. Notifications (`NotificationController`, base `/api/v1/notifications`)

### GET /notifications?limit=50&offset=0&unreadOnly=false

**Response** `200 OK` (`NotificationListResponse`):
```json
{
  "items": [{ "id": "...", "title": "...", "body": "...", "data": {"roomId": "...", "type": "new_message"}, "notificationType": "push", "isRead": false, "createdAt": "..." }],
  "total": 10,
  "unreadCount": 3
}
```

### GET /notifications/unread-count
**Response** `200 OK`: `{"unreadCount": 3}`

### PATCH /notifications/{notificationId}/read
**Response** `200 OK`: updated `NotificationResponse`. `404` if the notification doesn't belong to you (reported as "not found" rather than "forbidden").

### POST /notifications/read-all
**Response** `200 OK`: `{"message": "All notifications marked as read"}`

---

## 9. Health (`HealthController`)

### GET /health
`{"status": "ok", "service": "api-service"}` — always 200, unauthenticated.

### GET /health/db
`200 OK` either way: `{"status": "ok", "database": "connected"}` on success, or `{"status": "error", "database": "<exception message>"}` on failure (note: the endpoint itself returns HTTP 200 even when the DB check fails — the failure is only visible in the body).

---

## Error Response Shape

Handled centrally by `GlobalExceptionHandler` (`@RestControllerAdvice`). All error bodies share this shape (note: **not** the old `{"detail": "..."}` format):

```json
{ "status": 404, "error": "Not Found", "message": "Room not found", "path": "/api/v1/rooms/..." }
```

| Status | Source |
|--------|--------|
| 400 | Bean validation failures (`MethodArgumentNotValidException`), or explicit `ResponseStatusException` in a controller/service |
| 401 | `AuthenticationException` (missing/invalid JWT) |
| 403 | `AccessDeniedException` or an explicit `ResponseStatusException(FORBIDDEN, ...)` |
| 404 | `ResponseStatusException(NOT_FOUND, ...)` or Spring's `NoResourceFoundException` |
| 409 | `ResponseStatusException(CONFLICT, ...)` (e.g. duplicate relationship) |
| 429 | Rate limit exceeded (see below) — note this one has a *different* body shape |
| 500 | Any uncaught exception |

## Rate Limiting

`RateLimitingFilter` applies a single flat limit, **not** tiered by endpoint type: **120 requests/minute per client IP** (`app.rate-limit.requests-per-minute`, in-memory sliding window, not Redis-backed, so it is per-instance and resets if api-service restarts).

On exceeding the limit it returns `429` with:
```json
{ "status": 429, "error": "Too Many Requests", "message": "Rate limit exceeded" }
```
There are currently no `X-RateLimit-*` response headers.

---

## search-service API (separate microservice)

`search-service` is a standalone Spring Boot service backed by Elasticsearch; it is called internally (e.g. by worker-service to index messages) and is not exposed to browser clients through the main api-service. Base path `/api/v1/search` on its own port/container.

### POST /api/v1/search/index
Body (`IndexMessageRequest`): `{id, content, roomId, senderId, senderUsername, messageType, createdAt}`.
**Response** `201 Created`: `{"status": "indexed", "id": "<messageId>"}`

### GET /api/v1/search/messages?q=&roomId=&limit=50&offset=0
`limit` is capped server-side at 200. Returns a `SearchResult` object (Elasticsearch-backed).

### DELETE /api/v1/search/messages/{messageId}
**Response**: `{"status": "deleted", "id": "<messageId>"}`

### GET /api/v1/search/health
`{"status": "ok", "service": "search-service"}`
