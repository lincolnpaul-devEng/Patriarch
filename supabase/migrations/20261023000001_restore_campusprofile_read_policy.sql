-- Migration: 20261023000001_restore_campusprofile_read_policy.sql
-- Restores directory and marketplace read access for anon and authenticated users
-- so Find Friends, seller profile discovery, avatars, and item browsing work seamlessly.

DROP POLICY IF EXISTS "Profiles can be looked up by anon and authenticated" ON public.campusprofile;
DROP POLICY IF EXISTS "Authenticated profiles" ON public.campusprofile;

CREATE POLICY "Profiles can be looked up by anon and authenticated"
    ON public.campusprofile FOR SELECT
    TO anon, authenticated
    USING (true);

GRANT SELECT ON public.campusprofile TO anon, authenticated;

-- Allow browsing campus market items and micro gigs
GRANT SELECT ON public.campus_market_items TO anon, authenticated;
DROP POLICY IF EXISTS "Students browse market items" ON public.campus_market_items;
CREATE POLICY "Students browse market items"
    ON public.campus_market_items FOR SELECT
    TO anon, authenticated
    USING (true);

GRANT SELECT ON public.campus_micro_gigs TO anon, authenticated;
DROP POLICY IF EXISTS "Students read micro gigs" ON public.campus_micro_gigs;
CREATE POLICY "Students read micro gigs"
    ON public.campus_micro_gigs FOR SELECT
    TO anon, authenticated
    USING (true);
