-- ==============================================================================
-- Ephemeral typing presence for direct-message / chat threads
-- ==============================================================================

CREATE TABLE IF NOT EXISTS public.campus_dm_typing (
    thread_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    display_name TEXT NOT NULL DEFAULT 'Student',
    avatar_url TEXT DEFAULT NULL,
    updated_at_epoch_ms BIGINT NOT NULL,
    PRIMARY KEY (thread_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_dm_typing_updated
    ON public.campus_dm_typing(updated_at_epoch_ms DESC);

CREATE INDEX IF NOT EXISTS idx_dm_typing_thread
    ON public.campus_dm_typing(thread_id);

ALTER TABLE public.campus_dm_typing ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Select dm typing" ON public.campus_dm_typing;
CREATE POLICY "Select dm typing"
    ON public.campus_dm_typing FOR SELECT USING (true);

DROP POLICY IF EXISTS "Upsert dm typing" ON public.campus_dm_typing;
CREATE POLICY "Upsert dm typing"
    ON public.campus_dm_typing FOR ALL USING (true) WITH CHECK (true);
