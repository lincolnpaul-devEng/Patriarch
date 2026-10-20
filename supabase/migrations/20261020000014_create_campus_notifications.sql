-- ==============================================================================
-- Migration: campus_notifications inbox for DM + friend-request alerts
-- Polled by CampusNotificationPollManager; optionally pushed via FCM later.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS public.campus_notifications (
    id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
    recipient_user_id TEXT NOT NULL,
    actor_user_id TEXT NOT NULL DEFAULT '',
    actor_name TEXT NOT NULL DEFAULT 'Campus peer',
    type TEXT NOT NULL DEFAULT 'DIRECT_MESSAGE',
    title TEXT NOT NULL DEFAULT 'Campus Alert',
    body TEXT NOT NULL DEFAULT '',
    thread_id TEXT DEFAULT NULL,
    server_id TEXT DEFAULT NULL,
    payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    created_at_epoch_ms BIGINT NOT NULL DEFAULT ((extract(epoch from timezone('utc'::text, now())) * 1000)::bigint)
);

CREATE INDEX IF NOT EXISTS idx_campus_notifications_recipient_created
    ON public.campus_notifications(recipient_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_campus_notifications_type
    ON public.campus_notifications(type);

ALTER TABLE public.campus_notifications ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Select campus notifications" ON public.campus_notifications;
CREATE POLICY "Select campus notifications"
    ON public.campus_notifications FOR SELECT USING (true);

DROP POLICY IF EXISTS "Insert campus notifications" ON public.campus_notifications;
CREATE POLICY "Insert campus notifications"
    ON public.campus_notifications FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Update campus notifications" ON public.campus_notifications;
CREATE POLICY "Update campus notifications"
    ON public.campus_notifications FOR UPDATE USING (true) WITH CHECK (true);

ALTER TABLE public.campus_notifications REPLICA IDENTITY FULL;
DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.campus_notifications;
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN undefined_object THEN NULL;
END $$;

-- Mirror friend graph to remote for cross-device friend requests
ALTER TABLE public.campus_friends
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now());
