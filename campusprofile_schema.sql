-- =============================================================================
-- Migration: Complete Patriarch Campus Network Database Schema
-- Table Name: `campusprofile` (explicitly requested)
-- Additional Tables: `campus_rendezvous_points`, `campus_saved_listings`, `campus_safe_trades`
-- Target: PostgreSQL / Supabase SQL Editor
-- Includes: Merit points, trust scores, avatars, behavioral metrics, safe trade
--           rendezvous points, saved listings & watchlists, storage quotas, and RLS.
-- =============================================================================

-- 1. Ensure required extensions are available
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =============================================================================
-- 2. Core `campusprofile` Table
-- =============================================================================
CREATE TABLE IF NOT EXISTS public.campusprofile (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
    student_id TEXT UNIQUE,
    full_name TEXT NOT NULL DEFAULT '',
    university_email TEXT NOT NULL DEFAULT '',
    university_name TEXT NOT NULL DEFAULT '',
    degree_program TEXT NOT NULL DEFAULT '',
    academic_year TEXT NOT NULL DEFAULT '',
    consistency_streak_days INTEGER NOT NULL DEFAULT 0,
    behavioral_trust_score INTEGER NOT NULL DEFAULT 50 CHECK (behavioral_trust_score >= 0 AND behavioral_trust_score <= 100),
    campus_merit_points INTEGER NOT NULL DEFAULT 0 CHECK (campus_merit_points >= 0),
    persona_role TEXT NOT NULL DEFAULT 'Undergraduate Student',
    status_note TEXT DEFAULT NULL,
    status_note_emoji TEXT DEFAULT NULL,
    status_note_expiry TIMESTAMPTZ DEFAULT NULL,
    bio TEXT NOT NULL DEFAULT '',
    avatar_url TEXT DEFAULT NULL,
    campus_residence TEXT NOT NULL DEFAULT '',
    phone_number TEXT NOT NULL DEFAULT '',
    interests JSONB NOT NULL DEFAULT '[]'::jsonb,
    verified_skill_badges JSONB NOT NULL DEFAULT '[]'::jsonb,
    private_quest_deck JSONB NOT NULL DEFAULT '[]'::jsonb,
    alternate_profiles JSONB NOT NULL DEFAULT '[]'::jsonb,
    onboarding_preferences JSONB NOT NULL DEFAULT '{}'::jsonb,
    saved_listing_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    watchlist_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    preferred_rendezvous_point TEXT NOT NULL DEFAULT '',
    safe_trade_settings JSONB NOT NULL DEFAULT '{"auto_escrow": true, "require_id_badge": true, "strict_rendezvous": true}'::jsonb,
    behavioral_metrics JSONB NOT NULL DEFAULT '{"dispute_free_rate": 100, "punctuality_rate": 100, "academic_endorsements": 0, "peer_review_avg": 5.0, "total_trades": 0}'::jsonb,
    storage_used_bytes BIGINT NOT NULL DEFAULT 0,
    storage_quota_bytes BIGINT NOT NULL DEFAULT 1073741824, -- 1 GB standard quota
    notifications_enabled BOOLEAN NOT NULL DEFAULT true,
    escrow_alerts_enabled BOOLEAN NOT NULL DEFAULT true,
    quest_reminders_enabled BOOLEAN NOT NULL DEFAULT true,
    is_trust_score_public BOOLEAN NOT NULL DEFAULT true,
    is_online_status_visible BOOLEAN NOT NULL DEFAULT true,
    activity_ledger_visibility TEXT NOT NULL DEFAULT 'Public to Campus',
    location_privacy_scope TEXT NOT NULL DEFAULT 'VERIFIED_CAMPUS_PEERS',
    wallet_balance NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    vault_balance NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    is_verified_student BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- Documentation comments
COMMENT ON TABLE public.campusprofile IS 'Primary student profiles, merit points, trust scores, academic verification tags, and onboarding settings for Patriarch Campus Network';
COMMENT ON COLUMN public.campusprofile.user_id IS 'Foreign key reference to Supabase auth.users';
COMMENT ON COLUMN public.campusprofile.student_id IS 'Unique university issued identification number / registration tag';
COMMENT ON COLUMN public.campusprofile.campus_merit_points IS 'Earned campus merit points for peer engagement, study room check-ins, and marketplace activity';
COMMENT ON COLUMN public.campusprofile.behavioral_trust_score IS 'Real-time peer reputation score between 0 and 100 derived from transaction history';
COMMENT ON COLUMN public.campusprofile.saved_listing_ids IS 'Array of saved marketplace listing UUIDs in user watchlist';
COMMENT ON COLUMN public.campusprofile.preferred_rendezvous_point IS 'Default campus safe meeting zone for peer commerce exchanges';

-- High-performance lookup indexes for campusprofile
CREATE INDEX IF NOT EXISTS idx_campusprofile_user_id ON public.campusprofile(user_id);
CREATE INDEX IF NOT EXISTS idx_campusprofile_student_id ON public.campusprofile(student_id);
CREATE INDEX IF NOT EXISTS idx_campusprofile_email ON public.campusprofile(university_email);
CREATE INDEX IF NOT EXISTS idx_campusprofile_university ON public.campusprofile(university_name);
CREATE INDEX IF NOT EXISTS idx_campusprofile_trust_score ON public.campusprofile(behavioral_trust_score DESC);
CREATE INDEX IF NOT EXISTS idx_campusprofile_merit_points ON public.campusprofile(campus_merit_points DESC);
CREATE INDEX IF NOT EXISTS idx_campusprofile_created_at ON public.campusprofile(created_at DESC);

-- Row Level Security (RLS) on campusprofile
ALTER TABLE public.campusprofile ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public profiles are viewable by authenticated users" ON public.campusprofile;
DROP POLICY IF EXISTS "Users can insert their own profile" ON public.campusprofile;
DROP POLICY IF EXISTS "Users can update their own profile" ON public.campusprofile;
DROP POLICY IF EXISTS "Users can delete their own profile" ON public.campusprofile;

CREATE POLICY "Public profiles are viewable by authenticated users"
    ON public.campusprofile FOR SELECT TO authenticated USING (true);

CREATE POLICY "Users can insert their own profile"
    ON public.campusprofile FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own profile"
    ON public.campusprofile FOR UPDATE TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete their own profile"
    ON public.campusprofile FOR DELETE TO authenticated USING (auth.uid() = user_id);

-- Automatic `updated_at` timestamp trigger
CREATE OR REPLACE FUNCTION public.handle_campusprofile_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = timezone('utc'::text, now());
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trigger_campusprofile_updated_at ON public.campusprofile;
CREATE TRIGGER trigger_campusprofile_updated_at
    BEFORE UPDATE ON public.campusprofile
    FOR EACH ROW EXECUTE FUNCTION public.handle_campusprofile_updated_at();

-- Automatic Profile Provisioning on User Signup
CREATE OR REPLACE FUNCTION public.handle_new_auth_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.campusprofile (
        user_id,
        full_name,
        university_email,
        avatar_url,
        is_verified_student,
        campus_merit_points,
        behavioral_trust_score
    ) VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data->>'full_name', NEW.raw_user_meta_data->>'name', ''),
        COALESCE(NEW.email, ''),
        COALESCE(NEW.raw_user_meta_data->>'avatar_url', NEW.raw_user_meta_data->>'picture', NULL),
        COALESCE(NEW.email LIKE '%.edu' OR NEW.email LIKE '%.ac.%', false),
        100, -- Initial starter merit points bonus
        50   -- Baseline trust score
    )
    ON CONFLICT (user_id) DO NOTHING;
    RETURN NEW;
EXCEPTION
    WHEN OTHERS THEN
        RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_auth_user();

-- =============================================================================
-- 3. Campus Rendezvous Points Table (SafeTrade Verified Meeting Zones)
-- =============================================================================
CREATE TABLE IF NOT EXISTS public.campus_rendezvous_points (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    university_name TEXT NOT NULL,
    spot_name TEXT NOT NULL,
    description TEXT NOT NULL,
    security_level TEXT NOT NULL,
    latitude DOUBLE PRECISION DEFAULT NULL,
    longitude DOUBLE PRECISION DEFAULT NULL,
    operating_hours TEXT NOT NULL DEFAULT '24/7 Mon-Sun',
    has_cctv BOOLEAN NOT NULL DEFAULT true,
    has_security_desk BOOLEAN NOT NULL DEFAULT true,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_rendezvous_university ON public.campus_rendezvous_points(university_name);

ALTER TABLE public.campus_rendezvous_points ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Rendezvous spots viewable by all authenticated users" ON public.campus_rendezvous_points;
CREATE POLICY "Rendezvous spots viewable by all authenticated users"
    ON public.campus_rendezvous_points FOR SELECT TO authenticated USING (is_active = true);

-- Seed Initial Verified Rendezvous Zones
INSERT INTO public.campus_rendezvous_points (university_name, spot_name, description, security_level, has_cctv, has_security_desk)
VALUES 
    ('Strathmore University', 'Library Ground Floor Lockers', 'Safe exchange lockers with 24/7 CCTV surveillance and security presence.', 'CCTV Monitored Safe Zone', true, true),
    ('Strathmore University', 'Student Guild Center Foyer', 'Open cafeteria and student union commerce hub near campus security.', 'Campus Guard Station Adjacent', true, true),
    ('Strathmore University', 'Phase 1 Science & Computing Labs Entrance', 'Foyer with electronic card access and security receptionist.', 'Biometric / Student ID Gated', true, true),
    ('Strathmore University', 'Main Gate Visitors Lounge', 'Publicly accessible entry pavilion monitored by university security team.', '24/7 Manned Security Post', true, true)
ON CONFLICT DO NOTHING;

-- =============================================================================
-- 4. Saved Listings & Watchlist Table
-- =============================================================================
CREATE TABLE IF NOT EXISTS public.campus_saved_listings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    listing_id TEXT NOT NULL,
    listing_title TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT 'Marketplace',
    price_display TEXT NOT NULL,
    seller_name TEXT NOT NULL,
    seller_trust_score INTEGER NOT NULL DEFAULT 50,
    image_url TEXT DEFAULT NULL,
    is_available BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE(user_id, listing_id)
);

CREATE INDEX IF NOT EXISTS idx_saved_listings_user ON public.campus_saved_listings(user_id);
CREATE INDEX IF NOT EXISTS idx_saved_listings_created_at ON public.campus_saved_listings(created_at DESC);

ALTER TABLE public.campus_saved_listings ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Users can view their own saved listings" ON public.campus_saved_listings;
DROP POLICY IF EXISTS "Users can insert into their own saved listings" ON public.campus_saved_listings;
DROP POLICY IF EXISTS "Users can delete their own saved listings" ON public.campus_saved_listings;

CREATE POLICY "Users can view their own saved listings"
    ON public.campus_saved_listings FOR SELECT TO authenticated USING (auth.uid() = user_id);

CREATE POLICY "Users can insert into their own saved listings"
    ON public.campus_saved_listings FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete their own saved listings"
    ON public.campus_saved_listings FOR DELETE TO authenticated USING (auth.uid() = user_id);

-- =============================================================================
-- 5. SafeTrade Escrow Orders Table
-- =============================================================================
CREATE TABLE IF NOT EXISTS public.campus_safe_trades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number TEXT UNIQUE NOT NULL,
    buyer_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    seller_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    item_title TEXT NOT NULL,
    price_amount NUMERIC(12, 2) NOT NULL,
    currency_code TEXT NOT NULL DEFAULT 'KES',
    rendezvous_spot TEXT NOT NULL,
    rendezvous_time TIMESTAMPTZ,
    status TEXT NOT NULL DEFAULT 'SAFE_PAY_ACTIVE', -- PENDING, SAFE_PAY_ACTIVE, COMPLETED, DISPUTED, CANCELLED
    buyer_confirmed BOOLEAN NOT NULL DEFAULT false,
    seller_confirmed BOOLEAN NOT NULL DEFAULT false,
    escrow_held_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_safe_trades_buyer ON public.campus_safe_trades(buyer_id);
CREATE INDEX IF NOT EXISTS idx_safe_trades_seller ON public.campus_safe_trades(seller_id);
CREATE INDEX IF NOT EXISTS idx_safe_trades_status ON public.campus_safe_trades(status);

ALTER TABLE public.campus_safe_trades ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Users can view trades they participate in" ON public.campus_safe_trades;
DROP POLICY IF EXISTS "Users can update trades they participate in" ON public.campus_safe_trades;

CREATE POLICY "Users can view trades they participate in"
    ON public.campus_safe_trades FOR SELECT TO authenticated
    USING (auth.uid() = buyer_id OR auth.uid() = seller_id);

CREATE POLICY "Users can update trades they participate in"
    ON public.campus_safe_trades FOR UPDATE TO authenticated
    USING (auth.uid() = buyer_id OR auth.uid() = seller_id)
    WITH CHECK (auth.uid() = buyer_id OR auth.uid() = seller_id);

-- =============================================================================
-- 6. Campus Servers (Guilds/Communities) Table & Supporting Schemas
-- =============================================================================
CREATE TABLE IF NOT EXISTS public.campus_servers (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    category TEXT NOT NULL DEFAULT 'Community',
    owner_id TEXT NOT NULL DEFAULT 'user_student',
    avatar_res_name TEXT NOT NULL DEFAULT 'server_codesistency',
    banner_res_name TEXT DEFAULT NULL,
    is_verified BOOLEAN NOT NULL DEFAULT false,
    member_count INTEGER NOT NULL DEFAULT 1,
    invite_code TEXT UNIQUE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    created_at_epoch_ms BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint
);

CREATE INDEX IF NOT EXISTS idx_campus_servers_category ON public.campus_servers(category);
CREATE INDEX IF NOT EXISTS idx_campus_servers_invite_code ON public.campus_servers(invite_code);

-- Public view alias as requested
CREATE OR REPLACE VIEW public.campuserver AS SELECT * FROM public.campus_servers;

CREATE TABLE IF NOT EXISTS public.campus_server_channels (
    id TEXT PRIMARY KEY,
    server_id TEXT NOT NULL REFERENCES public.campus_servers(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    category_name TEXT NOT NULL DEFAULT 'Text Channels',
    channel_type TEXT NOT NULL DEFAULT 'text',
    topic TEXT NOT NULL DEFAULT '',
    unread_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_campus_channels_server ON public.campus_server_channels(server_id);

CREATE TABLE IF NOT EXISTS public.campus_server_members (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    server_id TEXT NOT NULL REFERENCES public.campus_servers(id) ON DELETE CASCADE,
    display_name TEXT NOT NULL,
    handle TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'MEMBER',
    role_title TEXT NOT NULL DEFAULT 'Member',
    role_color_hex BIGINT NOT NULL DEFAULT 6055024,
    avatar_url TEXT DEFAULT NULL,
    avatar_res_name TEXT NOT NULL DEFAULT 'avatar_lincoln',
    is_online BOOLEAN NOT NULL DEFAULT false,
    custom_status TEXT DEFAULT NULL,
    joined_at_epoch_ms BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    badge TEXT DEFAULT NULL,
    UNIQUE(server_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_campus_members_server ON public.campus_server_members(server_id);
CREATE INDEX IF NOT EXISTS idx_campus_members_user ON public.campus_server_members(user_id);

CREATE TABLE IF NOT EXISTS public.campus_server_rules (
    id TEXT PRIMARY KEY,
    server_id TEXT NOT NULL REFERENCES public.campus_servers(id) ON DELETE CASCADE,
    rule_number INTEGER NOT NULL DEFAULT 1,
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    rule_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_campus_rules_server ON public.campus_server_rules(server_id);

CREATE TABLE IF NOT EXISTS public.campus_server_invites (
    invite_code TEXT PRIMARY KEY,
    server_id TEXT NOT NULL REFERENCES public.campus_servers(id) ON DELETE CASCADE,
    creator_user_id TEXT NOT NULL,
    uses_count INTEGER NOT NULL DEFAULT 0,
    max_uses INTEGER NOT NULL DEFAULT 0,
    expires_at_epoch_ms BIGINT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true
);

-- RLS Policies for Servers & Communities
ALTER TABLE public.campus_servers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.campus_server_channels ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.campus_server_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.campus_server_rules ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.campus_server_invites ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public can view servers" ON public.campus_servers;
DROP POLICY IF EXISTS "Authenticated users can create servers" ON public.campus_servers;
DROP POLICY IF EXISTS "Public can view server channels" ON public.campus_server_channels;
DROP POLICY IF EXISTS "Authenticated users can create server channels" ON public.campus_server_channels;
DROP POLICY IF EXISTS "Public can view server members" ON public.campus_server_members;
DROP POLICY IF EXISTS "Public can view server rules" ON public.campus_server_rules;
DROP POLICY IF EXISTS "Public can view server invites" ON public.campus_server_invites;

CREATE POLICY "Public can view servers" ON public.campus_servers FOR SELECT USING (true);
CREATE POLICY "Authenticated users can create servers" ON public.campus_servers FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "Public can view server channels" ON public.campus_server_channels FOR SELECT USING (true);
CREATE POLICY "Authenticated users can create server channels" ON public.campus_server_channels FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "Public can view server members" ON public.campus_server_members FOR SELECT USING (true);
CREATE POLICY "Public can view server rules" ON public.campus_server_rules FOR SELECT USING (true);
CREATE POLICY "Public can view server invites" ON public.campus_server_invites FOR SELECT USING (true);

-- =============================================================================
-- 7. Enable Supabase Realtime for instant synchronization across devices
-- =============================================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' 
        AND schemaname = 'public' 
        AND tablename = 'campusprofile'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.campusprofile;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' 
        AND schemaname = 'public' 
        AND tablename = 'campus_saved_listings'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.campus_saved_listings;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' 
        AND schemaname = 'public' 
        AND tablename = 'campus_safe_trades'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.campus_safe_trades;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' 
        AND schemaname = 'public' 
        AND tablename = 'campus_servers'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.campus_servers;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' 
        AND schemaname = 'public' 
        AND tablename = 'campus_server_channels'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.campus_server_channels;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' 
        AND schemaname = 'public' 
        AND tablename = 'campus_server_members'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.campus_server_members;
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        NULL;
END $$;

-- =============================================================================
-- 8. Additional Campus Network Domain Tables (Flash Meets, Market Items,
--    Micro Gigs, Live Streams, Academic Channels, Friends, Places & Events)
-- =============================================================================

-- Campus Flash Meets
CREATE TABLE IF NOT EXISTS public.campus_flash_meets (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    category_name TEXT NOT NULL DEFAULT 'STUDY_JAM',
    latitude DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    longitude DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    location_name TEXT NOT NULL DEFAULT '',
    creator_name TEXT NOT NULL DEFAULT '',
    creator_avatar TEXT DEFAULT NULL,
    created_at_epoch_ms BIGINT NOT NULL,
    expires_at_epoch_ms BIGINT NOT NULL,
    attendee_count INTEGER NOT NULL DEFAULT 1,
    max_attendees INTEGER NOT NULL DEFAULT 10,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_campus_flash_meets_expires ON public.campus_flash_meets(expires_at_epoch_ms DESC);
CREATE INDEX IF NOT EXISTS idx_campus_flash_meets_category ON public.campus_flash_meets(category_name);

ALTER TABLE public.campus_flash_meets ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public can view flash meets" ON public.campus_flash_meets;
CREATE POLICY "Public can view flash meets" ON public.campus_flash_meets FOR SELECT USING (true);
DROP POLICY IF EXISTS "Authenticated can insert flash meets" ON public.campus_flash_meets;
CREATE POLICY "Authenticated can insert flash meets" ON public.campus_flash_meets FOR INSERT TO authenticated WITH CHECK (true);
DROP POLICY IF EXISTS "Authenticated can update flash meets" ON public.campus_flash_meets;
CREATE POLICY "Authenticated can update flash meets" ON public.campus_flash_meets FOR UPDATE TO authenticated USING (true);

-- Campus Market Items
CREATE TABLE IF NOT EXISTS public.campus_market_items (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    title TEXT NOT NULL,
    price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    currency TEXT NOT NULL DEFAULT 'KES',
    category TEXT NOT NULL DEFAULT 'General',
    condition TEXT NOT NULL DEFAULT 'Good',
    seller_name TEXT NOT NULL DEFAULT '',
    seller_rating REAL NOT NULL DEFAULT 5.0,
    is_campus_verified_seller BOOLEAN NOT NULL DEFAULT false,
    campus_hall_location TEXT NOT NULL DEFAULT '',
    image_url TEXT DEFAULT NULL,
    timestamp_ms BIGINT NOT NULL DEFAULT 0,
    description TEXT NOT NULL DEFAULT '',
    pickup_spot_name TEXT NOT NULL DEFAULT 'Quad Library Ground Floor Lockers',
    pickup_security_level TEXT NOT NULL DEFAULT 'CCTV Monitored Safe Zone',
    pickup_latitude DOUBLE PRECISION NOT NULL DEFAULT -1.3090,
    pickup_longitude DOUBLE PRECISION NOT NULL DEFAULT 36.8120,
    pickup_timing TEXT NOT NULL DEFAULT 'Immediate (within 1 hr)',
    payout_method TEXT NOT NULL DEFAULT 'Campus SafePay Escrow',
    requires_qr_release_code BOOLEAN NOT NULL DEFAULT true,
    qr_release_code TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_campus_market_items_category ON public.campus_market_items(category);
CREATE INDEX IF NOT EXISTS idx_campus_market_items_price ON public.campus_market_items(price);

ALTER TABLE public.campus_market_items ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public can view market items" ON public.campus_market_items;
CREATE POLICY "Public can view market items" ON public.campus_market_items FOR SELECT USING (true);
DROP POLICY IF EXISTS "Authenticated can insert market items" ON public.campus_market_items;
CREATE POLICY "Authenticated can insert market items" ON public.campus_market_items FOR INSERT TO authenticated WITH CHECK (true);
DROP POLICY IF EXISTS "Authenticated can update market items" ON public.campus_market_items;
CREATE POLICY "Authenticated can update market items" ON public.campus_market_items FOR UPDATE TO authenticated USING (true);

-- Campus Micro Gigs
CREATE TABLE IF NOT EXISTS public.campus_micro_gigs (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    payout_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    currency TEXT NOT NULL DEFAULT 'KES',
    estimated_duration TEXT NOT NULL DEFAULT '',
    poster_name TEXT NOT NULL DEFAULT '',
    poster_trust_score INTEGER NOT NULL DEFAULT 50,
    tag TEXT NOT NULL DEFAULT '',
    urgency_name TEXT NOT NULL DEFAULT 'NORMAL',
    spots_available INTEGER NOT NULL DEFAULT 1,
    timestamp_ms BIGINT NOT NULL DEFAULT 0,
    banner_url TEXT DEFAULT NULL,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_campus_micro_gigs_urgency ON public.campus_micro_gigs(urgency_name);

ALTER TABLE public.campus_micro_gigs ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public can view micro gigs" ON public.campus_micro_gigs;
CREATE POLICY "Public can view micro gigs" ON public.campus_micro_gigs FOR SELECT USING (true);
DROP POLICY IF EXISTS "Authenticated can insert micro gigs" ON public.campus_micro_gigs;
CREATE POLICY "Authenticated can insert micro gigs" ON public.campus_micro_gigs FOR INSERT TO authenticated WITH CHECK (true);
DROP POLICY IF EXISTS "Authenticated can update micro gigs" ON public.campus_micro_gigs;
CREATE POLICY "Authenticated can update micro gigs" ON public.campus_micro_gigs FOR UPDATE TO authenticated USING (true);

-- Campus Live Streams
CREATE TABLE IF NOT EXISTS public.campus_live_streams (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    broadcaster_id TEXT NOT NULL,
    broadcaster_name TEXT NOT NULL,
    broadcaster_handle TEXT NOT NULL DEFAULT '',
    broadcaster_avatar_res INTEGER NOT NULL DEFAULT 0,
    broadcaster_avatar_url TEXT DEFAULT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    category TEXT NOT NULL DEFAULT 'General',
    audience TEXT NOT NULL DEFAULT 'CAMPUS',
    is_live BOOLEAN NOT NULL DEFAULT true,
    stream_playback_url TEXT DEFAULT NULL,
    backdrop_image_res INTEGER NOT NULL DEFAULT 0,
    backdrop_image_url TEXT DEFAULT NULL,
    viewer_count INTEGER NOT NULL DEFAULT 0,
    peak_viewers INTEGER NOT NULL DEFAULT 0,
    like_count INTEGER NOT NULL DEFAULT 0,
    started_at_epoch_ms BIGINT NOT NULL DEFAULT 0,
    ended_at_epoch_ms BIGINT DEFAULT NULL,
    duration_seconds INTEGER NOT NULL DEFAULT 0,
    server_id TEXT DEFAULT NULL,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_campus_live_streams_is_live ON public.campus_live_streams(is_live);
CREATE INDEX IF NOT EXISTS idx_campus_live_streams_broadcaster ON public.campus_live_streams(broadcaster_id);

ALTER TABLE public.campus_live_streams ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public can view live streams" ON public.campus_live_streams;
CREATE POLICY "Public can view live streams" ON public.campus_live_streams FOR SELECT USING (true);
DROP POLICY IF EXISTS "Authenticated can insert live streams" ON public.campus_live_streams;
CREATE POLICY "Authenticated can insert live streams" ON public.campus_live_streams FOR INSERT TO authenticated WITH CHECK (true);
DROP POLICY IF EXISTS "Authenticated can update live streams" ON public.campus_live_streams;
CREATE POLICY "Authenticated can update live streams" ON public.campus_live_streams FOR UPDATE TO authenticated USING (true);

-- Campus Streamer Profiles
CREATE TABLE IF NOT EXISTS public.campus_streamer_profiles (
    student_id TEXT PRIMARY KEY,
    location TEXT NOT NULL DEFAULT '',
    joined_date TEXT NOT NULL DEFAULT '',
    hours_live INTEGER NOT NULL DEFAULT 0,
    followers_count TEXT NOT NULL DEFAULT '0',
    best_day TEXT NOT NULL DEFAULT '',
    streams_per_week INTEGER NOT NULL DEFAULT 0,
    top_topics_csv TEXT NOT NULL DEFAULT '',
    weekly_activity_csv TEXT NOT NULL DEFAULT '',
    updated_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.campus_streamer_profiles ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public can view streamer profiles" ON public.campus_streamer_profiles;
CREATE POLICY "Public can view streamer profiles" ON public.campus_streamer_profiles FOR SELECT USING (true);
DROP POLICY IF EXISTS "Authenticated can upsert streamer profiles" ON public.campus_streamer_profiles;
CREATE POLICY "Authenticated can upsert streamer profiles" ON public.campus_streamer_profiles FOR INSERT TO authenticated WITH CHECK (true);
CREATE POLICY "Authenticated can update streamer profiles" ON public.campus_streamer_profiles FOR UPDATE TO authenticated USING (true);

-- Campus Stream Comments
CREATE TABLE IF NOT EXISTS public.campus_stream_comments (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    stream_id TEXT NOT NULL,
    sender_id TEXT NOT NULL,
    sender_name TEXT NOT NULL,
    sender_avatar_url TEXT DEFAULT NULL,
    sender_avatar_res INTEGER NOT NULL DEFAULT 0,
    comment_text TEXT NOT NULL,
    timestamp_epoch_ms BIGINT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_campus_stream_comments_stream ON public.campus_stream_comments(stream_id);

ALTER TABLE public.campus_stream_comments ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public can view stream comments" ON public.campus_stream_comments;
CREATE POLICY "Public can view stream comments" ON public.campus_stream_comments FOR SELECT USING (true);
DROP POLICY IF EXISTS "Authenticated can insert stream comments" ON public.campus_stream_comments;
CREATE POLICY "Authenticated can insert stream comments" ON public.campus_stream_comments FOR INSERT TO authenticated WITH CHECK (true);

-- Campus Academic Channels
CREATE TABLE IF NOT EXISTS public.campus_academic_channels (
    id TEXT PRIMARY KEY,
    course_code TEXT NOT NULL,
    course_title TEXT NOT NULL,
    faculty TEXT NOT NULL DEFAULT '',
    department TEXT NOT NULL DEFAULT '',
    unread_count INTEGER NOT NULL DEFAULT 0,
    last_message_snippet TEXT NOT NULL DEFAULT '',
    last_message_time TEXT NOT NULL DEFAULT '',
    active_students_count INTEGER NOT NULL DEFAULT 0,
    is_pinned BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.campus_academic_channels ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public can view academic channels" ON public.campus_academic_channels;
CREATE POLICY "Public can view academic channels" ON public.campus_academic_channels FOR SELECT USING (true);
DROP POLICY IF EXISTS "Authenticated can manage academic channels" ON public.campus_academic_channels;
CREATE POLICY "Authenticated can manage academic channels" ON public.campus_academic_channels FOR ALL TO authenticated USING (true);

-- Campus Chat Messages
CREATE TABLE IF NOT EXISTS public.campus_chat_messages (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    channel_id TEXT NOT NULL,
    sender_id TEXT NOT NULL,
    sender_name TEXT NOT NULL,
    text TEXT NOT NULL DEFAULT '',
    timestamp_epoch_ms BIGINT NOT NULL,
    is_optimistic BOOLEAN NOT NULL DEFAULT false,
    settlement_payload_json JSONB DEFAULT NULL,
    image_url TEXT DEFAULT NULL,
    gif_url TEXT DEFAULT NULL,
    reply_to_text TEXT DEFAULT NULL,
    reaction TEXT DEFAULT NULL,
    is_delivered BOOLEAN NOT NULL DEFAULT true,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_campus_chat_messages_channel ON public.campus_chat_messages(channel_id, timestamp_epoch_ms ASC);
CREATE INDEX IF NOT EXISTS idx_campus_chat_messages_sender ON public.campus_chat_messages(sender_id);

ALTER TABLE public.campus_chat_messages ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public can view chat messages" ON public.campus_chat_messages;
CREATE POLICY "Public can view chat messages" ON public.campus_chat_messages FOR SELECT USING (true);
DROP POLICY IF EXISTS "Authenticated can insert chat messages" ON public.campus_chat_messages;
DROP POLICY IF EXISTS "Users can insert chat messages" ON public.campus_chat_messages;
CREATE POLICY "Users can insert chat messages" ON public.campus_chat_messages FOR INSERT WITH CHECK (true);
DROP POLICY IF EXISTS "Authenticated can update chat messages" ON public.campus_chat_messages;
DROP POLICY IF EXISTS "Users can update chat messages" ON public.campus_chat_messages;
CREATE POLICY "Users can update chat messages" ON public.campus_chat_messages FOR UPDATE USING (true) WITH CHECK (true);
DROP POLICY IF EXISTS "Authenticated can delete chat messages" ON public.campus_chat_messages;
DROP POLICY IF EXISTS "Users can delete chat messages" ON public.campus_chat_messages;
CREATE POLICY "Users can delete chat messages" ON public.campus_chat_messages FOR DELETE USING (true);

-- Campus Friends
CREATE TABLE IF NOT EXISTS public.campus_friends (
    id TEXT PRIMARY KEY, -- "$userId:$friendUserId"
    user_id TEXT NOT NULL,
    friend_user_id TEXT NOT NULL,
    friend_display_name TEXT NOT NULL,
    friend_handle TEXT NOT NULL DEFAULT '',
    friend_avatar_res_name TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL DEFAULT 'NONE',
    last_interaction_epoch_ms BIGINT NOT NULL DEFAULT 0,
    dm_channel_id TEXT DEFAULT NULL,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_campus_friends_user_id ON public.campus_friends(user_id);
CREATE INDEX IF NOT EXISTS idx_campus_friends_friend_id ON public.campus_friends(friend_user_id);

ALTER TABLE public.campus_friends ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Users can view their friendships" ON public.campus_friends;
CREATE POLICY "Users can view their friendships" ON public.campus_friends FOR SELECT USING (true);
DROP POLICY IF EXISTS "Authenticated can manage friendships" ON public.campus_friends;
CREATE POLICY "Authenticated can manage friendships" ON public.campus_friends FOR ALL TO authenticated USING (true);

-- Campus Safe Places & Hubs
CREATE TABLE IF NOT EXISTS public.campus_places (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    subtitle TEXT NOT NULL DEFAULT '',
    address TEXT NOT NULL DEFAULT '',
    category TEXT NOT NULL DEFAULT 'COMMONS',
    campus_region TEXT NOT NULL DEFAULT 'Central Campus',
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    rating REAL NOT NULL DEFAULT 4.8,
    review_count INTEGER NOT NULL DEFAULT 0,
    is_open_now BOOLEAN NOT NULL DEFAULT true,
    opening_hours TEXT NOT NULL DEFAULT '24/7',
    is_verified_safe_spot BOOLEAN NOT NULL DEFAULT true,
    image_url TEXT DEFAULT NULL,
    features JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_campus_places_category ON public.campus_places(category);
CREATE INDEX IF NOT EXISTS idx_campus_places_region ON public.campus_places(campus_region);

ALTER TABLE public.campus_places ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public can view campus places" ON public.campus_places;
CREATE POLICY "Public can view campus places" ON public.campus_places FOR SELECT USING (true);

-- Campus Events
CREATE TABLE IF NOT EXISTS public.campus_events (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    location_name TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ DEFAULT NULL,
    category TEXT NOT NULL DEFAULT 'ACADEMIC',
    attendee_count INTEGER NOT NULL DEFAULT 0,
    banner_url TEXT DEFAULT NULL,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_campus_events_start ON public.campus_events(start_time ASC);

ALTER TABLE public.campus_events ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public can view campus events" ON public.campus_events;
CREATE POLICY "Public can view campus events" ON public.campus_events FOR SELECT USING (true);
DROP POLICY IF EXISTS "Authenticated can insert campus events" ON public.campus_events;
CREATE POLICY "Authenticated can insert campus events" ON public.campus_events FOR INSERT TO authenticated WITH CHECK (true);

-- Realtime Publication for new entities
DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE 
        public.campus_flash_meets,
        public.campus_market_items,
        public.campus_micro_gigs,
        public.campus_live_streams,
        public.campus_stream_comments,
        public.campus_chat_messages,
        public.campus_friends;
EXCEPTION
    WHEN OTHERS THEN
        NULL;
END $$;

-- =============================================================================
-- Server Network & Discord Architecture Entities
-- =============================================================================

-- Servers
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

-- Server Members (Tracks persistent membership and interactions)
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

-- Server Channels
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

-- Server Rules
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

-- Server Invites
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
DROP POLICY IF EXISTS "Public can delete server invites" ON public.server_invites;
CREATE POLICY "Public can delete server invites" ON public.server_invites FOR DELETE USING (true);

-- Per-Server Profiles
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
DROP POLICY IF EXISTS "Public can delete per server profiles" ON public.per_server_profiles;
CREATE POLICY "Public can delete per server profiles" ON public.per_server_profiles FOR DELETE USING (true);

-- Realtime Publication for Server Entities
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



