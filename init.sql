CREATE DATABASE keycloak;
\c management_db
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL
);
-- Optional: Add a default user
INSERT INTO users (username, email, password, role, status)
VALUES ('admin', 'admin@example.com', '$2a$10$dXJ1aW5nLmV4YW1wbGUkMOhM8Zg8s8X8X8X8X8X8X8', 'ADMIN', 'ACTIVE');