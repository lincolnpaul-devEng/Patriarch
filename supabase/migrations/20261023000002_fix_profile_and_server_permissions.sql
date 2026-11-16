-- Migration: 20261023000002_fix_profile_and_server_permissions.sql
-- Fix permissions and policies for server creation, profile upsert, and channel discovery.

-- 1. Grant execute on create_server_atomic
GRANT EXECUTE ON FUNCTION public.create_server_atomic(
    TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT
) TO authenticated, anon;

-- 2. Allow authenticated users to insert their own profile (for client upsert)
DROP POLICY IF EXISTS "Users can insert their own campus profile" ON public.campusprofile;
CREATE POLICY "Users can insert their own campus profile"
    ON public.campusprofile FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

-- 3. Ensure server hierarchy can be viewed by all users (authenticated + anon for discovery)
DROP POLICY IF EXISTS "Public can view servers" ON public.servers;
CREATE POLICY "Public can view servers"
    ON public.servers FOR SELECT
    TO anon, authenticated
    USING (true);
GRANT SELECT ON public.servers TO anon, authenticated;

DROP POLICY IF EXISTS "Public can view server members" ON public.server_members;
CREATE POLICY "Public can view server members"
    ON public.server_members FOR SELECT
    TO anon, authenticated
    USING (true);
GRANT SELECT ON public.server_members TO anon, authenticated;

DROP POLICY IF EXISTS "Public can view server channels" ON public.server_channels;
CREATE POLICY "Public can view server channels"
    ON public.server_channels FOR SELECT
    TO anon, authenticated
    USING (true);
GRANT SELECT ON public.server_channels TO anon, authenticated;

DROP POLICY IF EXISTS "Public can view server rules" ON public.server_rules;
CREATE POLICY "Public can view server rules"
    ON public.server_rules FOR SELECT
    TO anon, authenticated
    USING (true);
GRANT SELECT ON public.server_rules TO anon, authenticated;

DROP POLICY IF EXISTS "Public can view server invites" ON public.server_invites;
CREATE POLICY "Public can view server invites"
    ON public.server_invites FOR SELECT
    TO anon, authenticated
    USING (true);
GRANT SELECT ON public.server_invites TO anon, authenticated;

-- 4. Grant execute on ensure_campusprofile_for_auth_user
GRANT EXECUTE ON FUNCTION public.ensure_campusprofile_for_auth_user(TEXT, TEXT) TO authenticated, anon;
