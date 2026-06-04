# API Reference

Base URL: `https://yourdomain.com/api/v1` (production) / `http://localhost:8000/api/v1` (development)

## Authentication

All protected endpoints require a Bearer JWT token:

```
Authorization: Bearer <access_token>
```

Access tokens expire in 30 minutes. Use the refresh endpoint to obtain new tokens.

---

## 1. Auth

### POST /auth/register

Create a new user account.

**Request body:**
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "securePassword123!",
  "display_name": "John Doe"
}
```

**Validation rules:**
- `username`: 3-50 chars, alphanumeric + underscore, unique
- `email`: valid email format, unique
- `password`: 8-128 chars
- `display_name`: optional, max 100 chars

**Response** `201 Created`:
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer",
  "user": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "username": "johndoe",
    "email": "john@example.com",
    "display_name": "John Doe",
    "avatar_url": null,
    "bio": null,
    "is_online": false,
    "last_seen": "2025-06-04T12:00:00Z",
    "created_at": "2025-06-04T12:00:00Z"
  }
}
```

**Error responses:**
- `409 Conflict`: `{"detail": "Email or username already taken"}`

### POST /auth/login

Authenticate with email and password.

**Request body:**
```json
{
  "email": "john@example.com",
  "password": "securePassword123!"
}
```

**Response** `200 OK`: Same structure as register.

**Error responses:**
- `401 Unauthorized`: `{"detail": "Invalid email or password"}`
- `403 Forbidden`: `{"detail": "Account is deactivated"}`

### POST /auth/refresh

Refresh an expired access token using a refresh token.

**Request body:**
```json
{
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response** `200 OK`: Returns new `access_token`, `refresh_token`, and `user`.

**Error responses:**
- `401 Unauthorized`: `{"detail": "Invalid or expired refresh token"}`
- `401 Unauthorized`: `{"detail": "User not found or inactive"}`

### POST /auth/logout

Log out the current user (client-side token discard; server returns confirmation).

**Response** `200 OK`:
```json
{
  "message": "Logged out successfully"
}
```

---

## 2. Users

### GET /users/me

Get the authenticated user's profile.

**Response** `200 OK`: Full `UserResponse` object.

### PATCH /users/me

Update profile fields. Only provided fields are updated.

**Request body** (all optional):
```json
{
  "display_name": "New Display Name",
  "avatar_url": "https://cdn.example.com/avatars/uuid.jpg",
  "bio": "Software engineer and coffee enthusiast."
}
```

**Validation:**
- `display_name`: max 100 chars
- `avatar_url`: max 500 chars
- `bio`: max 500 chars

**Response** `200 OK`: Updated `UserResponse`.

### POST /users/me/change-password

Change the current user's password.

**Request body:**
```json
{
  "current_password": "oldPassword123!",
  "new_password": "newPassword456!"
}
```

**Response** `200 OK`:
```json
{
  "message": "Password changed successfully"
}
```

**Error responses:**
- `400 Bad Request`: `{"detail": "Current password is incorrect"}`

### GET /users/search?q={query}&limit={limit}

Search for users by username or display_name (case-insensitive).

**Parameters:**
| Name | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| `q` | string | Yes | - | Search query, 1-50 chars |
| `limit` | integer | No | 20 | Max results (max 200) |

**Response** `200 OK`:
```json
[
  {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "username": "johndoe",
    "display_name": "John Doe",
    "avatar_url": null,
    "is_online": true
  }
]
```

The authenticated user is excluded from results.

### GET /users/{user_id}

Get a user's public profile by UUID.

**Response** `200 OK`: Full `UserResponse` object.

**Error responses:**
- `404 Not Found`: `{"detail": "User not found"}`

---

## 3. Rooms

### POST /rooms

Create a new chat room. The creator becomes an admin member automatically.

**Request body:**
```json
{
  "name": "Project Alpha",
  "description": "Discussion about Project Alpha",
  "is_private": false
}
```

**Validation:**
- `name`: 1-100 chars, required
- `description`: max 500 chars, optional
- `is_private`: boolean, default false

**Response** `201 Created`:
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "name": "Project Alpha",
  "description": "Discussion about Project Alpha",
  "is_private": false,
  "created_by_id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "created_at": "2025-06-04T12:00:00Z",
  "updated_at": "2025-06-04T12:00:00Z",
  "member_count": 1,
  "last_message": null,
  "last_message_at": null
}
```

**Error responses:**
- `400 Bad Request`: `{"detail": "Maximum rooms limit reached"}` (limit is 100)

### GET /rooms?limit={limit}&offset={offset}

List rooms the user is a member of, ordered by most recent activity.

**Parameters:**
| Name | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| `limit` | integer | No | 50 | Max results (max 200) |
| `offset` | integer | No | 0 | Pagination offset |

**Response** `200 OK`: Array of `RoomResponse` objects.

### GET /rooms/{room_id}

Get detailed room information including members.

**Response** `200 OK`:
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "name": "Project Alpha",
  "description": "Discussion about Project Alpha",
  "is_private": false,
  "created_by_id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "created_at": "2025-06-04T12:00:00Z",
  "updated_at": "2025-06-04T12:00:00Z",
  "member_count": 3,
  "members": [
    {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "user_id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "username": "johndoe",
      "display_name": "John Doe",
      "avatar_url": null,
      "role": "admin",
      "joined_at": "2025-06-04T12:00:00Z"
    },
    {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa7",
      "user_id": "3fa85f64-5717-4562-b3fc-2c963f66afa7",
      "username": "janedoe",
      "display_name": "Jane Doe",
      "avatar_url": null,
      "role": "member",
      "joined_at": "2025-06-04T12:30:00Z"
    }
  ]
}
```

**Error responses:**
- `403 Forbidden`: `{"detail": "You are not a member of this room"}`
- `404 Not Found`: `{"detail": "Room not found"}`

### PUT /rooms/{room_id}

Update room settings (admin only).

**Request body** (all optional):
```json
{
  "name": "Project Alpha Renamed",
  "description": "Updated description",
  "is_private": true
}
```

**Response** `200 OK`: Updated `RoomResponse`.

**Error responses:**
- `403 Forbidden`: `{"detail": "Only room admins can update the room"}`

### DELETE /rooms/{room_id}

Delete a room (creator only). Cascades to delete members, messages, and reactions.

**Response** `204 No Content`.

**Error responses:**
- `403 Forbidden`: `{"detail": "Only the room creator can delete the room"}`

### POST /rooms/{room_id}/members

Add a member to a room (admin only).

**Request body:**
```json
{
  "user_id": "3fa85f64-5717-4562-b3fc-2c963f66afa7"
}
```

**Response** `201 Created`:
```json
{
  "message": "Member added successfully"
}
```

**Error responses:**
- `403 Forbidden`: `{"detail": "Only admins can add members"}`
- `409 Conflict`: `{"detail": "User is already a member"}`

### DELETE /rooms/{room_id}/members/{user_id}

Remove a member from a room (admin only).

**Response** `200 OK`:
```json
{
  "message": "Member removed successfully"
}
```

### POST /rooms/{room_id}/leave

Leave a room (remove yourself from members).

**Response** `200 OK`:
```json
{
  "message": "Left room successfully"
}
```

**Error responses:**
- `404 Not Found`: `{"detail": "You are not a member of this room"}`

---

## 4. Messages

### GET /rooms/{room_id}/messages?limit={limit}&offset={offset}&before={iso_datetime}

Get messages in a room, newest first.

**Parameters:**
| Name | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| `limit` | integer | No | 50 | Max results (max 200) |
| `offset` | integer | No | 0 | Pagination offset |
| `before` | ISO datetime | No | - | Cursor: get messages before this timestamp |

**Response** `200 OK`:
```json
{
  "items": [
    {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "room_id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "sender_id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "sender_username": "johndoe",
      "sender_avatar": null,
      "content": "Hello everyone!",
      "message_type": "text",
      "reply_to_id": null,
      "reply_preview": null,
      "is_deleted": false,
      "edited_at": null,
      "created_at": "2025-06-04T12:00:00Z",
      "reactions": [
        {
          "id": "3fa85f64-5717-4562-b3fc-2c963f66afa7",
          "user_id": "3fa85f64-5717-4562-b3fc-2c963f66afa7",
          "username": "janedoe",
          "emoji": "👍",
          "created_at": "2025-06-04T12:00:05Z"
        }
      ],
      "attachments": [
        {
          "id": "3fa85f64-5717-4562-b3fc-2c963f66afa8",
          "file_name": "presentation.pdf",
          "file_url": "/uploads/2025/06/04/uuid.pdf",
          "file_type": "document",
          "file_size": 2048000,
          "created_at": "2025-06-04T12:00:01Z"
        }
      ]
    }
  ],
  "total": 150,
  "limit": 50,
  "offset": 0,
  "has_more": true
}
```

### POST /rooms/{room_id}/messages

Send a message in a room.

**Request body:**
```json
{
  "room_id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "content": "Hello, world!",
  "message_type": "text",
  "reply_to_id": null
}
```

**Message types:** `text`, `image`, `file`, `system`

**Validation:**
- `content`: 1-5000 chars, required
- `reply_to_id`: optional, must reference a valid message in the same room

**Response** `201 Created`: Full `MessageResponse`.

### PUT /rooms/{room_id}/messages/{message_id}

Edit your own message.

**Request body:**
```json
{
  "content": "Updated message content"
}
```

**Response** `200 OK`: Updated `MessageResponse` with `edited_at` timestamp set.

**Error responses:**
- `403 Forbidden`: `{"detail": "Can only edit your own messages"}`
- `400 Bad Request`: `{"detail": "Cannot edit a deleted message"}`

### DELETE /rooms/{room_id}/messages/{message_id}

Soft-delete your own message. Content is replaced with `[deleted]`.

**Response** `204 No Content`.

**Error responses:**
- `403 Forbidden`: `{"detail": "Can only delete your own messages"}`

### POST /rooms/{room_id}/messages/{message_id}/reactions

Add a reaction emoji to a message.

**Request body:**
```json
{
  "emoji": "🎉"
}
```

**Response** `201 Created`:
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "user_id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "username": "johndoe",
  "emoji": "🎉",
  "created_at": "2025-06-04T12:00:00Z"
}
```

**Error responses:**
- `409 Conflict`: `{"detail": "Reaction already exists"}`

### DELETE /rooms/{room_id}/messages/{message_id}/reactions/{reaction_id}

Remove your own reaction from a message.

**Response** `204 No Content`.

---

## 5. Relationships

### POST /relationships/request/{user_id}

Send a friend request to another user.

**Response** `201 Created`:
```json
{
  "message": "Friend request sent"
}
```

**Error responses:**
- `400 Bad Request`: `{"detail": "Cannot send friend request to yourself"}`
- `404 Not Found`: `{"detail": "User not found"}`
- `409 Conflict`: `{"detail": "Relationship already exists"}`

### POST /relationships/respond/{relationship_id}?action={action}

Respond to a pending friend request.

**Parameters:**
| Name | Type | Required | Description |
|------|------|----------|-------------|
| `action` | string | Yes | `accept` or `reject` |

**Response** `200 OK`:
```json
{
  "message": "Friend request accepted"
}
```

**Error responses:**
- `403 Forbidden`: `{"detail": "Not authorized to respond to this request"}`
- `400 Bad Request`: `{"detail": "Request already responded to"}`

### GET /relationships/friends

List all accepted friends.

**Response** `200 OK`:
```json
[
  {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "username": "janedoe",
    "display_name": "Jane Doe",
    "avatar_url": null,
    "is_online": true
  }
]
```

### GET /relationships/pending

List pending friend requests received.

**Response** `200 OK`:
```json
[
  {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "user": {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa7",
      "username": "bob",
      "display_name": "Bob Smith",
      "avatar_url": null
    },
    "created_at": "2025-06-03T12:00:00Z"
  }
]
```

### DELETE /relationships/{relationship_id}

Remove a friend or cancel a pending request.

**Response** `200 OK`:
```json
{
  "message": "Friend removed"
}
```

---

## 6. Search

### GET /search/messages?q={query}&room_id={room_id}&limit={limit}

Search message content across rooms the user is a member of.

**Parameters:**
| Name | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| `q` | string | Yes | - | Search query, 1-200 chars |
| `room_id` | UUID | No | - | Limit search to specific room |
| `limit` | integer | No | 50 | Max results (max 200) |

**Response** `200 OK`: Array of `MessageResponse` objects.

---

## 7. Upload

### POST /upload?room_id={room_id}&message_id={message_id}

Upload a file attachment.

**Parameters:**
| Name | Type | Required | Description |
|------|------|----------|-------------|
| `room_id` | UUID | Yes | Target room |
| `message_id` | UUID | No | Attach to existing message |

**Request:** `multipart/form-data` with field `file`.

**Allowed types:**
- Images: JPEG, PNG, GIF, WebP
- Documents: PDF, TXT, DOC, DOCX
- Archives: ZIP, RAR, 7z

**Max size:** 50 MB

**File storage:** `<upload_dir>/YYYY/MM/DD/<uuid>.<ext>`

**Response** `201 Created`:
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "message_id": null,
  "file_name": "photo.jpg",
  "file_url": "/uploads/2025/06/04/uuid.jpg",
  "file_type": "image",
  "file_size": 1024000,
  "mime_type": "image/jpeg"
}
```

---

## 8. Notifications

### GET /notifications?limit={limit}&offset={offset}&unread_only={bool}

List notifications for the authenticated user.

**Response** `200 OK`:
```json
{
  "items": [
    {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "title": "New message in Project Alpha",
      "body": "John: Check out this link",
      "data": {
        "room_id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "type": "new_message"
      },
      "notification_type": "push",
      "is_read": false,
      "created_at": "2025-06-04T12:00:00Z"
    }
  ],
  "total": 10,
  "unread_count": 3
}
```

### GET /notifications/unread-count

**Response** `200 OK`:
```json
{
  "unread_count": 3
}
```

### PATCH /notifications/{notification_id}/read

Mark a notification as read.

**Response** `200 OK`: Updated `NotificationResponse`.

### POST /notifications/read-all

Mark all user notifications as read.

**Response** `200 OK`:
```json
{
  "message": "All notifications marked as read"
}
```

---

## 9. Health

### GET /health

**Response** `200 OK`:
```json
{
  "status": "ok",
  "service": "api-service"
}
```

### GET /health/db

**Response** `200 OK`:
```json
{
  "status": "ok",
  "database": "connected"
}
```

**Response on failure** (HTTP 500):
```json
{
  "status": "error",
  "database": "connection refused"
}
```

---

## Common Error Responses

```json
{
  "detail": "Human-readable error description"
}
```

| Status | Meaning |
|--------|---------|
| 400 | Bad request - validation error or invalid parameters |
| 401 | Unauthorized - missing or invalid authentication |
| 403 | Forbidden - insufficient permissions |
| 404 | Resource not found |
| 409 | Conflict - duplicate or conflicting state |
| 422 | Unprocessable entity - request body validation failed |
| 429 | Too many requests - rate limit exceeded |
| 500 | Internal server error |

## Rate Limiting

| Endpoint Type | Limit |
|---------------|-------|
| Authenticated endpoints | 100 req/min per IP |
| Auth (register, login) | 20 req/min per IP |
| WebSocket connections | 60/min per IP |

Rate limit headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`
