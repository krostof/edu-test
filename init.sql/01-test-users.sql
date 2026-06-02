-- Testowi użytkownicy dla systemu EduTest
-- Hasło dla wszystkich użytkowników: Test123!
-- Hash BCrypt został wygenerowany za pomocą BCryptPasswordEncoder (strength 10)
-- Aby wygenerować nowy hash, uruchom: mvn exec:java -Dexec.mainClass="com.edutest.webserver.util.BCryptHashGenerator"

-- Usuń constraint NOT NULL z kolumny role (teraz używamy tabeli user_roles)
ALTER TABLE users ALTER COLUMN role DROP NOT NULL;

-- Administratorzy (3)
INSERT INTO users (username, email, password, first_name, last_name, role, is_active, student_number, created_at, updated_at)
VALUES
    ('admin', 'admin@edutest.pl', '$2a$10$ruJf3.J8/4A5UPkAGYEOZOFBc72vyz3N1DoXKaWgkn9DNnBBIlbES', 'Jan', 'Kowalski', 'ADMIN', true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('admin2', 'admin2@edutest.pl', '$2a$10$ruJf3.J8/4A5UPkAGYEOZOFBc72vyz3N1DoXKaWgkn9DNnBBIlbES', 'Anna', 'Nowak', 'ADMIN', true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('superadmin', 'superadmin@edutest.pl', '$2a$10$ruJf3.J8/4A5UPkAGYEOZOFBc72vyz3N1DoXKaWgkn9DNnBBIlbES', 'Piotr', 'Wiśniewski', 'ADMIN', true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;

-- Nauczyciele (5)
INSERT INTO users (username, email, password, first_name, last_name, role, is_active, student_number, created_at, updated_at)
VALUES
    ('nauczyciel1', 'nauczyciel1@edutest.pl', '$2a$10$ruJf3.J8/4A5UPkAGYEOZOFBc72vyz3N1DoXKaWgkn9DNnBBIlbES', 'Maria', 'Lewandowska', 'TEACHER', true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('nauczyciel2', 'nauczyciel2@edutest.pl', '$2a$10$ruJf3.J8/4A5UPkAGYEOZOFBc72vyz3N1DoXKaWgkn9DNnBBIlbES', 'Tomasz', 'Wójcik', 'TEACHER', true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('nauczyciel3', 'nauczyciel3@edutest.pl', '$2a$10$ruJf3.J8/4A5UPkAGYEOZOFBc72vyz3N1DoXKaWgkn9DNnBBIlbES', 'Katarzyna', 'Kamińska', 'TEACHER', true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('nauczyciel4', 'nauczyciel4@edutest.pl', '$2a$10$ruJf3.J8/4A5UPkAGYEOZOFBc72vyz3N1DoXKaWgkn9DNnBBIlbES', 'Marek', 'Zieliński', 'TEACHER', true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('nauczyciel5', 'nauczyciel5@edutest.pl', '$2a$10$ruJf3.J8/4A5UPkAGYEOZOFBc72vyz3N1DoXKaWgkn9DNnBBIlbES', 'Ewa', 'Szymańska', 'TEACHER', true, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;

-- Studenci (10)
INSERT INTO users (username, email, password, first_name, last_name, role, is_active, student_number, created_at, updated_at)
VALUES
    ('student1', 'student1@edutest.pl', '$2a$10$ruJf3.J8/4A5UPkAGYEOZOFBc72vyz3N1DoXKaWgkn9DNnBBIlbES', 'Adam', 'Dąbrowski', 'STUDENT', true, 'S001234', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('student2', 'student2@edutest.pl', '$2a$10$ruJf3.J8/4A5UPkAGYEOZOFBc72vyz3N1DoXKaWgkn9DNnBBIlbES', 'Barbara', 'Kozłowska', 'STUDENT', true, 'S001235', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('student3', 'student3@edutest.pl', '$2a$10$ruJf3.J8/4A5UPkAGYEOZOFBc72vyz3N1DoXKaWgkn9DNnBBIlbES', 'Cezary', 'Mazur', 'STUDENT', true, 'S001236', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('student4', 'student4@edutest.pl', '$2a$10$ruJf3.J8/4A5UPkAGYEOZOFBc72vyz3N1DoXKaWgkn9DNnBBIlbES', 'Dorota', 'Jankowska', 'STUDENT', true, 'S001237', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('student5', 'student5@edutest.pl', '$2a$10$ruJf3.J8/4A5UPkAGYEOZOFBc72vyz3N1DoXKaWgkn9DNnBBIlbES', 'Emil', 'Krawczyk', 'STUDENT', true, 'S001238', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('student6', 'student6@edutest.pl', '$2a$10$ruJf3.J8/4A5UPkAGYEOZOFBc72vyz3N1DoXKaWgkn9DNnBBIlbES', 'Franciszka', 'Piotrowski', 'STUDENT', true, 'S001239', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('student7', 'student7@edutest.pl', '$2a$10$ruJf3.J8/4A5UPkAGYEOZOFBc72vyz3N1DoXKaWgkn9DNnBBIlbES', 'Grzegorz', 'Grabowski', 'STUDENT', true, 'S001240', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('student8', 'student8@edutest.pl', '$2a$10$ruJf3.J8/4A5UPkAGYEOZOFBc72vyz3N1DoXKaWgkn9DNnBBIlbES', 'Helena', 'Pawłowska', 'STUDENT', true, 'S001241', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('student9', 'student9@edutest.pl', '$2a$10$ruJf3.J8/4A5UPkAGYEOZOFBc72vyz3N1DoXKaWgkn9DNnBBIlbES', 'Ignacy', 'Michalski', 'STUDENT', true, 'S001242', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('student10', 'student10@edutest.pl', '$2a$10$ruJf3.J8/4A5UPkAGYEOZOFBc72vyz3N1DoXKaWgkn9DNnBBIlbES', 'Joanna', 'Zając', 'STUDENT', true, 'S001243', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;

-- Dodatkowi nieaktywni użytkownicy dla testów (2)
INSERT INTO users (username, email, password, first_name, last_name, role, is_active, student_number, created_at, updated_at)
VALUES
    ('nieaktywny_student', 'nieaktywny@edutest.pl', '$2a$10$ruJf3.J8/4A5UPkAGYEOZOFBc72vyz3N1DoXKaWgkn9DNnBBIlbES', 'Krzysztof', 'Nowakowski', 'STUDENT', false, 'S001244', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('nieaktywny_teacher', 'nieaktywny.nauczyciel@edutest.pl', '$2a$10$ruJf3.J8/4A5UPkAGYEOZOFBc72vyz3N1DoXKaWgkn9DNnBBIlbES', 'Łucja', 'Olszewska', 'TEACHER', false, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;

-- Role użytkowników (nowa tabela user_roles)
-- Administratorzy
INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN' FROM users WHERE username IN ('admin', 'admin2', 'superadmin')
ON CONFLICT DO NOTHING;

-- Nauczyciele
INSERT INTO user_roles (user_id, role)
SELECT id, 'TEACHER' FROM users WHERE username IN ('nauczyciel1', 'nauczyciel2', 'nauczyciel3', 'nauczyciel4', 'nauczyciel5', 'nieaktywny_teacher')
ON CONFLICT DO NOTHING;

-- Studenci
INSERT INTO user_roles (user_id, role)
SELECT id, 'STUDENT' FROM users WHERE username IN ('student1', 'student2', 'student3', 'student4', 'student5', 'student6', 'student7', 'student8', 'student9', 'student10', 'nieaktywny_student')
ON CONFLICT DO NOTHING;

-- Testowe grupy studentów
INSERT INTO student_groups (name, description, created_at, updated_at)
VALUES
    ('Grupa A - Informatyka', 'Studenci pierwszego roku informatyki', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Grupa B - Informatyka', 'Studenci drugiego roku informatyki', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Grupa C - Matematyka', 'Studenci pierwszego roku matematyki', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- Przypisanie nauczycieli do grup (tabela group_teachers)
-- Każdy aktywny nauczyciel jest przypisany do wszystkich grup
INSERT INTO group_teachers (group_id, teacher_id)
SELECT sg.id, u.id
FROM student_groups sg
CROSS JOIN users u
JOIN user_roles ur ON u.id = ur.user_id
WHERE ur.role = 'TEACHER' AND u.is_active = true
ON CONFLICT DO NOTHING;

-- Przypisanie studentów do grup
-- Studenci 1-4 do Grupy A
UPDATE users SET student_group_id = (SELECT id FROM student_groups WHERE name = 'Grupa A - Informatyka')
WHERE username IN ('student1', 'student2', 'student3', 'student4');

-- Studenci 5-7 do Grupy B
UPDATE users SET student_group_id = (SELECT id FROM student_groups WHERE name = 'Grupa B - Informatyka')
WHERE username IN ('student5', 'student6', 'student7');

-- Studenci 8-10 do Grupy C
UPDATE users SET student_group_id = (SELECT id FROM student_groups WHERE name = 'Grupa C - Matematyka')
WHERE username IN ('student8', 'student9', 'student10');

-- Backfill @Version (BaseEntity) — Hibernate requires non-null version on UPDATE
UPDATE users SET version = 0 WHERE version IS NULL;
UPDATE student_groups SET version = 0 WHERE version IS NULL;
