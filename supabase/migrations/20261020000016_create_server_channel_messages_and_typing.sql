-- ==============================================================================
-- Server channel message stream + ephemeral typing presence
-- ==============================================================================

CREATE TABLE IF NOT EXISTS public.campus_server_channel_messages (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    server_id TEXT NOT NULL,
    channel_id TEXT NOT NULL,
    sender_id TEXT NOT NULL,
    sender_name TEXT NOT NULL DEFAULT 'Member',
    sender_avatar_url TEXT DEFAULT NULL,
    text TEXT NOT NULL DEFAULT '',
    gif_url TEXT DEFAULT NULL,
    image_urls JSONB NOT NULL DEFAULT '[]'::jsonb,
    reaction_count INTEGER NOT NULL DEFAULT 0,
    timestamp_epoch_ms BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_server_channel_msgs_channel_ts
    ON public.campus_server_channel_messages(channel_id, timestamp_epoch_ms ASC);

CREATE INDEX IF NOT EXISTS idx_server_channel_msgs_server
    ON public.campus_server_channel_messages(server_id);

ALTER TABLE public.campus_server_channel_messages ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Select server channel messages" ON public.campus_server_channel_messages;
CREATE POLICY "Select server channel messages"
    ON public.campus_server_channel_messages FOR SELECT USING (true);

DROP POLICY IF EXISTS "Insert server channel messages" ON public.campus_server_channel_messages;
CREATE POLICY "Insert server channel messages"
    ON public.campus_server_channel_messages FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Update server channel messages" ON public.campus_server_channel_messages;
CREATE POLICY "Update server channel messages"
    ON public.campus_server_channel_messages FOR UPDATE USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Delete server channel messages" ON public.campus_server_channel_messages;
CREATE POLICY "Delete server channel messages"
    ON public.campus_server_channel_messages FOR DELETE USING (true);

ALTER TABLE public.campus_server_channel_messages REPLICA IDENTITY FULL;
DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.campus_server_channel_messages;
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN undefined_object THEN NULL;
END $$;

-- Typing presence (ephemeral; clients expire rows older than ~4s)
CREATE TABLE IF NOT EXISTS public.campus_channel_typing (
    channel_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    server_id TEXT NOT NULL DEFAULT '',
    display_name TEXT NOT NULL DEFAULT 'Member',
    avatar_url TEXT DEFAULT NULL,
    updated_at_epoch_ms BIGINT NOT NULL,
    PRIMARY KEY (channel_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_channel_typing_updated
    ON public.campus_channel_typing(updated_at_epoch_ms DESC);

CREATE INDEX IF NOT EXISTS idx_channel_typing_server
    ON public.campus_channel_typing(server_id);

ALTER TABLE public.campus_channel_typing ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Select channel typing" ON public.campus_channel_typing;
CREATE POLICY "Select channel typing"
    ON public.campus_channel_typing FOR SELECT USING (true);

DROP POLICY IF EXISTS "Upsert channel typing" ON public.campus_channel_typing;
CREATE POLICY "Upsert channel typing"
    ON public.campus_channel_typing FOR ALL USING (true) WITH CHECK (true);
