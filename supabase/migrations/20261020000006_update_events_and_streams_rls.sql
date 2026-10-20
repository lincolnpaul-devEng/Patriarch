-- Migration: 20261020000006_update_events_and_streams_rls.sql
-- Enables public CRUD RLS policies for campus_events, campus_live_streams, campus_stream_comments, and campus_flash_meets

-- 1. campus_events
DROP POLICY IF EXISTS "Public can view campus events" ON public.campus_events;
DROP POLICY IF EXISTS "Authenticated can insert campus events" ON public.campus_events;
DROP POLICY IF EXISTS "Allow public select on campus events" ON public.campus_events;
DROP POLICY IF EXISTS "Allow public insert on campus events" ON public.campus_events;
DROP POLICY IF EXISTS "Allow public update on campus events" ON public.campus_events;
DROP POLICY IF EXISTS "Allow public delete on campus events" ON public.campus_events;

CREATE POLICY "Allow public select on campus events" ON public.campus_events FOR SELECT USING (true);
CREATE POLICY "Allow public insert on campus events" ON public.campus_events FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow public update on campus events" ON public.campus_events FOR UPDATE USING (true) WITH CHECK (true);
CREATE POLICY "Allow public delete on campus events" ON public.campus_events FOR DELETE USING (true);

-- 2. campus_live_streams
DROP POLICY IF EXISTS "Public can view live streams" ON public.campus_live_streams;
DROP POLICY IF EXISTS "Authenticated can insert live streams" ON public.campus_live_streams;
DROP POLICY IF EXISTS "Authenticated can update live streams" ON public.campus_live_streams;
DROP POLICY IF EXISTS "Allow public select on live streams" ON public.campus_live_streams;
DROP POLICY IF EXISTS "Allow public insert on live streams" ON public.campus_live_streams;
DROP POLICY IF EXISTS "Allow public update on live streams" ON public.campus_live_streams;
DROP POLICY IF EXISTS "Allow public delete on live streams" ON public.campus_live_streams;

CREATE POLICY "Allow public select on live streams" ON public.campus_live_streams FOR SELECT USING (true);
CREATE POLICY "Allow public insert on live streams" ON public.campus_live_streams FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow public update on live streams" ON public.campus_live_streams FOR UPDATE USING (true) WITH CHECK (true);
CREATE POLICY "Allow public delete on live streams" ON public.campus_live_streams FOR DELETE USING (true);

-- 3. campus_stream_comments
DROP POLICY IF EXISTS "Public can view stream comments" ON public.campus_stream_comments;
DROP POLICY IF EXISTS "Authenticated can insert stream comments" ON public.campus_stream_comments;
DROP POLICY IF EXISTS "Allow public select on stream comments" ON public.campus_stream_comments;
DROP POLICY IF EXISTS "Allow public insert on stream comments" ON public.campus_stream_comments;
DROP POLICY IF EXISTS "Allow public update on stream comments" ON public.campus_stream_comments;
DROP POLICY IF EXISTS "Allow public delete on stream comments" ON public.campus_stream_comments;

CREATE POLICY "Allow public select on stream comments" ON public.campus_stream_comments FOR SELECT USING (true);
CREATE POLICY "Allow public insert on stream comments" ON public.campus_stream_comments FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow public update on stream comments" ON public.campus_stream_comments FOR UPDATE USING (true) WITH CHECK (true);
CREATE POLICY "Allow public delete on stream comments" ON public.campus_stream_comments FOR DELETE USING (true);

-- 4. campus_flash_meets
DROP POLICY IF EXISTS "Public can view flash meets" ON public.campus_flash_meets;
DROP POLICY IF EXISTS "Authenticated can insert flash meets" ON public.campus_flash_meets;
DROP POLICY IF EXISTS "Authenticated can update flash meets" ON public.campus_flash_meets;
DROP POLICY IF EXISTS "Allow public select on flash meets" ON public.campus_flash_meets;
DROP POLICY IF EXISTS "Allow public insert on flash meets" ON public.campus_flash_meets;
DROP POLICY IF EXISTS "Allow public update on flash meets" ON public.campus_flash_meets;
DROP POLICY IF EXISTS "Allow public delete on flash meets" ON public.campus_flash_meets;

CREATE POLICY "Allow public select on flash meets" ON public.campus_flash_meets FOR SELECT USING (true);
CREATE POLICY "Allow public insert on flash meets" ON public.campus_flash_meets FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow public update on flash meets" ON public.campus_flash_meets FOR UPDATE USING (true) WITH CHECK (true);
CREATE POLICY "Allow public delete on flash meets" ON public.campus_flash_meets FOR DELETE USING (true);
