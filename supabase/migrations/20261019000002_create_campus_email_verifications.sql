-- =============================================================================
-- Migration: Create campus_email_verifications table for Resend OTP email verification
-- =============================================================================

CREATE TABLE IF NOT EXISTS public.campus_email_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT NOT NULL,
    otp_code TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_campus_email_verifications_lookup 
    ON public.campus_email_verifications(email, otp_code, verified);

CREATE INDEX IF NOT EXISTS idx_campus_email_verifications_email 
    ON public.campus_email_verifications(email);

ALTER TABLE public.campus_email_verifications ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow anon and auth to check and insert verification" ON public.campus_email_verifications;

CREATE POLICY "Allow anon and auth to check and insert verification" 
    ON public.campus_email_verifications 
    FOR ALL 
    USING (true) 
    WITH CHECK (true);
