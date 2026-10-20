-- =============================================================================
-- Migration: Create Server Network, Server Members, Channels, Rules & Invites
-- Target: PostgreSQL / Supabase SQL Editor
-- Synchronizes Discord-grade server architecture and persistent membership tracking
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Servers Table
CREATE TABLE IF NOT EXISTS public.servers (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT 'Community',
    description TEXT NOT NULL DEFAULT '',
    owner_id TEXT NOT NULL DEFAULT '',
    owner_display_name TEXT NOT NULL DEFAULT 'Campus Founder',
    owner_handle TEXT NOT NULL DEFAULT '@founder',
    owner_avatar_url TEXT DEFAULT NULL,
    avatar_res_name TEXT NOT NULL DEFAULT 'server_codesistency',
    banner_res_name TEXT DEFAULT NULL,
    is_verified BOOLEAN NOT NULL DEFAULT false,
    member_count INTEGER NOT NULL DEFAULT 1,
    created_at_epoch_ms BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM now()) * 1000)::bigint,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_servers_owner_id ON public.servers(owner_id);
CREATE INDEX IF NOT EXISTS idx_servers_category ON public.servers(category);

ALTER TABLE public.servers ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public can view servers" ON public.servers;
CREATE POLICY "Public can view servers" ON public.servers FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public can insert servers" ON public.servers;
CREATE POLICY "Public can insert servers" ON public.servers FOR INSERT WITH CHECK (true);
DROP POLICY IF EXISTS "Public can update servers" ON public.servers;
CREATE POLICY "Public can update servers" ON public.servers FOR UPDATE USING (true);
DROP POLICY IF EXISTS "Public can delete servers" ON public.servers;
CREATE POLICY "Public can delete servers" ON public.servers FOR DELETE USING (true);

-- 2. Server Members Table (Tracks persistent server memberships)
CREATE TABLE IF NOT EXISTS public.server_members (
    id TEXT PRIMARY KEY,
    server_id TEXT NOT NULL REFERENCES public.servers(id) ON DELETE CASCADE,
    user_id TEXT NOT NULL,
    display_name TEXT NOT NULL DEFAULT 'Campus Student',
    handle TEXT NOT NULL DEFAULT '@student',
    role TEXT NOT NULL DEFAULT 'MEMBER',
    role_title TEXT NOT NULL DEFAULT 'Member',
    role_color_hex BIGINT NOT NULL DEFAULT 6055024,
    avatar_url TEXT DEFAULT NULL,
    avatar_res_name TEXT NOT NULL DEFAULT '',
    is_online BOOLEAN NOT NULL DEFAULT true,
    custom_status TEXT DEFAULT 'Campus Member',
    joined_at_epoch_ms BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM now()) * 1000)::bigint,
    badge TEXT DEFAULT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT uq_server_members_server_user UNIQUE (server_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_server_members_user ON public.server_members(user_id);
CREATE INDEX IF NOT EXISTS idx_server_members_server ON public.server_members(server_id);
CREATE INDEX IF NOT EXISTS idx_server_members_role ON public.server_members(role);

ALTER TABLE public.server_members ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public can view server members" ON public.server_members;
CREATE POLICY "Public can view server members" ON public.server_members FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public can insert server members" ON public.server_members;
CREATE POLICY "Public can insert server members" ON public.server_members FOR INSERT WITH CHECK (true);
DROP POLICY IF EXISTS "Public can update server members" ON public.server_members;
CREATE POLICY "Public can update server members" ON public.server_members FOR UPDATE USING (true);
DROP POLICY IF EXISTS "Public can delete server members" ON public.server_members;
CREATE POLICY "Public can delete server members" ON public.server_members FOR DELETE USING (true);

-- 3. Server Channels Table
CREATE TABLE IF NOT EXISTS public.server_channels (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    server_id TEXT NOT NULL REFERENCES public.servers(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    category_name TEXT NOT NULL DEFAULT 'TEXT CHANNELS',
    channel_type TEXT NOT NULL DEFAULT 'text',
    topic TEXT NOT NULL DEFAULT '',
    is_unread BOOLEAN NOT NULL DEFAULT false,
    order_index INTEGER NOT NULL DEFAULT 0,
    created_at_epoch_ms BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM now()) * 1000)::bigint,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_server_channels_server ON public.server_channels(server_id);
CREATE INDEX IF NOT EXISTS idx_server_channels_order ON public.server_channels(server_id, order_index ASC);

ALTER TABLE public.server_channels ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public can view server channels" ON public.server_channels;
CREATE POLICY "Public can view server channels" ON public.server_channels FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public can insert server channels" ON public.server_channels;
CREATE POLICY "Public can insert server channels" ON public.server_channels FOR INSERT WITH CHECK (true);
DROP POLICY IF EXISTS "Public can update server channels" ON public.server_channels;
CREATE POLICY "Public can update server channels" ON public.server_channels FOR UPDATE USING (true);
DROP POLICY IF EXISTS "Public can delete server channels" ON public.server_channels;
CREATE POLICY "Public can delete server channels" ON public.server_channels FOR DELETE USING (true);

-- 4. Server Rules Table
CREATE TABLE IF NOT EXISTS public.server_rules (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    server_id TEXT NOT NULL REFERENCES public.servers(id) ON DELETE CASCADE,
    rule_index INTEGER NOT NULL DEFAULT 1,
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_server_rules_server ON public.server_rules(server_id, rule_index ASC);

ALTER TABLE public.server_rules ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public can view server rules" ON public.server_rules;
CREATE POLICY "Public can view server rules" ON public.server_rules FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public can insert server rules" ON public.server_rules;
CREATE POLICY "Public can insert server rules" ON public.server_rules FOR INSERT WITH CHECK (true);
DROP POLICY IF EXISTS "Public can update server rules" ON public.server_rules;
CREATE POLICY "Public can update server rules" ON public.server_rules FOR UPDATE USING (true);
DROP POLICY IF EXISTS "Public can delete server rules" ON public.server_rules;
CREATE POLICY "Public can delete server rules" ON public.server_rules FOR DELETE USING (true);

-- 5. Server Invites Table
CREATE TABLE IF NOT EXISTS public.server_invites (
    code TEXT PRIMARY KEY,
    server_id TEXT NOT NULL REFERENCES public.servers(id) ON DELETE CASCADE,
    server_name TEXT NOT NULL DEFAULT '',
    inviter_id TEXT NOT NULL DEFAULT '',
    inviter_name TEXT NOT NULL DEFAULT 'Campus Member',
    expires_at_epoch_ms BIGINT NOT NULL DEFAULT 9223372036854775807,
    max_uses INTEGER NOT NULL DEFAULT 0,
    uses_count INTEGER NOT NULL DEFAULT 0,
    is_revoked BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_server_invites_server ON public.server_invites(server_id);

ALTER TABLE public.server_invites ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public can view server invites" ON public.server_invites;
CREATE POLICY "Public can view server invites" ON public.server_invites FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public can insert server invites" ON public.server_invites;
CREATE POLICY "Public can insert server invites" ON public.server_invites FOR INSERT WITH CHECK (true);
DROP POLICY IF EXISTS "Public can update server invites" ON public.server_invites;
CREATE POLICY "Public can update server invites" ON public.server_invites FOR UPDATE USING (true);

-- 6. Per-Server Profiles Table
CREATE TABLE IF NOT EXISTS public.per_server_profiles (
    id TEXT PRIMARY KEY,
    server_id TEXT NOT NULL REFERENCES public.servers(id) ON DELETE CASCADE,
    user_id TEXT NOT NULL,
    nickname TEXT NOT NULL DEFAULT '',
    tag TEXT NOT NULL DEFAULT 'MEMBER',
    tag_color_hex BIGINT NOT NULL DEFAULT 6055024,
    role_title TEXT NOT NULL DEFAULT 'Member',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT uq_per_server_profiles UNIQUE (server_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_per_server_profiles_lookup ON public.per_server_profiles(server_id, user_id);

ALTER TABLE public.per_server_profiles ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public can view per server profiles" ON public.per_server_profiles;
CREATE POLICY "Public can view per server profiles" ON public.per_server_profiles FOR SELECT USING (true);
DROP POLICY IF EXISTS "Public can insert per server profiles" ON public.per_server_profiles;
CREATE POLICY "Public can insert per server profiles" ON public.per_server_profiles FOR INSERT WITH CHECK (true);
DROP POLICY IF EXISTS "Public can update per server profiles" ON public.per_server_profiles;
CREATE POLICY "Public can update per server profiles" ON public.per_server_profiles FOR UPDATE USING (true);

-- 7. Realtime Publication Registration
DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE 
        public.servers,
        public.server_members,
        public.server_channels,
        public.server_rules,
        public.server_invites,
        public.per_server_profiles;
EXCEPTION
    WHEN OTHERS THEN
        NULL;
END $$;
