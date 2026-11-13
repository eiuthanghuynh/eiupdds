-- Flyway Migration: V5__add_deleted_at_to_user_identities.sql
-- Description: Add soft delete tombstone column to user_identities for BaseAuditEntity compliance

ALTER TABLE user_identities ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
