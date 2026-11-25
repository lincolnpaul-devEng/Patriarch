-- =============================================================================
-- Migration: Live Room Realtime Publications, Likes Ledger, Presence & RPCs
-- =============================================================================

-- 1. Live stream likes ledger (audit trail + atomic counter synchronization)
CREATE TABLE IF NOT EXISTS public.campus_live_stream_likes (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    stream_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    created_at_epoch_ms BIGINT NOT NULL DEFAULT ((extract(epoch from timezone('utc'::text, now())) * 1000)::bigint),
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_campus_live_stream_likes_stream
    ON public.campus_live_stream_likes(stream_id, created_at_epoch_ms DESC);

ALTER TABLE public.campus_live_stream_likes ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Students read live stream likes" ON public.campus_live_stream_likes;
CREATE POLICY "Students read live stream likes"
    ON public.campus_live_stream_likes FOR SELECT TO authenticated USING (true);

DROP POLICY IF EXISTS "Student inserts own live stream like" ON public.campus_live_stream_likes;
CREATE POLICY "Student inserts own live stream like"
    ON public.campus_live_stream_likes FOR INSERT TO authenticated
    WITH CHECK (public.is_current_campus_user(user_id));

-- 2. Atomic RPC to register a viewer/host like on any live stream
CREATE OR REPLACE FUNCTION public.increment_live_stream_likes(
    p_stream_id TEXT,
    p_delta INTEGER DEFAULT 1
)
RETURNS INTEGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_user_id TEXT;
    v_delta INTEGER := LEAST(GREATEST(COALESCE(p_delta, 1), 1), 10);
    v_new_count INTEGER;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Authentication is required' USING ERRCODE = '42501';
    END IF;

    SELECT COALESCE(NULLIF(BTRIM(p.student_id), ''), v_uid::text)
    INTO v_user_id
    FROM public.campusprofile AS p
    WHERE p.user_id = v_uid
    LIMIT 1;

    IF v_user_id IS NULL OR BTRIM(v_user_id) = '' THEN
        v_user_id := v_uid::text;
    END IF;

    INSERT INTO public.campus_live_stream_likes (stream_id, user_id, created_at_epoch_ms)
    VALUES (
        p_stream_id,
        v_user_id,
        ((extract(epoch from timezone('utc'::text, now())) * 1000)::bigint)
    );

    UPDATE public.campus_live_streams
    SET like_count = GREATEST(0, like_count + v_delta)
    WHERE id = p_stream_id
    RETURNING like_count INTO v_new_count;

    RETURN COALESCE(v_new_count, 0);
END;
$$;

REVOKE ALL ON FUNCTION public.increment_live_stream_likes(TEXT, INTEGER) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.increment_live_stream_likes(TEXT, INTEGER) TO authenticated;

-- 3. Atomic RPC to adjust live viewer counts when viewers join or leave
CREATE OR REPLACE FUNCTION public.adjust_live_stream_viewers(
    p_stream_id TEXT,
    p_delta INTEGER
)
RETURNS INTEGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_delta INTEGER := LEAST(GREATEST(COALESCE(p_delta, 0), -1), 1);
    v_new_count INTEGER;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Authentication is required' USING ERRCODE = '42501';
    END IF;

    UPDATE public.campus_live_streams
    SET
        viewer_count = GREATEST(1, viewer_count + v_delta),
        peak_viewers = GREATEST(peak_viewers, GREATEST(1, viewer_count + v_delta))
    WHERE id = p_stream_id
      AND is_live = true
    RETURNING viewer_count INTO v_new_count;

    RETURN COALESCE(v_new_count, 1);
END;
$$;

REVOKE ALL ON FUNCTION public.adjust_live_stream_viewers(TEXT, INTEGER) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.adjust_live_stream_viewers(TEXT, INTEGER) TO authenticated;

-- 4. Real-time Live Room presence (raised hands, mic/cam status, active participants)
CREATE TABLE IF NOT EXISTS public.campus_live_room_presence (
    user_id TEXT PRIMARY KEY,
    display_name TEXT NOT NULL DEFAULT '',
    avatar_url TEXT DEFAULT NULL,
    stream_id TEXT DEFAULT NULL,
    is_hand_raised BOOLEAN NOT NULL DEFAULT false,
    is_muted BOOLEAN NOT NULL DEFAULT false,
    is_camera_on BOOLEAN NOT NULL DEFAULT true,
    is_on_stage BOOLEAN NOT NULL DEFAULT false,
    last_seen_epoch_ms BIGINT NOT NULL DEFAULT ((extract(epoch from timezone('utc'::text, now())) * 1000)::bigint),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_campus_live_room_presence_last_seen
    ON public.campus_live_room_presence(last_seen_epoch_ms DESC);

ALTER TABLE public.campus_live_room_presence ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Students read live room presence" ON public.campus_live_room_presence;
CREATE POLICY "Students read live room presence"
    ON public.campus_live_room_presence FOR SELECT TO authenticated USING (true);

DROP POLICY IF EXISTS "Student inserts own live room presence" ON public.campus_live_room_presence;
CREATE POLICY "Student inserts own live room presence"
    ON public.campus_live_room_presence FOR INSERT TO authenticated
    WITH CHECK (public.is_current_campus_user(user_id));

DROP POLICY IF EXISTS "Student updates own live room presence" ON public.campus_live_room_presence;
CREATE POLICY "Student updates own live room presence"
    ON public.campus_live_room_presence FOR UPDATE TO authenticated
    USING (public.is_current_campus_user(user_id))
    WITH CHECK (public.is_current_campus_user(user_id));

DROP POLICY IF EXISTS "Student deletes own live room presence" ON public.campus_live_room_presence;
CREATE POLICY "Student deletes own live room presence"
    ON public.campus_live_room_presence FOR DELETE TO authenticated
    USING (public.is_current_campus_user(user_id));

-- 5. Enable FULL replica identity and ensure Realtime publication includes all live tables
ALTER TABLE public.campus_live_streams REPLICA IDENTITY FULL;
ALTER TABLE public.campus_stream_comments REPLICA IDENTITY FULL;
ALTER TABLE public.campus_live_room_presence REPLICA IDENTITY FULL;
ALTER TABLE public.campus_live_stream_likes REPLICA IDENTITY FULL;

DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.campus_live_streams;
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN undefined_object THEN NULL;
END $$;

DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.campus_stream_comments;
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN undefined_object THEN NULL;
END $$;

DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.campus_live_room_presence;
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN undefined_object THEN NULL;
END $$;

DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.campus_live_stream_likes;
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN undefined_object THEN NULL;
END $$;
