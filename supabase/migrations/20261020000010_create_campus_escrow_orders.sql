-- ==============================================================================
-- Migration: 20261020000010_create_campus_escrow_orders.sql
-- Production SafeTrade escrow ledger keyed by stable student/user ids + payment socket.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS public.campus_escrow_orders (
    id TEXT PRIMARY KEY,
    order_number TEXT UNIQUE NOT NULL,
    market_item_id TEXT NOT NULL,
    buyer_user_id TEXT NOT NULL,
    seller_user_id TEXT NOT NULL,
    item_title TEXT NOT NULL,
    amount DOUBLE PRECISION NOT NULL CHECK (amount >= 0),
    currency_code TEXT NOT NULL DEFAULT 'KES',
    payment_provider TEXT NOT NULL,
    payment_method TEXT NOT NULL DEFAULT '',
    provider_hold_id TEXT,
    status TEXT NOT NULL DEFAULT 'PENDING_PAYMENT',
    rendezvous_spot TEXT NOT NULL DEFAULT '',
    buyer_note TEXT NOT NULL DEFAULT '',
    created_at_epoch_ms BIGINT NOT NULL DEFAULT 0,
    funded_at_epoch_ms BIGINT,
    released_at_epoch_ms BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_campus_escrow_orders_buyer
    ON public.campus_escrow_orders(buyer_user_id);
CREATE INDEX IF NOT EXISTS idx_campus_escrow_orders_seller
    ON public.campus_escrow_orders(seller_user_id);
CREATE INDEX IF NOT EXISTS idx_campus_escrow_orders_item
    ON public.campus_escrow_orders(market_item_id);
CREATE INDEX IF NOT EXISTS idx_campus_escrow_orders_status
    ON public.campus_escrow_orders(status);
CREATE INDEX IF NOT EXISTS idx_campus_escrow_orders_hold
    ON public.campus_escrow_orders(provider_hold_id);

ALTER TABLE public.campus_escrow_orders ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Anyone can browse escrow orders" ON public.campus_escrow_orders;
DROP POLICY IF EXISTS "Insert escrow orders" ON public.campus_escrow_orders;
DROP POLICY IF EXISTS "Update escrow orders" ON public.campus_escrow_orders;

-- Tighten to JWT-scoped participant checks once the app always sends user JWT.
CREATE POLICY "Anyone can browse escrow orders"
    ON public.campus_escrow_orders FOR SELECT USING (true);

CREATE POLICY "Insert escrow orders"
    ON public.campus_escrow_orders FOR INSERT
    WITH CHECK (
        length(trim(buyer_user_id)) > 0
        AND length(trim(seller_user_id)) > 0
        AND length(trim(payment_provider)) > 0
    );

CREATE POLICY "Update escrow orders"
    ON public.campus_escrow_orders FOR UPDATE
    USING (true)
    WITH CHECK (length(trim(status)) > 0);

ALTER TABLE public.campus_escrow_orders REPLICA IDENTITY FULL;
DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.campus_escrow_orders;
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN undefined_object THEN NULL;
END $$;
