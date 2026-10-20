-- ==============================================================================
-- Migration: Enable anon and auth full management of campus_friends
-- Allows friend requests and reciprocal relationships to be written from mobile client
-- ==============================================================================

ALTER TABLE public.campus_friends ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view their friendships" ON public.campus_friends;
CREATE POLICY "Users can view their friendships"
    ON public.campus_friends FOR SELECT USING (true);

DROP POLICY IF EXISTS "Authenticated can manage friendships" ON public.campus_friends;
DROP POLICY IF EXISTS "Allow anon and auth manage friendships" ON public.campus_friends;
CREATE POLICY "Allow anon and auth manage friendships"
    ON public.campus_friends FOR ALL
    USING (true)
    WITH CHECK (true);

DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.campus_friends;
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN undefined_object THEN NULL;
END $$;
