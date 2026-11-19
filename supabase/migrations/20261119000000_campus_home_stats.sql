-- Home screen statistics: real, global campus counts (events, posts, guilds).
-- Aggregates only — no row data leaves the database, so it is safe for any signed-in user.
CREATE OR REPLACE FUNCTION public.campus_home_stats()
RETURNS TABLE(events_count BIGINT, posts_count BIGINT, guilds_count BIGINT)
LANGUAGE sql
SECURITY DEFINER
SET search_path = ''
AS $$
  SELECT
    (SELECT count(*) FROM public.campus_events),
    (SELECT count(*) FROM public.campus_server_channel_messages),
    (SELECT count(*) FROM public.campus_servers);
$$;

REVOKE ALL ON FUNCTION public.campus_home_stats() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.campus_home_stats() TO authenticated, anon;
