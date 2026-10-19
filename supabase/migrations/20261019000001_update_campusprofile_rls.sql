-- =============================================================================
-- Migration: Update campusprofile RLS to allow lookup and registration
-- =============================================================================

DROP POLICY IF EXISTS "Public profiles are viewable by authenticated users" ON public.campusprofile;
DROP POLICY IF EXISTS "Profiles can be looked up by anon and authenticated" ON public.campusprofile;
DROP POLICY IF EXISTS "Users can insert their own profile" ON public.campusprofile;
DROP POLICY IF EXISTS "Users can update their own profile" ON public.campusprofile;

-- 1. Allow profile lookup by email for both anon (checking registration) and authenticated users
CREATE POLICY "Profiles can be looked up by anon and authenticated"
    ON public.campusprofile FOR SELECT
    USING (true);

-- 2. Allow profile creation on registration
CREATE POLICY "Allow profile creation on registration"
    ON public.campusprofile FOR INSERT
    WITH CHECK (auth.uid() = user_id OR user_id IS NULL OR auth.uid() IS NULL);

-- 3. Allow profile update
CREATE POLICY "Allow profile update for owners"
    ON public.campusprofile FOR UPDATE
    USING (auth.uid() = user_id OR user_id IS NULL OR auth.uid() IS NULL)
    WITH CHECK (auth.uid() = user_id OR user_id IS NULL OR auth.uid() IS NULL);
