-- ==============================================================================
-- Migration: 20261020000003_enforce_idempotency_constraints.sql
-- Enforce unique constraints and conflict targets for idempotent writes
-- ==============================================================================

-- 1. Server Channels: Enforce unique channel name per server
CREATE UNIQUE INDEX IF NOT EXISTS idx_server_channels_server_name
    ON public.server_channels(server_id, name);

-- 2. Server Rules: Enforce unique rule index per server
CREATE UNIQUE INDEX IF NOT EXISTS idx_server_rules_server_index
    ON public.server_rules(server_id, rule_index);

-- 3. Server Members: Ensure composite unique constraint on (server_id, user_id)
CREATE UNIQUE INDEX IF NOT EXISTS idx_server_members_unique_membership
    ON public.server_members(server_id, user_id);
