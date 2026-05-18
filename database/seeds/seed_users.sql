INSERT INTO users (id, username, email, hashed_password, display_name, is_online, is_active)
VALUES
    (uuid_generate_v4(), 'alice', 'alice@example.com', '$2b$12$LJ3m4ys3Lk0TSwHnbfOMiOXPm1Qlq5yjYKq8iYLn7rJmKkRqzvXyK', 'Alice Johnson', TRUE, TRUE),
    (uuid_generate_v4(), 'bob', 'bob@example.com', '$2b$12$LJ3m4ys3Lk0TSwHnbfOMiOXPm1Qlq5yjYKq8iYLn7rJmKkRqzvXyK', 'Bob Smith', FALSE, TRUE),
    (uuid_generate_v4(), 'charlie', 'charlie@example.com', '$2b$12$LJ3m4ys3Lk0TSwHnbfOMiOXPm1Qlq5yjYKq8iYLn7rJmKkRqzvXyK', 'Charlie Brown', TRUE, TRUE)
ON CONFLICT (username) DO NOTHING;
