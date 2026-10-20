-- Migration: Deduplicate and enforce unique lowercase email on campusprofile
-- Eliminates duplicate user registrations and enforces strict email uniqueness

-- 1. Deduplicate any existing duplicate rows keeping the most recently created row
DELETE FROM public.campusprofile a
USING public.campusprofile b
WHERE a.id < b.id
  AND LOWER(TRIM(a.university_email)) = LOWER(TRIM(b.university_email))
  AND a.university_email <> '';

-- 2. Create unique index on LOWER(TRIM(university_email))
CREATE UNIQUE INDEX IF NOT EXISTS idx_campusprofile_unique_email
ON public.campusprofile(LOWER(TRIM(university_email)))
WHERE university_email <> '';
