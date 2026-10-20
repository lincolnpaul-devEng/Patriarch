-- Prevent a single email from having multiple campusprofile rows.
-- Existing duplicates must be reconciled manually; never delete user profiles
-- automatically because they may be linked to distinct Supabase Auth users.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public.campusprofile
        WHERE TRIM(university_email) <> ''
        GROUP BY LOWER(TRIM(university_email))
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION
            'Duplicate campusprofile emails exist. Reconcile the rows and their auth.users identities before applying this migration.';
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS idx_campusprofile_unique_normalized_email
    ON public.campusprofile (LOWER(TRIM(university_email)))
    WHERE TRIM(university_email) <> '';