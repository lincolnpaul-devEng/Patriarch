-- ==============================================================================
-- Campus call signaling (invite / accept / decline / end)
-- Callees discover RINGING sessions by polling or realtime; FCM can mirror later.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS public.campus_call_sessions (
    id TEXT PRIMARY KEY,
    channel_id TEXT NOT NULL,
    caller_id TEXT NOT NULL,
    caller_name TEXT NOT NULL DEFAULT '',
    callee_id TEXT NOT NULL,
    callee_name TEXT NOT NULL DEFAULT '',
    media_kind TEXT NOT NULL DEFAULT 'AUDIO',
    status TEXT NOT NULL DEFAULT 'RINGING',
    created_at_epoch_ms BIGINT NOT NULL DEFAULT 0,
    updated_at_epoch_ms BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_campus_call_sessions_callee_status
    ON public.campus_call_sessions(callee_id, status, updated_at_epoch_ms DESC);

CREATE INDEX IF NOT EXISTS idx_campus_call_sessions_caller
    ON public.campus_call_sessions(caller_id, status);

CREATE INDEX IF NOT EXISTS idx_campus_call_sessions_channel
    ON public.campus_call_sessions(channel_id);

ALTER TABLE public.campus_call_sessions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Select call sessions" ON public.campus_call_sessions;
DROP POLICY IF EXISTS "Insert call sessions" ON public.campus_call_sessions;
DROP POLICY IF EXISTS "Update call sessions" ON public.campus_call_sessions;

CREATE POLICY "Select call sessions"
    ON public.campus_call_sessions FOR SELECT USING (true);

CREATE POLICY "Insert call sessions"
    ON public.campus_call_sessions FOR INSERT
    WITH CHECK (length(trim(caller_id)) > 0 AND length(trim(callee_id)) > 0);

CREATE POLICY "Update call sessions"
    ON public.campus_call_sessions FOR UPDATE
    USING (true)
    WITH CHECK (length(trim(status)) > 0);

-- Optional device push tokens for a future notify-call Edge Function
CREATE TABLE IF NOT EXISTS public.campus_device_push_tokens (
    user_id TEXT PRIMARY KEY,
    fcm_token TEXT NOT NULL,
    updated_at_epoch_ms BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.campus_device_push_tokens ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Upsert own push token" ON public.campus_device_push_tokens;
DROP POLICY IF EXISTS "Select push tokens" ON public.campus_device_push_tokens;
CREATE POLICY "Select push tokens" ON public.campus_device_push_tokens FOR SELECT USING (true);
CREATE POLICY "Upsert own push token" ON public.campus_device_push_tokens
    FOR ALL USING (true) WITH CHECK (length(trim(user_id)) > 0 AND length(trim(fcm_token)) > 0);

ALTER TABLE public.campus_call_sessions REPLICA IDENTITY FULL;
DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.campus_call_sessions;
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN undefined_object THEN NULL;
END $$;
