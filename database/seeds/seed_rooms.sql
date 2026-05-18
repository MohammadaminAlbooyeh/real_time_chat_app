INSERT INTO rooms (id, name, description, is_private, created_by_id)
SELECT '00000000-0000-0000-0000-000000000001'::uuid, 'General', 'General discussion room', FALSE, id
FROM users WHERE username = 'alice'
ON CONFLICT (id) DO NOTHING;

INSERT INTO rooms (id, name, description, is_private, created_by_id)
SELECT '00000000-0000-0000-0000-000000000002'::uuid, 'Random', 'Random topics', FALSE, id
FROM users WHERE username = 'bob'
ON CONFLICT (id) DO NOTHING;

INSERT INTO room_members (room_id, user_id, role)
SELECT r.id, u.id, CASE WHEN u.username = 'alice' THEN 'admin' ELSE 'member' END
FROM rooms r, users u
WHERE r.name = 'General'
ON CONFLICT (room_id, user_id) DO NOTHING;

INSERT INTO room_members (room_id, user_id, role)
SELECT r.id, u.id, CASE WHEN u.username = 'bob' THEN 'admin' ELSE 'member' END
FROM rooms r, users u
WHERE r.name = 'Random'
ON CONFLICT (room_id, user_id) DO NOTHING;
