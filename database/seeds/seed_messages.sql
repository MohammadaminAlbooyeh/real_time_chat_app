INSERT INTO messages (room_id, sender_id, content, message_type)
SELECT r.id, u.id, 'Hello everyone!', 'text'
FROM rooms r, users u
WHERE r.name = 'General' AND u.username = 'alice';

INSERT INTO messages (room_id, sender_id, content, message_type)
SELECT r.id, u.id, 'Hey Alice!', 'text'
FROM rooms r, users u
WHERE r.name = 'General' AND u.username = 'bob';

INSERT INTO messages (room_id, sender_id, content, message_type)
SELECT r.id, u.id, 'How is everyone doing?', 'text'
FROM rooms r, users u
WHERE r.name = 'General' AND u.username = 'charlie';
