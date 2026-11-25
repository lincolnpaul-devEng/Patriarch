-- =============================================================================
-- Migration: Map Screen Production Readiness (Phases 1-4)
-- Adds:
--   1. Real-time Friend Live Location Sharing (campus_live_locations,
--      campus_location_share_grants, strict RLS, start/stop/redeem RPCs)
--   2. Flash Meet Attendance & Remote End (campus_flash_meet_attendees,
--      toggle_flash_meet_attendance, end_flash_meet)
--   3. Campus Events Extensions & RSVPs (campus_events columns,
--      campus_event_rsvps, toggle_campus_event_rsvp)
--   4. Campus Places Security, Bookmarks, Reviews & Seed Data
--   5. REPLICA IDENTITY FULL & Realtime publications
-- =============================================================================

-- ============================================================================
-- 1. LIVE LOCATION SHARING & EPHEMERAL SHARE GRANTS
-- ============================================================================

CREATE TABLE IF NOT EXISTS public.campus_live_locations (
    user_id TEXT PRIMARY KEY,
    display_name TEXT NOT NULL DEFAULT '',
    avatar_url TEXT DEFAULT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    accuracy_meters REAL DEFAULT NULL,
    heading REAL DEFAULT NULL,
    privacy_scope TEXT NOT NULL DEFAULT 'GHOST_MODE_HIDDEN'
        CHECK (privacy_scope IN (
            'ESCROW_COUNTERPARTY_ONLY',
            'VERIFIED_CAMPUS_PEERS',
            'TIMED_SESSION_30M',
            'GHOST_MODE_HIDDEN'
        )),
    is_sharing BOOLEAN NOT NULL DEFAULT false,
    share_token TEXT DEFAULT NULL,
    expires_at_epoch_ms BIGINT NOT NULL DEFAULT 0,
    updated_at_epoch_ms BIGINT NOT NULL DEFAULT ((extract(epoch from timezone('utc'::text, now())) * 1000)::bigint),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_campus_live_locations_active
    ON public.campus_live_locations(is_sharing, expires_at_epoch_ms DESC);

CREATE UNIQUE INDEX IF NOT EXISTS idx_campus_live_locations_token
    ON public.campus_live_locations(share_token)
    WHERE share_token IS NOT NULL;

CREATE TABLE IF NOT EXISTS public.campus_location_share_grants (
    owner_user_id TEXT NOT NULL,
    grantee_user_id TEXT NOT NULL,
    token TEXT NOT NULL,
    expires_at_epoch_ms BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    PRIMARY KEY (owner_user_id, grantee_user_id)
);

CREATE INDEX IF NOT EXISTS idx_campus_location_share_grants_grantee
    ON public.campus_location_share_grants(grantee_user_id, expires_at_epoch_ms DESC);

ALTER TABLE public.campus_live_locations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.campus_location_share_grants ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Owner inserts live location" ON public.campus_live_locations;
CREATE POLICY "Owner inserts live location"
    ON public.campus_live_locations FOR INSERT TO authenticated
    WITH CHECK (public.is_current_campus_user(user_id));

DROP POLICY IF EXISTS "Owner updates live location" ON public.campus_live_locations;
CREATE POLICY "Owner updates live location"
    ON public.campus_live_locations FOR UPDATE TO authenticated
    USING (public.is_current_campus_user(user_id))
    WITH CHECK (public.is_current_campus_user(user_id));

DROP POLICY IF EXISTS "Owner deletes live location" ON public.campus_live_locations;
CREATE POLICY "Owner deletes live location"
    ON public.campus_live_locations FOR DELETE TO authenticated
    USING (public.is_current_campus_user(user_id));

DROP POLICY IF EXISTS "Authorized viewers read live locations" ON public.campus_live_locations;
CREATE POLICY "Authorized viewers read live locations"
    ON public.campus_live_locations FOR SELECT TO authenticated
    USING (
        public.is_current_campus_user(user_id)
        OR (
            is_sharing = true
            AND privacy_scope <> 'GHOST_MODE_HIDDEN'
            AND expires_at_epoch_ms > ((extract(epoch from timezone('utc'::text, now())) * 1000)::bigint)
            AND (
                (
                    privacy_scope IN ('VERIFIED_CAMPUS_PEERS', 'TIMED_SESSION_30M')
                    AND EXISTS (
                        SELECT 1 FROM public.campus_friends f
                        WHERE f.status = 'FRIENDS'
                          AND (
                              (public.is_current_campus_user(f.user_id) AND lower(trim(f.friend_user_id)) = lower(trim(campus_live_locations.user_id)))
                              OR (public.is_current_campus_user(f.friend_user_id) AND lower(trim(f.user_id)) = lower(trim(campus_live_locations.user_id)))
                          )
                    )
                )
                OR (
                    privacy_scope = 'TIMED_SESSION_30M'
                    AND EXISTS (
                        SELECT 1 FROM public.campus_location_share_grants g
                        WHERE lower(trim(g.owner_user_id)) = lower(trim(campus_live_locations.user_id))
                          AND public.is_current_campus_user(g.grantee_user_id)
                          AND g.expires_at_epoch_ms > ((extract(epoch from timezone('utc'::text, now())) * 1000)::bigint)
                    )
                )
                OR (
                    privacy_scope = 'ESCROW_COUNTERPARTY_ONLY'
                    AND EXISTS (
                        SELECT 1 FROM public.campus_escrow_orders e
                        WHERE e.status = 'FUNDS_HELD'
                          AND (
                              (public.is_current_campus_user(e.buyer_user_id) AND lower(trim(e.seller_user_id)) = lower(trim(campus_live_locations.user_id)))
                              OR (public.is_current_campus_user(e.seller_user_id) AND lower(trim(e.buyer_user_id)) = lower(trim(campus_live_locations.user_id)))
                          )
                    )
                )
            )
        )
    );

DROP POLICY IF EXISTS "Share grant participants read" ON public.campus_location_share_grants;
CREATE POLICY "Share grant participants read"
    ON public.campus_location_share_grants FOR SELECT TO authenticated
    USING (
        public.is_current_campus_user(owner_user_id)
        OR public.is_current_campus_user(grantee_user_id)
    );

DROP POLICY IF EXISTS "Owner deletes share grants" ON public.campus_location_share_grants;
CREATE POLICY "Owner deletes share grants"
    ON public.campus_location_share_grants FOR DELETE TO authenticated
    USING (public.is_current_campus_user(owner_user_id));

-- Start or update live location sharing with a server-generated opaque token
CREATE OR REPLACE FUNCTION public.start_live_location_share(
    p_scope TEXT,
    p_ttl_minutes INTEGER,
    p_lat DOUBLE PRECISION,
    p_lng DOUBLE PRECISION,
    p_accuracy REAL DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_user_id TEXT;
    v_display_name TEXT := '';
    v_avatar_url TEXT := NULL;
    v_now_ms BIGINT := (extract(epoch from timezone('utc'::text, now())) * 1000)::bigint;
    v_ttl_mins INTEGER := LEAST(GREATEST(COALESCE(p_ttl_minutes, 30), 5), 480);
    v_expires_ms BIGINT;
    v_token TEXT := NULL;
    v_is_sharing BOOLEAN := true;
    v_scope TEXT := upper(trim(COALESCE(p_scope, 'VERIFIED_CAMPUS_PEERS')));
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Authentication is required' USING ERRCODE = '42501';
    END IF;

    IF v_scope NOT IN (
        'ESCROW_COUNTERPARTY_ONLY',
        'VERIFIED_CAMPUS_PEERS',
        'TIMED_SESSION_30M',
        'GHOST_MODE_HIDDEN'
    ) THEN
        RAISE EXCEPTION 'Invalid privacy scope: %', p_scope USING ERRCODE = '22023';
    END IF;

    SELECT
        COALESCE(NULLIF(BTRIM(p.student_id), ''), NULLIF(BTRIM(p.university_email), ''), v_uid::text),
        COALESCE(NULLIF(BTRIM(p.full_name), ''), ''),
        p.avatar_url
    INTO v_user_id, v_display_name, v_avatar_url
    FROM public.campusprofile p
    WHERE p.user_id = v_uid
    LIMIT 1;

    IF v_user_id IS NULL OR BTRIM(v_user_id) = '' THEN
        v_user_id := v_uid::text;
    END IF;

    IF v_scope = 'GHOST_MODE_HIDDEN' THEN
        v_is_sharing := false;
        v_expires_ms := 0;
        v_token := NULL;
        DELETE FROM public.campus_location_share_grants
        WHERE public.is_current_campus_user(owner_user_id);
    ELSE
        IF v_scope = 'TIMED_SESSION_30M' THEN
            v_ttl_mins := 30;
        END IF;
        v_expires_ms := v_now_ms + (v_ttl_mins::bigint * 60000::bigint);
        v_token := replace(pg_catalog.gen_random_uuid()::text, '-', '')
            || substr(replace(pg_catalog.gen_random_uuid()::text, '-', ''), 1, 8);
    END IF;

    INSERT INTO public.campus_live_locations (
        user_id, display_name, avatar_url, latitude, longitude, accuracy_meters,
        privacy_scope, is_sharing, share_token, expires_at_epoch_ms, updated_at_epoch_ms, updated_at
    )
    VALUES (
        v_user_id, v_display_name, v_avatar_url, p_lat, p_lng, p_accuracy,
        v_scope, v_is_sharing, v_token, v_expires_ms, v_now_ms, timezone('utc'::text, now())
    )
    ON CONFLICT (user_id) DO UPDATE SET
        display_name = EXCLUDED.display_name,
        avatar_url = EXCLUDED.avatar_url,
        latitude = EXCLUDED.latitude,
        longitude = EXCLUDED.longitude,
        accuracy_meters = EXCLUDED.accuracy_meters,
        privacy_scope = EXCLUDED.privacy_scope,
        is_sharing = EXCLUDED.is_sharing,
        share_token = EXCLUDED.share_token,
        expires_at_epoch_ms = EXCLUDED.expires_at_epoch_ms,
        updated_at_epoch_ms = EXCLUDED.updated_at_epoch_ms,
        updated_at = timezone('utc'::text, now());

    RETURN jsonb_build_object(
        'user_id', v_user_id,
        'share_token', v_token,
        'expires_at_epoch_ms', v_expires_ms,
        'privacy_scope', v_scope,
        'is_sharing', v_is_sharing
    );
END;
$$;

REVOKE ALL ON FUNCTION public.start_live_location_share(TEXT, INTEGER, DOUBLE PRECISION, DOUBLE PRECISION, REAL) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.start_live_location_share(TEXT, INTEGER, DOUBLE PRECISION, DOUBLE PRECISION, REAL) TO authenticated;

-- Immediately revoke all active live location sharing & grants
CREATE OR REPLACE FUNCTION public.stop_live_location_share()
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_now_ms BIGINT := (extract(epoch from timezone('utc'::text, now())) * 1000)::bigint;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Authentication is required' USING ERRCODE = '42501';
    END IF;

    UPDATE public.campus_live_locations
    SET
        is_sharing = false,
        privacy_scope = 'GHOST_MODE_HIDDEN',
        share_token = NULL,
        expires_at_epoch_ms = 0,
        updated_at_epoch_ms = v_now_ms,
        updated_at = timezone('utc'::text, now())
    WHERE public.is_current_campus_user(user_id);

    DELETE FROM public.campus_location_share_grants
    WHERE public.is_current_campus_user(owner_user_id);

    RETURN TRUE;
END;
$$;

REVOKE ALL ON FUNCTION public.stop_live_location_share() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.stop_live_location_share() TO authenticated;

-- Redeem an opaque share token into a grant so the caller can view the sharer live
CREATE OR REPLACE FUNCTION public.redeem_location_share_token(p_token TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_caller_id TEXT;
    v_now_ms BIGINT := (extract(epoch from timezone('utc'::text, now())) * 1000)::bigint;
    v_loc public.campus_live_locations%ROWTYPE;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Authentication is required' USING ERRCODE = '42501';
    END IF;

    SELECT COALESCE(NULLIF(BTRIM(p.student_id), ''), NULLIF(BTRIM(p.university_email), ''), v_uid::text)
    INTO v_caller_id
    FROM public.campusprofile p
    WHERE p.user_id = v_uid
    LIMIT 1;

    IF v_caller_id IS NULL OR BTRIM(v_caller_id) = '' THEN
        v_caller_id := v_uid::text;
    END IF;

    SELECT * INTO v_loc
    FROM public.campus_live_locations l
    WHERE l.share_token = BTRIM(COALESCE(p_token, ''))
      AND l.is_sharing = true
      AND l.privacy_scope <> 'GHOST_MODE_HIDDEN'
      AND l.expires_at_epoch_ms > v_now_ms
    LIMIT 1;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Location share link has expired or been revoked' USING ERRCODE = 'P0002';
    END IF;

    IF v_loc.privacy_scope = 'ESCROW_COUNTERPARTY_ONLY' AND NOT EXISTS (
        SELECT 1 FROM public.campus_escrow_orders e
        WHERE e.status = 'FUNDS_HELD'
          AND (
              (public.is_current_campus_user(e.buyer_user_id) AND lower(trim(e.seller_user_id)) = lower(trim(v_loc.user_id)))
              OR (public.is_current_campus_user(e.seller_user_id) AND lower(trim(e.buyer_user_id)) = lower(trim(v_loc.user_id)))
          )
    ) THEN
        RAISE EXCEPTION 'Not authorized for this escrow-only share' USING ERRCODE = '42501';
    END IF;

    IF lower(trim(v_caller_id)) <> lower(trim(v_loc.user_id)) THEN
        INSERT INTO public.campus_location_share_grants (owner_user_id, grantee_user_id, token, expires_at_epoch_ms)
        VALUES (v_loc.user_id, v_caller_id, v_loc.share_token, v_loc.expires_at_epoch_ms)
        ON CONFLICT (owner_user_id, grantee_user_id) DO UPDATE SET
            token = EXCLUDED.token,
            expires_at_epoch_ms = EXCLUDED.expires_at_epoch_ms;
    END IF;

    RETURN jsonb_build_object(
        'user_id', v_loc.user_id,
        'display_name', v_loc.display_name,
        'avatar_url', v_loc.avatar_url,
        'latitude', v_loc.latitude,
        'longitude', v_loc.longitude,
        'accuracy_meters', v_loc.accuracy_meters,
        'heading', v_loc.heading,
        'privacy_scope', v_loc.privacy_scope,
        'expires_at_epoch_ms', v_loc.expires_at_epoch_ms,
        'updated_at_epoch_ms', v_loc.updated_at_epoch_ms
    );
END;
$$;

REVOKE ALL ON FUNCTION public.redeem_location_share_token(TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.redeem_location_share_token(TEXT) TO authenticated;


-- ============================================================================
-- 2. FLASH MEET ATTENDANCE & REMOTE END
-- ============================================================================

CREATE TABLE IF NOT EXISTS public.campus_flash_meet_attendees (
    meet_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    joined_at_epoch_ms BIGINT NOT NULL DEFAULT ((extract(epoch from timezone('utc'::text, now())) * 1000)::bigint),
    PRIMARY KEY (meet_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_campus_flash_meet_attendees_user
    ON public.campus_flash_meet_attendees(user_id);

ALTER TABLE public.campus_flash_meet_attendees ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Students read meet attendees" ON public.campus_flash_meet_attendees;
CREATE POLICY "Students read meet attendees"
    ON public.campus_flash_meet_attendees FOR SELECT TO authenticated
    USING (true);

DROP POLICY IF EXISTS "Student inserts own meet attendance" ON public.campus_flash_meet_attendees;
CREATE POLICY "Student inserts own meet attendance"
    ON public.campus_flash_meet_attendees FOR INSERT TO authenticated
    WITH CHECK (public.is_current_campus_user(user_id));

DROP POLICY IF EXISTS "Student deletes own meet attendance" ON public.campus_flash_meet_attendees;
CREATE POLICY "Student deletes own meet attendance"
    ON public.campus_flash_meet_attendees FOR DELETE TO authenticated
    USING (public.is_current_campus_user(user_id));

CREATE OR REPLACE FUNCTION public.toggle_flash_meet_attendance(p_meet_id TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_user_id TEXT;
    v_meet public.campus_flash_meets%ROWTYPE;
    v_now_ms BIGINT := (extract(epoch from timezone('utc'::text, now())) * 1000)::bigint;
    v_existing BOOLEAN := false;
    v_is_joined BOOLEAN := false;
    v_new_count INTEGER;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Authentication is required' USING ERRCODE = '42501';
    END IF;

    SELECT COALESCE(NULLIF(BTRIM(p.student_id), ''), NULLIF(BTRIM(p.university_email), ''), v_uid::text)
    INTO v_user_id
    FROM public.campusprofile p
    WHERE p.user_id = v_uid
    LIMIT 1;

    IF v_user_id IS NULL OR BTRIM(v_user_id) = '' THEN
        v_user_id := v_uid::text;
    END IF;

    SELECT * INTO v_meet
    FROM public.campus_flash_meets m
    WHERE m.id = p_meet_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Flash meet not found' USING ERRCODE = 'P0002';
    END IF;

    SELECT EXISTS (
        SELECT 1 FROM public.campus_flash_meet_attendees a
        WHERE a.meet_id = p_meet_id AND public.is_current_campus_user(a.user_id)
    ) INTO v_existing;

    IF v_existing THEN
        DELETE FROM public.campus_flash_meet_attendees a
        WHERE a.meet_id = p_meet_id AND public.is_current_campus_user(a.user_id);
        v_is_joined := false;
    ELSE
        IF v_meet.expires_at_epoch_ms <= v_now_ms THEN
            RAISE EXCEPTION 'Flash meet has already ended' USING ERRCODE = '22023';
        END IF;
        IF v_meet.attendee_count >= v_meet.max_attendees THEN
            RAISE EXCEPTION 'Flash meet is already at max capacity' USING ERRCODE = '22023';
        END IF;
        INSERT INTO public.campus_flash_meet_attendees (meet_id, user_id, joined_at_epoch_ms)
        VALUES (p_meet_id, v_user_id, v_now_ms)
        ON CONFLICT (meet_id, user_id) DO NOTHING;
        v_is_joined := true;
    END IF;

    SELECT GREATEST(
        1,
        1 + (
            SELECT COUNT(*)::integer FROM public.campus_flash_meet_attendees a
            WHERE a.meet_id = p_meet_id
              AND lower(trim(a.user_id)) <> lower(trim(COALESCE(v_meet.creator_id, '')))
        )
    ) INTO v_new_count;

    UPDATE public.campus_flash_meets
    SET attendee_count = v_new_count
    WHERE id = p_meet_id;

    RETURN jsonb_build_object(
        'is_joined', v_is_joined,
        'attendee_count', v_new_count
    );
END;
$$;

REVOKE ALL ON FUNCTION public.toggle_flash_meet_attendance(TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.toggle_flash_meet_attendance(TEXT) TO authenticated;

CREATE OR REPLACE FUNCTION public.end_flash_meet(p_meet_id TEXT)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_creator_id TEXT;
    v_now_ms BIGINT := (extract(epoch from timezone('utc'::text, now())) * 1000)::bigint;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Authentication is required' USING ERRCODE = '42501';
    END IF;

    SELECT creator_id INTO v_creator_id
    FROM public.campus_flash_meets
    WHERE id = p_meet_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RETURN FALSE;
    END IF;

    IF NOT public.is_current_campus_user(COALESCE(v_creator_id, '')) THEN
        RAISE EXCEPTION 'Only the meet creator can end this flash meet' USING ERRCODE = '42501';
    END IF;

    UPDATE public.campus_flash_meets
    SET expires_at_epoch_ms = LEAST(expires_at_epoch_ms, v_now_ms)
    WHERE id = p_meet_id;

    RETURN TRUE;
END;
$$;

REVOKE ALL ON FUNCTION public.end_flash_meet(TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.end_flash_meet(TEXT) TO authenticated;


-- ============================================================================
-- 3. CAMPUS EVENTS EXTENSIONS & RSVPS
-- ============================================================================

ALTER TABLE public.campus_events ADD COLUMN IF NOT EXISTS host_name TEXT NOT NULL DEFAULT '';
ALTER TABLE public.campus_events ADD COLUMN IF NOT EXISTS full_address TEXT NOT NULL DEFAULT '';
ALTER TABLE public.campus_events ADD COLUMN IF NOT EXISTS price_label TEXT DEFAULT 'Free Admission';
ALTER TABLE public.campus_events ADD COLUMN IF NOT EXISTS max_capacity INTEGER NOT NULL DEFAULT 100;
ALTER TABLE public.campus_events ADD COLUMN IF NOT EXISTS artwork_key TEXT NOT NULL DEFAULT 'event_thumb_founders';

CREATE TABLE IF NOT EXISTS public.campus_event_rsvps (
    event_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    display_name TEXT NOT NULL DEFAULT '',
    created_at_epoch_ms BIGINT NOT NULL DEFAULT ((extract(epoch from timezone('utc'::text, now())) * 1000)::bigint),
    PRIMARY KEY (event_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_campus_event_rsvps_user
    ON public.campus_event_rsvps(user_id);

ALTER TABLE public.campus_event_rsvps ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Students read event rsvps" ON public.campus_event_rsvps;
CREATE POLICY "Students read event rsvps"
    ON public.campus_event_rsvps FOR SELECT TO authenticated
    USING (true);

DROP POLICY IF EXISTS "Student inserts own event rsvp" ON public.campus_event_rsvps;
CREATE POLICY "Student inserts own event rsvp"
    ON public.campus_event_rsvps FOR INSERT TO authenticated
    WITH CHECK (public.is_current_campus_user(user_id));

DROP POLICY IF EXISTS "Student deletes own event rsvp" ON public.campus_event_rsvps;
CREATE POLICY "Student deletes own event rsvp"
    ON public.campus_event_rsvps FOR DELETE TO authenticated
    USING (public.is_current_campus_user(user_id));

CREATE OR REPLACE FUNCTION public.toggle_campus_event_rsvp(p_event_id TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_user_id TEXT;
    v_display_name TEXT := '';
    v_event public.campus_events%ROWTYPE;
    v_now_ms BIGINT := (extract(epoch from timezone('utc'::text, now())) * 1000)::bigint;
    v_existing BOOLEAN := false;
    v_is_registered BOOLEAN := false;
    v_new_count INTEGER;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Authentication is required' USING ERRCODE = '42501';
    END IF;

    SELECT
        COALESCE(NULLIF(BTRIM(p.student_id), ''), NULLIF(BTRIM(p.university_email), ''), v_uid::text),
        COALESCE(NULLIF(BTRIM(p.full_name), ''), '')
    INTO v_user_id, v_display_name
    FROM public.campusprofile p
    WHERE p.user_id = v_uid
    LIMIT 1;

    IF v_user_id IS NULL OR BTRIM(v_user_id) = '' THEN
        v_user_id := v_uid::text;
    END IF;

    SELECT * INTO v_event
    FROM public.campus_events e
    WHERE e.id = p_event_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Campus event not found' USING ERRCODE = 'P0002';
    END IF;

    SELECT EXISTS (
        SELECT 1 FROM public.campus_event_rsvps r
        WHERE r.event_id = p_event_id AND public.is_current_campus_user(r.user_id)
    ) INTO v_existing;

    IF v_existing THEN
        DELETE FROM public.campus_event_rsvps r
        WHERE r.event_id = p_event_id AND public.is_current_campus_user(r.user_id);
        v_is_registered := false;
    ELSE
        IF v_event.attendee_count >= COALESCE(NULLIF(v_event.max_capacity, 0), 100) THEN
            RAISE EXCEPTION 'Event is at full capacity' USING ERRCODE = '22023';
        END IF;
        INSERT INTO public.campus_event_rsvps (event_id, user_id, display_name, created_at_epoch_ms)
        VALUES (p_event_id, v_user_id, v_display_name, v_now_ms)
        ON CONFLICT (event_id, user_id) DO NOTHING;
        v_is_registered := true;
    END IF;

    SELECT GREATEST(
        0,
        (
            SELECT COUNT(*)::integer FROM public.campus_event_rsvps r
            WHERE r.event_id = p_event_id
        )
    ) INTO v_new_count;

    UPDATE public.campus_events
    SET attendee_count = v_new_count
    WHERE id = p_event_id;

    RETURN jsonb_build_object(
        'is_registered', v_is_registered,
        'attendee_count', v_new_count
    );
END;
$$;

REVOKE ALL ON FUNCTION public.toggle_campus_event_rsvp(TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.toggle_campus_event_rsvp(TEXT) TO authenticated;


-- ============================================================================
-- 4. CAMPUS PLACES SECURITY, BOOKMARKS, REVIEWS & SEED DATA
-- ============================================================================

ALTER TABLE public.campus_places ADD COLUMN IF NOT EXISTS neighborhood TEXT NOT NULL DEFAULT '';
ALTER TABLE public.campus_places ADD COLUMN IF NOT EXISTS security_level TEXT NOT NULL DEFAULT 'CCTV Monitored · 24/7 Guard';
ALTER TABLE public.campus_places ADD COLUMN IF NOT EXISTS has_wifi BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE public.campus_places ADD COLUMN IF NOT EXISTS has_power_outlets BOOLEAN NOT NULL DEFAULT true;

DROP POLICY IF EXISTS "Public can view campus places" ON public.campus_places;
DROP POLICY IF EXISTS "Authenticated view campus places" ON public.campus_places;
CREATE POLICY "Authenticated view campus places"
    ON public.campus_places FOR SELECT TO authenticated
    USING (true);

CREATE TABLE IF NOT EXISTS public.campus_place_bookmarks (
    place_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('SAVED', 'BEEN')),
    created_at_epoch_ms BIGINT NOT NULL DEFAULT ((extract(epoch from timezone('utc'::text, now())) * 1000)::bigint),
    PRIMARY KEY (place_id, user_id, status)
);

CREATE INDEX IF NOT EXISTS idx_campus_place_bookmarks_user
    ON public.campus_place_bookmarks(user_id);

ALTER TABLE public.campus_place_bookmarks ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Students read place bookmarks" ON public.campus_place_bookmarks;
CREATE POLICY "Students read place bookmarks"
    ON public.campus_place_bookmarks FOR SELECT TO authenticated
    USING (true);

DROP POLICY IF EXISTS "Student inserts own place bookmark" ON public.campus_place_bookmarks;
CREATE POLICY "Student inserts own place bookmark"
    ON public.campus_place_bookmarks FOR INSERT TO authenticated
    WITH CHECK (public.is_current_campus_user(user_id));

DROP POLICY IF EXISTS "Student deletes own place bookmark" ON public.campus_place_bookmarks;
CREATE POLICY "Student deletes own place bookmark"
    ON public.campus_place_bookmarks FOR DELETE TO authenticated
    USING (public.is_current_campus_user(user_id));

CREATE TABLE IF NOT EXISTS public.campus_place_reviews (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    place_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    author_name TEXT NOT NULL DEFAULT '',
    comment TEXT NOT NULL,
    created_at_epoch_ms BIGINT NOT NULL DEFAULT ((extract(epoch from timezone('utc'::text, now())) * 1000)::bigint)
);

CREATE INDEX IF NOT EXISTS idx_campus_place_reviews_place
    ON public.campus_place_reviews(place_id, created_at_epoch_ms DESC);

ALTER TABLE public.campus_place_reviews ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Students read place reviews" ON public.campus_place_reviews;
CREATE POLICY "Students read place reviews"
    ON public.campus_place_reviews FOR SELECT TO authenticated
    USING (true);

DROP POLICY IF EXISTS "Student inserts own place review" ON public.campus_place_reviews;
CREATE POLICY "Student inserts own place review"
    ON public.campus_place_reviews FOR INSERT TO authenticated
    WITH CHECK (public.is_current_campus_user(user_id));

DROP POLICY IF EXISTS "Student deletes own place review" ON public.campus_place_reviews;
CREATE POLICY "Student deletes own place review"
    ON public.campus_place_reviews FOR DELETE TO authenticated
    USING (public.is_current_campus_user(user_id));

-- Ensure any legacy seeded campus places are removed (production does not seed sample rows)
DELETE FROM public.campus_places
WHERE id IN (
    'strat_quad_b',
    'main_library',
    'innovation_hub_auditorium',
    'student_guild_hub',
    'mombasa_marine_commons',
    'kisumu_lakeview_lab',
    'kisumu_lakeside_lab'
);


-- ============================================================================
-- 5. REALTIME PUBLICATIONS & REPLICA IDENTITY
-- ============================================================================

ALTER TABLE public.campus_live_locations REPLICA IDENTITY FULL;
ALTER TABLE public.campus_flash_meets REPLICA IDENTITY FULL;
ALTER TABLE public.campus_flash_meet_attendees REPLICA IDENTITY FULL;
ALTER TABLE public.campus_events REPLICA IDENTITY FULL;
ALTER TABLE public.campus_event_rsvps REPLICA IDENTITY FULL;
ALTER TABLE public.campus_places REPLICA IDENTITY FULL;
ALTER TABLE public.campus_place_bookmarks REPLICA IDENTITY FULL;
ALTER TABLE public.campus_place_reviews REPLICA IDENTITY FULL;

DO $$ BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE public.campus_live_locations; EXCEPTION WHEN duplicate_object THEN NULL; WHEN undefined_object THEN NULL; END $$;
DO $$ BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE public.campus_flash_meets; EXCEPTION WHEN duplicate_object THEN NULL; WHEN undefined_object THEN NULL; END $$;
DO $$ BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE public.campus_flash_meet_attendees; EXCEPTION WHEN duplicate_object THEN NULL; WHEN undefined_object THEN NULL; END $$;
DO $$ BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE public.campus_events; EXCEPTION WHEN duplicate_object THEN NULL; WHEN undefined_object THEN NULL; END $$;
DO $$ BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE public.campus_event_rsvps; EXCEPTION WHEN duplicate_object THEN NULL; WHEN undefined_object THEN NULL; END $$;
DO $$ BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE public.campus_places; EXCEPTION WHEN duplicate_object THEN NULL; WHEN undefined_object THEN NULL; END $$;
DO $$ BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE public.campus_place_bookmarks; EXCEPTION WHEN duplicate_object THEN NULL; WHEN undefined_object THEN NULL; END $$;
DO $$ BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE public.campus_place_reviews; EXCEPTION WHEN duplicate_object THEN NULL; WHEN undefined_object THEN NULL; END $$;
