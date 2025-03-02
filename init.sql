-- Create the database if it doesn’t exist
-- DO $$ 
-- BEGIN 
--    IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'management_db') THEN
--       CREATE DATABASE management_db;
--    END IF;
-- END $$;

-- -- Switch to the management_db database
-- \c management_db

-- Create the users table
CREATE TABLE IF NOT EXISTS users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE,
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    role VARCHAR(50),
    status VARCHAR(50)
);