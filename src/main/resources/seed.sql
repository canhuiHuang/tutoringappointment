-- Local sample accounts. Every password is 1234, stored as a BCrypt hash.
-- Existing accounts and bookings are preserved on restart.

INSERT INTO users (name, email, role, password_hash)
SELECT 'Carlos Montoya', 'carlos@smallfolk.com', 'STUDENT', '$2a$10$n0BByA.hLdiYmA8daOQ7S.Ol.o4CvlZiZCpuqUDw2SJVizkyrvrbi'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'carlos@smallfolk.com');

INSERT INTO users (name, email, role, password_hash)
SELECT 'Second Student', 'student2@sjsu.edu', 'STUDENT', '$2a$10$n0BByA.hLdiYmA8daOQ7S.Ol.o4CvlZiZCpuqUDw2SJVizkyrvrbi'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'student2@sjsu.edu');

INSERT INTO users (name, email, role, password_hash)
SELECT 'Cercei Lannister', 'cerceilion@sjsu.edu', 'TUTOR', '$2a$10$n0BByA.hLdiYmA8daOQ7S.Ol.o4CvlZiZCpuqUDw2SJVizkyrvrbi'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'cerceilion@sjsu.edu');

INSERT INTO users (name, email, role, password_hash)
SELECT 'Balaji Venkatraman', 'balaji@sjsu.edu', 'TUTOR', '$2a$10$n0BByA.hLdiYmA8daOQ7S.Ol.o4CvlZiZCpuqUDw2SJVizkyrvrbi'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'balaji@sjsu.edu');

INSERT INTO users (name, email, role, password_hash)
SELECT 'John', 'john@sjsu.edu', 'STUDENT', '$2a$10$n0BByA.hLdiYmA8daOQ7S.Ol.o4CvlZiZCpuqUDw2SJVizkyrvrbi'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'john@sjsu.edu');

INSERT INTO users (name, email, role, password_hash)
SELECT 'Alice', 'alice@sjsu.edu', 'TUTOR', '$2a$10$n0BByA.hLdiYmA8daOQ7S.Ol.o4CvlZiZCpuqUDw2SJVizkyrvrbi'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'alice@sjsu.edu');

INSERT INTO users (name, email, role, password_hash)
SELECT 'Bob', 'bob@sjsu.edu', 'TUTOR', '$2a$10$n0BByA.hLdiYmA8daOQ7S.Ol.o4CvlZiZCpuqUDw2SJVizkyrvrbi'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'bob@sjsu.edu');

INSERT INTO users (name, email, role, password_hash)
SELECT 'Emma', 'emma@sjsu.edu', 'TUTOR', '$2a$10$n0BByA.hLdiYmA8daOQ7S.Ol.o4CvlZiZCpuqUDw2SJVizkyrvrbi'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'emma@sjsu.edu');

INSERT INTO providers (user_id, bio)
SELECT id, 'Calculus tutor' FROM users
WHERE email = 'cerceilion@sjsu.edu'
AND NOT EXISTS (SELECT 1 FROM providers WHERE user_id = users.id);

INSERT INTO services (provider_id, name, description, duration)
SELECT p.id, 'Calculus', 'Calculus tutoring session', 60
FROM providers p JOIN users u ON u.id = p.user_id
WHERE u.email = 'cerceilion@sjsu.edu'
AND NOT EXISTS (SELECT 1 FROM services WHERE provider_id = p.id AND name = 'Calculus');

INSERT INTO providers (user_id, bio)
SELECT id, 'OOP tutor' FROM users
WHERE email = 'balaji@sjsu.edu'
AND NOT EXISTS (SELECT 1 FROM providers WHERE user_id = users.id);

INSERT INTO services (provider_id, name, description, duration)
SELECT p.id, 'OOP', 'OOP tutoring session', 120
FROM providers p JOIN users u ON u.id = p.user_id
WHERE u.email = 'balaji@sjsu.edu'
AND NOT EXISTS (SELECT 1 FROM services WHERE provider_id = p.id AND name = 'OOP');

INSERT INTO providers (user_id, bio)
SELECT id, 'Algebra tutor' FROM users
WHERE email = 'alice@sjsu.edu'
AND NOT EXISTS (SELECT 1 FROM providers WHERE user_id = users.id);

INSERT INTO services (provider_id, name, description, duration)
SELECT p.id, 'Algebra', 'Algebra tutoring session', 60
FROM providers p JOIN users u ON u.id = p.user_id
WHERE u.email = 'alice@sjsu.edu'
AND NOT EXISTS (SELECT 1 FROM services WHERE provider_id = p.id AND name = 'Algebra');

INSERT INTO providers (user_id, bio)
SELECT id, 'Java Programming tutor' FROM users
WHERE email = 'bob@sjsu.edu'
AND NOT EXISTS (SELECT 1 FROM providers WHERE user_id = users.id);

INSERT INTO services (provider_id, name, description, duration)
SELECT p.id, 'Java Programming', 'Java Programming tutoring session', 90
FROM providers p JOIN users u ON u.id = p.user_id
WHERE u.email = 'bob@sjsu.edu'
AND NOT EXISTS (SELECT 1 FROM services WHERE provider_id = p.id AND name = 'Java Programming');

INSERT INTO providers (user_id, bio)
SELECT id, 'Physics tutor' FROM users
WHERE email = 'emma@sjsu.edu'
AND NOT EXISTS (SELECT 1 FROM providers WHERE user_id = users.id);

INSERT INTO services (provider_id, name, description, duration)
SELECT p.id, 'Physics', 'Physics tutoring session', 60
FROM providers p JOIN users u ON u.id = p.user_id
WHERE u.email = 'emma@sjsu.edu'
AND NOT EXISTS (SELECT 1 FROM services WHERE provider_id = p.id AND name = 'Physics');

-- Each tutor works three different days in each seven-day cycle over the next eight weeks.
-- Skip matching slots in every status, including BOOKED and REMOVED.
INSERT INTO availability_slots (provider_id, service_id, start_time, end_time, status)
SELECT s.provider_id, s.id,
       CAST(CURRENT_DATE AS TIMESTAMP) + days.day_offset * INTERVAL '1' DAY
           + sample.start_hour * INTERVAL '1' HOUR,
       CAST(CURRENT_DATE AS TIMESTAMP) + days.day_offset * INTERVAL '1' DAY
           + sample.start_hour * INTERVAL '1' HOUR + s.duration * INTERVAL '1' MINUTE,
       'AVAILABLE'
FROM services s
JOIN providers p ON p.id = s.provider_id
JOIN users u ON u.id = p.user_id
JOIN (VALUES
    ('cerceilion@sjsu.edu', 'Calculus', 10, 1, 3, 5),
    ('balaji@sjsu.edu', 'OOP', 13, 2, 4, 6),
    ('alice@sjsu.edu', 'Algebra', 10, 1, 2, 4),
    ('bob@sjsu.edu', 'Java Programming', 13, 0, 3, 5),
    ('emma@sjsu.edu', 'Physics', 16, 0, 2, 6)
) AS sample(username, service_name, start_hour, day_a, day_b, day_c)
    ON sample.username = u.email AND sample.service_name = s.name
CROSS JOIN (VALUES (1), (2), (3), (4), (5), (6), (7),
                   (8), (9), (10), (11), (12), (13), (14),
                   (15), (16), (17), (18), (19), (20), (21),
                   (22), (23), (24), (25), (26), (27), (28),
                   (29), (30), (31), (32), (33), (34), (35),
                   (36), (37), (38), (39), (40), (41), (42),
                   (43), (44), (45), (46), (47), (48), (49),
                   (50), (51), (52), (53), (54), (55), (56)) AS days(day_offset)
WHERE MOD(days.day_offset, 7) IN (sample.day_a, sample.day_b, sample.day_c)
AND NOT EXISTS (
    SELECT 1 FROM availability_slots existing
    WHERE existing.service_id = s.id
      AND existing.start_time = CAST(CURRENT_DATE AS TIMESTAMP)
          + days.day_offset * INTERVAL '1' DAY + sample.start_hour * INTERVAL '1' HOUR
);
