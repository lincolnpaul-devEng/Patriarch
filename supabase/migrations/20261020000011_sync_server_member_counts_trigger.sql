-- Migration: 20261020000007_sync_server_member_counts_trigger.sql
-- Automatically synchronizes member_count in public.servers whenever server_members change.

CREATE OR REPLACE FUNCTION public.sync_server_member_count()
RETURNS TRIGGER AS $$
DECLARE
    target_server_id TEXT;
BEGIN
    IF TG_OP = 'DELETE' THEN
        target_server_id := OLD.server_id;
    ELSE
        target_server_id := NEW.server_id;
    END IF;

    UPDATE public.servers
    SET member_count = (
        SELECT COUNT(*) FROM public.server_members WHERE server_id = target_server_id
    ),
    updated_at = timezone('utc'::text, now())
    WHERE id = target_server_id;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trg_sync_server_member_count ON public.server_members;

CREATE TRIGGER trg_sync_server_member_count
AFTER INSERT OR UPDATE OR DELETE ON public.server_members
FOR EACH ROW EXECUTE FUNCTION public.sync_server_member_count();

-- Recalculate member_count for all existing servers
UPDATE public.servers s
SET member_count = (
    SELECT COUNT(*) FROM public.server_members m WHERE m.server_id = s.id
);
