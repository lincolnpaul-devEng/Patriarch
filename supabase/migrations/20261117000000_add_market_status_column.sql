-- ==============================================================================
-- Migration: 20261117000000_add_market_status_column.sql
-- Marketplace availability lifecycle for campus_market_items:
--   ACTIVE   — browseable, buyable
--   RESERVED — an escrow order holds funds for this listing (server-owned state)
--   SOLD     — trade completed or seller marked it sold
--
-- Rules:
--   * Only the server (edge function, service_role) may move a listing to
--     RESERVED or out of RESERVED. The app can never fake an escrow hold.
--   * The seller may toggle their own listing ACTIVE <-> SOLD.
--   * The server may perform any transition (release -> SOLD, refund -> ACTIVE).
-- ==============================================================================

ALTER TABLE public.campus_market_items
    ADD COLUMN IF NOT EXISTS status TEXT NOT NULL DEFAULT 'ACTIVE';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'campus_market_items_status_chk'
    ) THEN
        ALTER TABLE public.campus_market_items
            ADD CONSTRAINT campus_market_items_status_chk
            CHECK (status IN ('ACTIVE', 'RESERVED', 'SOLD'));
    END IF;
END;
$$;

-- Existing rows adopt 'ACTIVE' via the column default; backfill defensively anyway.
UPDATE public.campus_market_items SET status = 'ACTIVE' WHERE status IS NULL OR status = '';

CREATE INDEX IF NOT EXISTS idx_campus_market_items_status
    ON public.campus_market_items(status);

CREATE INDEX IF NOT EXISTS idx_campus_market_items_active_ts
    ON public.campus_market_items(timestamp_ms DESC)
    WHERE status = 'ACTIVE';

-- Ownership/role guard on status transitions.
CREATE OR REPLACE FUNCTION public.guard_market_item_status()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        IF NEW.status = 'RESERVED' THEN
            IF coalesce(auth.role(), '') <> 'service_role' THEN
                RAISE EXCEPTION 'Only the payment server may reserve a listing';
            END IF;
        ELSIF NEW.status = 'SOLD' THEN
            IF coalesce(auth.role(), '') <> 'service_role'
               AND NOT public.is_current_campus_user(NEW.seller_id) THEN
                RAISE EXCEPTION 'Only the seller or the payment server may mark a listing sold';
            END IF;
        ELSIF NEW.status = 'ACTIVE' THEN
            IF OLD.status = 'RESERVED'
               AND coalesce(auth.role(), '') <> 'service_role' THEN
                RAISE EXCEPTION 'Only the payment server may un-reserve a listing';
            END IF;
            -- ACTIVE from SOLD: seller un-marking or server refund. Both allowed.
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS campus_market_items_status_guard ON public.campus_market_items;
CREATE TRIGGER campus_market_items_status_guard
    BEFORE UPDATE ON public.campus_market_items
    FOR EACH ROW
    EXECUTE FUNCTION public.guard_market_item_status();
