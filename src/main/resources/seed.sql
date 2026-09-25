INSERT INTO users (name, email, role)
VALUES ('Carlos Montoya', 'carlos@smallfolk.com', 'STUDENT');

INSERT INTO users (name, email, role)
VALUES ('Cercei Lannister', 'cerceilion@sjsu.edu', 'TUTOR');

INSERT INTO users (name, email, role)
VALUES ('Balaji Venkatraman', 'balaji@sjsu.edu', 'TUTOR');

-- Providers
INSERT INTO providers (user_id, bio)
VALUES (2, '10 years in the filed of mathematics.');

INSERT INTO providers (user_id, bio)
VALUES (3, 'Phd in computer science.');

-- Services
INSERT INTO services (provider_id, name, description, duration)
VALUES (1, 'Calculus', 'Calculus tutoring session', 60);

INSERT INTO services (provider_id, name, description, duration)
VALUES (2, 'OOP', 'OOP tutoring session', 120);

-- Availability slots
INSERT INTO availability_slots
(provider_id, service_id, start_time, end_time, status)
VALUES (1, 1, '2026-10-01 10:00:00', '2026-10-01 11:00:00', 'AVAILABLE');

INSERT INTO availability_slots
(provider_id, service_id, start_time, end_time, status)
VALUES (1, 1, '2026-10-01 13:00:00', '2026-10-01 14:00:00', 'AVAILABLE');

INSERT INTO availability_slots
(provider_id, service_id, start_time, end_time, status)
VALUES (2, 2, '2026-10-02 15:00:00', '2026-10-02 17:00:00', 'AVAILABLE');