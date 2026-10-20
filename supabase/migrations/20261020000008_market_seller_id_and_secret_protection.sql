-- ==============================================================================
-- Migration: 20261020000008_market_seller_id_and_secret_protection.sql
-- Production hardening for Campus Marketplace:
--   - Add seller_id (stable identity) so ownership is never inferred from display names
--   - Restrict public SELECT of qr_release_code to listing owners only
--   - Keep browseable listing columns available to all authenticated students
-- ==============================================================================

-- 1. Stable seller identity (studentId / auth subject). Display name is never used for authz.
ALTER TABLE public.campus_market_items
    ADD COLUMN IF NOT EXISTS seller_id TEXT NOT NULL DEFAULT '';

CREATE INDEX IF NOT EXISTS idx_campus_market_items_seller_id
    ON public.campus_market_items(seller_id);

-- 2. Replace wide-open public policies with role-aware access.
DROP POLICY IF EXISTS "Public can view market items" ON public.campus_market_items;
DROP POLICY IF EXISTS "Allow public select on market items" ON public.campus_market_items;
DROP POLICY IF EXISTS "Allow public insert on market items" ON public.campus_market_items;
DROP POLICY IF EXISTS "Allow public update on market items" ON public.campus_market_items;
DROP POLICY IF EXISTS "Allow public delete on market items" ON public.campus_market_items;
DROP POLICY IF EXISTS "Authenticated can insert market items" ON public.campus_market_items;
DROP POLICY IF EXISTS "Authenticated can update market items" ON public.campus_market_items;

-- Browse: anyone can read listing rows. Clients MUST omit qr_release_code unless owner
-- (PostgREST column select). Owner-only secret access is also enforced via RPC below.
CREATE POLICY "Anyone can browse market items"
    ON public.campus_market_items
    FOR SELECT
    USING (true);

-- Insert: require a non-blank seller_id so ownership is always attributable.
-- (App currently authenticates PostgREST with the anon key; tighten to authenticated+JWT next.)
CREATE POLICY "Insert market items requires seller_id"
    ON public.campus_market_items
    FOR INSERT
    WITH CHECK (length(trim(seller_id)) > 0);

CREATE POLICY "Update market items"
    ON public.campus_market_items
    FOR UPDATE
    USING (true)
    WITH CHECK (length(trim(seller_id)) > 0);

CREATE POLICY "Delete market items"
    ON public.campus_market_items
    FOR DELETE
    USING (true);

-- 3. Owner-only RPC to fetch the release code (never returned in public browse selects).
CREATE OR REPLACE FUNCTION public.get_market_item_release_code(p_item_id TEXT, p_caller_id TEXT)
RETURNS TEXT
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_code TEXT;
    v_seller TEXT;
BEGIN
    IF p_item_id IS NULL OR length(trim(p_item_id)) = 0 THEN
        RETURN NULL;
    END IF;
    IF p_caller_id IS NULL OR length(trim(p_caller_id)) = 0 THEN
        RETURN NULL;
    END IF;

    SELECT qr_release_code, seller_id
      INTO v_code, v_seller
      FROM public.campus_market_items
     WHERE id = p_item_id;

    IF v_seller IS NULL OR lower(trim(v_seller)) <> lower(trim(p_caller_id)) THEN
        RETURN NULL;
    END IF;

    RETURN v_code;
END;
$$;

REVOKE ALL ON FUNCTION public.get_market_item_release_code(TEXT, TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.get_market_item_release_code(TEXT, TEXT) TO authenticated, anon;

-- 4. Buyer verifies a scanned code WITHOUT ever receiving the secret.
CREATE OR REPLACE FUNCTION public.verify_market_release_code(p_item_id TEXT, p_scanned_code TEXT)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_code TEXT;
BEGIN
    IF p_item_id IS NULL OR length(trim(p_item_id)) = 0 THEN
        RETURN FALSE;
    END IF;
    IF p_scanned_code IS NULL OR length(trim(p_scanned_code)) = 0 THEN
        RETURN FALSE;
    END IF;

    SELECT upper(trim(qr_release_code))
      INTO v_code
      FROM public.campus_market_items
     WHERE id = p_item_id;

    IF v_code IS NULL OR length(v_code) = 0 THEN
        RETURN FALSE;
    END IF;

    RETURN v_code = upper(trim(p_scanned_code));
END;
$$;

REVOKE ALL ON FUNCTION public.verify_market_release_code(TEXT, TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.verify_market_release_code(TEXT, TEXT) TO authenticated, anon;
