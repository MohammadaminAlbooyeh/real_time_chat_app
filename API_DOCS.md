# API Documentation

Base URL: `https://yourdomain.com/api/v1` (production) or `http://localhost:8000/api/v1` (development)

## Authentication

Most endpoints require a Bearer token in the `Authorization` header:

```
Authorization: Bearer <access_token>
```

Tokens are JWT-based. Access tokens expire after 30 minutes. Refresh tokens expire after 7 days.

---

## Auth Endpoints

### POST /auth/register

Create a new user account.

**Request:**
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "securePassword123",
  "display_name": "John Doe"
}
```

**Response (201):**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIs...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIs...",
  "token_type": "bearer",
  "user": {
    "id": "uuid",
    "username": "johndoe",
    "email": "john@example.com",
    "display_name": "John Doe",
    "avatar_url": null,
    "bio": null,
    "is_online": false,
    "last_seen": "2025-01-01T00:00:00Z",
    "created_at": "2025-01-01T00:00:00Z"
  }
}
```

**Errors:** `409 Conflict` if email or username already exists.

### POST /auth/login

Authenticate with email and password.

**Request:**
```json
{
  "email": "john@example.com",
  "password": "securePassword123"
}
```

**Response (200):** Same structure as register response.

**Errors:** `401 Unauthorized` for invalid credentials, `403 Forbidden` if account is deactivated.

### POST /auth/refresh

Obtain a new access token using a refresh token.

**Request:**
```json
{
  "refresh_token": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response (200):** Returns new `access_token`, `refresh_token`, and `user` object.

**Errors:** `401 Unauthorized` if refresh token is invalid or expired.

### POST /auth/logout

Invalidate the current session (client-side; server returns confirmation).

**Headers:** Authorization: Bearer \<token\>

**Response (200):**
```json
{ "message": "Logged out successfully" }
```

---

## User Endpoints

### GET /users/me

Get the authenticated user's profile.

**Response (200):** Full `UserResponse` object.

### PATCH /users/me

Update the authenticated user's profile.

**Request:**
```json
{
  "display_name": "New Name",
  "avatar_url": "https://example.com/avatar.jpg",
  "bio": "Hello, I'm John!"
}
```

All fields are optional. Only provided fields are updated.

**Response (200):** Updated `UserResponse` object.

### POST /users/me/change-password

Change the authenticated user's password.

**Request:**
```json
{
  "current_password": "oldPassword123",
  "new_password": "newPassword456"
}
```

**Response (200):**
```json
{ "message": "Password changed successfully" }
```

**Errors:** `400 Bad Request` if current password is incorrect.

### GET /users/search?q={query}&limit={limit}

Search for users by username or display name.

**Query Parameters:**
- `q` (required): Search query, 1-50 characters
- `limit` (optional): Max results, default 20, max 200

**Response (200):**
```json
[
  {
    "id": "uuid",
    "username": "johndoe",
    "display_name": "John Doe",
    "avatar_url": null,
    "is_online": true
  }
]
```

The authenticated user is excluded from results.

### GET /users/{user_id}

Get a user's public profile by ID.

**Response (200):** Full `UserResponse` object.

**Errors:** `404 Not Found` if user does not exist.

---

## Room Endpoints

### POST /rooms

Create a new room. The creator is automatically added as an admin.

**Request:**
```json
{
  "name": "My Room",
  "description": "A room for chatting",
  "is_private": false
}
```

**Response (201):**
```json
{
  "id": "uuid",
  "name": "My Room",
  "description": "A room for chatting",
  "is_private": false,
  "created_by_id": "uuid",
  "created_at": "2025-01-01T00:00:00Z",
  "updated_at": "2025-01-01T00:00:00Z",
  "member_count": 1,
  "last_message": null,
  "last_message_at": null
}
```

**Errors:** `400 Bad Request` if the user has reached the maximum room limit (100).

### GET /rooms?limit={limit}&offset={offset}

List rooms the authenticated user is a member of.

**Response (200):** Array of `RoomResponse` objects, ordered by most recent activity.

### GET /rooms/{room_id}

Get room details including member list.

**Response (200):**
```json
{
  "id": "uuid",
  "name": "My Room",
  "description": "A room for chatting",
  "is_private": false,
  "created_by_id": "uuid",
  "created_at": "2025-01-01T00:00:00Z",
  "updated_at": "2025-01-01T00:00:00Z",
  "member_count": 3,
  "members": [
    {
      "id": "uuid",
      "user_id": "uuid",
      "username": "johndoe",
      "display_name": "John Doe",
      "avatar_url": null,
      "role": "admin",
      "joined_at": "2025-01-01T00:00:00Z"
    }
  ]
}
```

**Errors:** `403 Forbidden` if user is not a member.

### PUT /rooms/{room_id}

Update room settings (admin only).

**Request:**
```json
{
  "name": "Updated Room Name",
  "description": "Updated description",
  "is_private": true
}
```

All fields optional.

**Response (200):** Updated `RoomResponse`.

### DELETE /rooms/{room_id}

Delete a room (creator only).

**Response:** `204 No Content`

### POST /rooms/{room_id}/members

Add a member to a room (admin only).

**Request:**
```json
{
  "user_id": "uuid"
}
```

**Response (201):**
```json
{ "message": "Member added successfully" }
```

### DELETE /rooms/{room_id}/members/{user_id}

Remove a member from a room (admin only).

**Response (200):**
```json
{ "message": "Member removed successfully" }
```

### POST /rooms/{room_id}/leave

Leave a room.

**Response (200):**
```json
{ "message": "Left room successfully" }
```

---

## Message Endpoints

### GET /rooms/{room_id}/messages?limit={limit}&offset={offset}&before={iso_datetime}

Get messages in a room.

**Query Parameters:**
- `limit` (optional, default 50, max 200)
- `offset` (optional, default 0)
- `before` (optional): ISO 8601 datetime for cursor-based pagination

**Response (200):**
```json
{
  "items": [
    {
      "id": "uuid",
      "room_id": "uuid",
      "sender_id": "uuid",
      "sender_username": "johndoe",
      "sender_avatar": null,
      "content": "Hello!",
      "message_type": "text",
      "reply_to_id": null,
      "reply_preview": null,
      "is_deleted": false,
      "edited_at": null,
      "created_at": "2025-01-01T00:00:00Z",
      "reactions": [
        {
          "id": "uuid",
          "user_id": "uuid",
          "username": "janedoe",
          "emoji": "👍",
          "created_at": "2025-01-01T00:00:01Z"
        }
      ],
      "attachments": []
    }
  ],
  "total": 150,
  "limit": 50,
  "offset": 0,
  "has_more": true
}
```

### POST /rooms/{room_id}/messages

Send a message.

**Request:**
```json
{
  "room_id": "uuid",
  "content": "Hello, world!",
  "message_type": "text",
  "reply_to_id": null
}
```

`message_type` can be: `text`, `image`, `file`, `system`.

**Response (201):** `MessageResponse` object.

### PUT /rooms/{room_id}/messages/{message_id}

Edit a message (own messages only).

**Request:**
```json
{
  "content": "Updated content"
}
```

**Response (200):** Updated `MessageResponse`. Also publishes a `message:updated` event via Redis.

### DELETE /rooms/{room_id}/messages/{message_id}

Soft-delete a message (own messages only).

**Response:** `204 No Content`. Content is replaced with `[deleted]`. Also publishes a `message:deleted` event.

### POST /rooms/{room_id}/messages/{message_id}/reactions

Add a reaction to a message.

**Request:**
```json
{
  "emoji": "👍"
}
```

**Response (201):**
```json
{
  "id": "uuid",
  "user_id": "uuid",
  "username": "johndoe",
  "emoji": "👍",
  "created_at": "2025-01-01T00:00:00Z"
}
```

### DELETE /rooms/{room_id}/messages/{message_id}/reactions/{reaction_id}

Remove own reaction from a message.

**Response:** `204 No Content`.

---

## Relationship Endpoints

### POST /relationships/request/{user_id}

Send a friend request.

**Response (201):**
```json
{ "message": "Friend request sent" }
```

### POST /relationships/respond/{relationship_id}?action={accept|reject}

Respond to a pending friend request.

**Query Parameters:**
- `action` (required): `accept` or `reject`

**Response (200):**
```json
{ "message": "Friend request accepted" }
```

### GET /relationships/friends

List all accepted friends.

**Response (200):**
```json
[
  {
    "id": "uuid",
    "username": "janedoe",
    "display_name": "Jane Doe",
    "avatar_url": null,
    "is_online": true
  }
]
```

### GET /relationships/pending

List pending friend requests received by the authenticated user.

**Response (200):**
```json
[
  {
    "id": "uuid",
    "user": {
      "id": "uuid",
      "username": "bob",
      "display_name": "Bob",
      "avatar_url": null
    },
    "created_at": "2025-01-01T00:00:00Z"
  }
]
```

### DELETE /relationships/{relationship_id}

Remove a friend or cancel a pending request.

**Response (200):**
```json
{ "message": "Friend removed" }
```

---

## Search Endpoint

### GET /search/messages?q={query}&room_id={room_id}&limit={limit}

Search messages across rooms the user is a member of.

**Query Parameters:**
- `q` (required): Search query, 1-200 characters
- `room_id` (optional): Limit search to a specific room
- `limit` (optional, default 50, max 200)

**Response (200):** Array of `MessageResponse` objects. Excludes deleted messages.

---

## Upload Endpoint

### POST /upload?room_id={room_id}&message_id={message_id}

Upload a file attachment.

**Query Parameters:**
- `room_id` (required): Target room
- `message_id` (optional): Attach file to an existing message

**Request:** `multipart/form-data` with a `file` field.

**Allowed file types:** JPEG, PNG, GIF, WebP, PDF, plain text, ZIP, RAR, 7z, DOC, DOCX.

**Max file size:** 50 MB.

**Response (201):**
```json
{
  "id": "uuid",
  "message_id": "uuid",
  "file_name": "photo.jpg",
  "file_url": "/uploads/2025/01/01/uuid.jpg",
  "file_type": "image",
  "file_size": 1024000,
  "mime_type": "image/jpeg"
}
```

---

## Notification Endpoints

### GET /notifications?limit={limit}&offset={offset}&unread_only={bool}

List notifications for the authenticated user.

**Response (200):**
```json
{
  "items": [
    {
      "id": "uuid",
      "title": "New message in General",
      "body": "John: Hello everyone!",
      "data": { "room_id": "uuid", "type": "new_message" },
      "notification_type": "push",
      "is_read": false,
      "created_at": "2025-01-01T00:00:00Z"
    }
  ],
  "total": 10,
  "unread_count": 3
}
```

### GET /notifications/unread-count

Get the count of unread notifications.

**Response (200):**
```json
{ "unread_count": 3 }
```

### PATCH /notifications/{notification_id}/read

Mark a single notification as read.

**Response (200):** Updated `NotificationResponse`.

### POST /notifications/read-all

Mark all notifications as read.

**Response (200):**
```json
{ "message": "All notifications marked as read" }
```

---

## Health Endpoints

### GET /health

Basic health check.

**Response (200):**
```json
{ "status": "ok", "service": "api-service" }
```

### GET /health/db

Database connectivity check.

**Response (200):**
```json
{ "status": "ok", "database": "connected" }
```

**Response (500) on failure:**
```json
{ "status": "error", "database": "connection refused" }
```

---

## Error Responses

All errors follow this structure:

```json
{
  "detail": "Error message describing what went wrong"
}
```

Common HTTP status codes:
- `400` - Bad request (validation error, invalid parameters)
- `401` - Unauthorized (missing/invalid token)
- `403` - Forbidden (insufficient permissions)
- `404` - Resource not found
- `409` - Conflict (duplicate resource)
- `422` - Validation error (invalid request body)
- `429` - Rate limited (too many requests)
- `500` - Internal server error

## Rate Limiting

Rate limits are enforced per IP address:
- 100 requests/minute for authenticated endpoints
- 20 requests/minute for auth endpoints (register, login)
- 60 requests/minute for WebSocket connections

Response headers include `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`.
