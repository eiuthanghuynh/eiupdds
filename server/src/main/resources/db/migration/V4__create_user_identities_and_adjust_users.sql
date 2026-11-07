-- Flyway Migration: V3__create_user_identities_and_adjust_users.sql
-- Description: Federated Identity Pattern for Social Login and User Profile

-- 1. Allow nullable password_hash for users registered via external OAuth2 identity providers
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

-- 2. Add avatar_url for user profile icon / social picture
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url TEXT;

-- 3. Create user_identities table for federated authentication
CREATE TABLE IF NOT EXISTS user_identities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider TEXT NOT NULL CHECK (provider IN ('LOCAL', 'GOOGLE')),
    provider_user_id TEXT NOT NULL,
    email TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_provider_user_id UNIQUE (provider, provider_user_id)
);

-- 4. Performance Indexes
CREATE INDEX IF NOT EXISTS idx_user_identities_user_id ON user_identities(user_id);
CREATE INDEX IF NOT EXISTS idx_user_identities_email ON user_identities(email);
