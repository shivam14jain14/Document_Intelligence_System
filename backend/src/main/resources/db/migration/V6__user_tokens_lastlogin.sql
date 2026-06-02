-- Token version: incrementing this invalidates all of a user's existing JWTs ("log out everywhere")
ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 0;

-- Track last successful login
ALTER TABLE users ADD COLUMN last_login TIMESTAMP;
