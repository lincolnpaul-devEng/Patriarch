-- ==============================================================================
-- Migration: 20261020000001_update_chat_messages_schema.sql
-- Direct Messaging Production Features:
--   - Adds media (image_url, gif_url), replies (reply_to_text), reactions, and read receipts
--   - Adds comprehensive RLS policies (SELECT, INSERT, UPDATE, DELETE)
--   - Adds realtime replication publication
-- ==============================================================================

-- 1. Ensure table and modern metadata columns exist
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

ALTER TABLE public.campus_chat_messages
    ADD COLUMN IF NOT EXISTS image_url TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS gif_url TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS reply_to_text TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS reaction TEXT DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS is_delivered BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS is_read BOOLEAN NOT NULL DEFAULT false;

-- 2. Indexes for chronological stream retrieval
CREATE INDEX IF NOT EXISTS idx_campus_chat_messages_channel
    ON public.campus_chat_messages(channel_id, timestamp_epoch_ms ASC);

CREATE INDEX IF NOT EXISTS idx_campus_chat_messages_sender
    ON public.campus_chat_messages(sender_id);

-- 3. Row Level Security Policies
ALTER TABLE public.campus_chat_messages ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public can view chat messages" ON public.campus_chat_messages;
CREATE POLICY "Public can view chat messages"
    ON public.campus_chat_messages
    FOR SELECT
    USING (true);

DROP POLICY IF EXISTS "Authenticated can insert chat messages" ON public.campus_chat_messages;
DROP POLICY IF EXISTS "Users can insert chat messages" ON public.campus_chat_messages;
CREATE POLICY "Users can insert chat messages"
    ON public.campus_chat_messages
    FOR INSERT
    WITH CHECK (true);

DROP POLICY IF EXISTS "Authenticated can update chat messages" ON public.campus_chat_messages;
DROP POLICY IF EXISTS "Users can update chat messages" ON public.campus_chat_messages;
CREATE POLICY "Users can update chat messages"
    ON public.campus_chat_messages
    FOR UPDATE
    USING (true)
    WITH CHECK (true);

DROP POLICY IF EXISTS "Authenticated can delete chat messages" ON public.campus_chat_messages;
DROP POLICY IF EXISTS "Users can delete chat messages" ON public.campus_chat_messages;
CREATE POLICY "Users can delete chat messages"
    ON public.campus_chat_messages
    FOR DELETE
    USING (true);

-- 4. Enable Realtime Publications
ALTER TABLE public.campus_chat_messages REPLICA IDENTITY FULL;

DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.campus_chat_messages;
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN undefined_object THEN NULL;
END $$;
