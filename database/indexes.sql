CREATE INDEX IF NOT EXISTS idx_messages_room_id ON messages(room_id);
CREATE INDEX IF NOT EXISTS idx_messages_sender_id ON messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_created_at ON messages(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_room_created ON messages(room_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_room_pinned ON messages(room_id, is_pinned);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_online ON users(is_online) WHERE is_online = TRUE;

CREATE INDEX IF NOT EXISTS idx_room_members_room ON room_members(room_id);
CREATE INDEX IF NOT EXISTS idx_room_members_user ON room_members(user_id);

CREATE INDEX IF NOT EXISTS idx_relationships_requester ON relationships(requester_id);
CREATE INDEX IF NOT EXISTS idx_relationships_addressee ON relationships(addressee_id);
CREATE INDEX IF NOT EXISTS idx_relationships_status ON relationships(status);

CREATE INDEX IF NOT EXISTS idx_message_reactions_message ON message_reactions(message_id);
CREATE INDEX IF NOT EXISTS idx_message_attachments_message ON message_attachments(message_id);

CREATE INDEX IF NOT EXISTS idx_rooms_created_by ON rooms(created_by_id);

CREATE INDEX IF NOT EXISTS idx_user_status_history_user ON user_status_history(user_id);
CREATE INDEX IF NOT EXISTS idx_user_status_history_time ON user_status_history(changed_at DESC);

CREATE INDEX IF NOT EXISTS idx_user_notifications_user ON user_notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_user_notifications_unread ON user_notifications(user_id) WHERE is_read = FALSE;
