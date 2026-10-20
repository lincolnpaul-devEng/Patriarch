-- =============================================================================
-- Migration: Create Missing Campus Network Domain Entities in Supabase
-- Tables:
--   - campus_flash_meets
--   - campus_market_items
--   - campus_micro_gigs
--   - campus_live_streams
--   - campus_streamer_profiles
--   - campus_stream_comments
--   - campus_academic_channels
--   - campus_chat_messages
--   - campus_friends
--   - campus_places
--   - campus_events
-- Includes: Indexes, RLS Policies, and Realtime Publications.
-- =============================================================================

-- 1. Campus Flash Meets
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

-- 2. Campus Market Items
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

-- 3. Campus Micro Gigs
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

-- 4. Campus Live Streams
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

-- 5. Campus Streamer Profiles
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

-- 6. Campus Stream Comments
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

-- 7. Campus Academic Channels
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

-- 8. Campus Chat Messages
CREATE TABLE IF NOT EXISTS public.campus_chat_messages (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    channel_id TEXT NOT NULL,
    sender_id TEXT NOT NULL,
    sender_name TEXT NOT NULL,
    text TEXT NOT NULL DEFAULT '',
    timestamp_epoch_ms BIGINT NOT NULL,
    is_optimistic BOOLEAN NOT NULL DEFAULT false,
    settlement_payload_json JSONB DEFAULT NULL,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_campus_chat_messages_channel ON public.campus_chat_messages(channel_id, timestamp_epoch_ms ASC);

ALTER TABLE public.campus_chat_messages ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Public can view chat messages" ON public.campus_chat_messages;
CREATE POLICY "Public can view chat messages" ON public.campus_chat_messages FOR SELECT USING (true);
DROP POLICY IF EXISTS "Authenticated can insert chat messages" ON public.campus_chat_messages;
CREATE POLICY "Authenticated can insert chat messages" ON public.campus_chat_messages FOR INSERT TO authenticated WITH CHECK (true);

-- 9. Campus Friends
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

-- 10. Campus Safe Places & Hubs
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

-- 11. Campus Events
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
