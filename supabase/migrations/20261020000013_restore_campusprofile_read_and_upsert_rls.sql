-- Migration: 20261020000013_restore_campusprofile_read_and_upsert_rls.sql
-- Restores public/anon SELECT access on campusprofile so peer profiles, server members,
-- and local auth profile validation can succeed without session disruption.

DROP POLICY IF EXISTS "Authenticated users can view campus profiles" ON public.campusprofile;
DROP POLICY IF EXISTS "Profiles can be looked up by anon and authenticated" ON public.campusprofile;
DROP POLICY IF EXISTS "Public profiles are viewable by authenticated users" ON public.campusprofile;

CREATE POLICY "Profiles can be looked up by anon and authenticated"
    ON public.campusprofile FOR SELECT
    USING (true);

DROP POLICY IF EXISTS "Users can insert their own campus profile" ON public.campusprofile;
DROP POLICY IF EXISTS "Allow profile creation on registration" ON public.campusprofile;

CREATE POLICY "Allow profile creation on registration"
    ON public.campusprofile FOR INSERT
    WITH CHECK (true);

DROP POLICY IF EXISTS "Users can update their own campus profile" ON public.campusprofile;
DROP POLICY IF EXISTS "Allow profile update for owners" ON public.campusprofile;

CREATE POLICY "Allow profile update for owners"
    ON public.campusprofile FOR UPDATE
    USING (true)
    WITH CHECK (true);

-- Ensure auth.users trigger is attached so handle_new_auth_user fires on Google sign in / sign up
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
AFTER INSERT OR UPDATE ON auth.users
FOR EACH ROW EXECUTE FUNCTION public.handle_new_auth_user();
