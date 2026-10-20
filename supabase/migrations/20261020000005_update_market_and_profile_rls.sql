-- Migration: 20261020000005_update_market_and_profile_rls.sql
-- Enables seamless remote writes for Marketplace, Gigs, and Profile uniqueness.

-- 1. Ensure unique constraint on university_email for ON CONFLICT target
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_campusprofile_university_email'
    ) THEN
        ALTER TABLE public.campusprofile ADD CONSTRAINT uq_campusprofile_university_email UNIQUE (university_email);
    END IF;
END $$;

-- 2. Open RLS on campus_market_items for student operations
DROP POLICY IF EXISTS "Public can view market items" ON public.campus_market_items;
DROP POLICY IF EXISTS "Authenticated can insert market items" ON public.campus_market_items;
DROP POLICY IF EXISTS "Authenticated can update market items" ON public.campus_market_items;
DROP POLICY IF EXISTS "Allow public insert on market items" ON public.campus_market_items;
DROP POLICY IF EXISTS "Allow public update on market items" ON public.campus_market_items;
DROP POLICY IF EXISTS "Allow public delete on market items" ON public.campus_market_items;

CREATE POLICY "Allow public select on market items" ON public.campus_market_items FOR SELECT USING (true);
CREATE POLICY "Allow public insert on market items" ON public.campus_market_items FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow public update on market items" ON public.campus_market_items FOR UPDATE USING (true) WITH CHECK (true);
CREATE POLICY "Allow public delete on market items" ON public.campus_market_items FOR DELETE USING (true);

-- 3. Open RLS on campus_micro_gigs
DROP POLICY IF EXISTS "Public can view micro gigs" ON public.campus_micro_gigs;
DROP POLICY IF EXISTS "Authenticated can insert micro gigs" ON public.campus_micro_gigs;
DROP POLICY IF EXISTS "Authenticated can update micro gigs" ON public.campus_micro_gigs;

CREATE POLICY "Allow public select on micro gigs" ON public.campus_micro_gigs FOR SELECT USING (true);
CREATE POLICY "Allow public insert on micro gigs" ON public.campus_micro_gigs FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow public update on micro gigs" ON public.campus_micro_gigs FOR UPDATE USING (true) WITH CHECK (true);
CREATE POLICY "Allow public delete on micro gigs" ON public.campus_micro_gigs FOR DELETE USING (true);
