-- Profile registration must run only for a real Supabase Auth identity.
-- Using auth.users.email as the source of truth prevents caller-supplied email
-- addresses from being used to reserve another user's campus profile.
CREATE OR REPLACE FUNCTION public.ensure_campusprofile_for_auth_user(
    p_full_name TEXT DEFAULT NULL,
    p_avatar_url TEXT DEFAULT NULL
)
RETURNS SETOF public.campusprofile
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    current_user_id UUID := auth.uid();
    current_email TEXT;
    clean_name TEXT;
BEGIN
    IF current_user_id IS NULL THEN
        RAISE EXCEPTION 'Authentication is required' USING ERRCODE = '42501';
    END IF;

    SELECT LOWER(TRIM(u.email))
    INTO current_email
    FROM auth.users AS u
    WHERE u.id = current_user_id;

    IF current_email IS NULL OR current_email = '' THEN
        RAISE EXCEPTION 'Authenticated account has no email address' USING ERRCODE = '22023';
    END IF;

    clean_name := NULLIF(TRIM(p_full_name), '');

    RETURN QUERY
    INSERT INTO public.campusprofile AS profile (
        user_id,
        student_id,
        full_name,
        university_email,
        avatar_url
    )
    VALUES (
        current_user_id,
        'STD-' || UPPER(SUBSTRING(REPLACE(current_user_id::TEXT, '-', '') FROM 1 FOR 12)),
        COALESCE(clean_name, current_email),
        current_email,
        NULLIF(TRIM(p_avatar_url), '')
    )
    ON CONFLICT (user_id) DO UPDATE
    SET full_name = COALESCE(clean_name, profile.full_name),
        avatar_url = COALESCE(NULLIF(TRIM(p_avatar_url), ''), profile.avatar_url),
        university_email = current_email
    RETURNING profile.*;
END;
$$;

REVOKE ALL ON FUNCTION public.ensure_campusprofile_for_auth_user(TEXT, TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.ensure_campusprofile_for_auth_user(TEXT, TEXT) TO authenticated;

-- Keep Auth-user creation and profile creation in the same database transaction.
-- Do not catch uniqueness errors here: a conflicting profile email must reject
-- Auth signup rather than leaving a second auth.users account without a profile.
CREATE OR REPLACE FUNCTION public.handle_new_auth_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    clean_email TEXT := LOWER(TRIM(COALESCE(NEW.email, '')));
BEGIN
    IF clean_email = '' THEN
        RAISE EXCEPTION 'Email is required to create a campus profile' USING ERRCODE = '22023';
    END IF;

    INSERT INTO public.campusprofile (
        user_id,
        student_id,
        full_name,
        university_email,
        avatar_url
    )
    VALUES (
        NEW.id,
        'STD-' || UPPER(SUBSTRING(REPLACE(NEW.id::TEXT, '-', '') FROM 1 FOR 12)),
        COALESCE(
            NULLIF(TRIM(NEW.raw_user_meta_data ->> 'full_name'), ''),
            NULLIF(TRIM(NEW.raw_user_meta_data ->> 'name'), ''),
            clean_email
        ),
        clean_email,
        COALESCE(
            NULLIF(TRIM(NEW.raw_user_meta_data ->> 'avatar_url'), ''),
            NULLIF(TRIM(NEW.raw_user_meta_data ->> 'picture'), '')
        )
    )
    ON CONFLICT (user_id) DO UPDATE
    SET university_email = EXCLUDED.university_email;

    RETURN NEW;
END;
$$;

-- Remove anonymous access to campus profile data and writes. Authenticated
-- reads remain available for the app's current cross-campus profile features.
DROP POLICY IF EXISTS "Profiles can be looked up by anon and authenticated" ON public.campusprofile;
DROP POLICY IF EXISTS "Allow profile creation on registration" ON public.campusprofile;
DROP POLICY IF EXISTS "Allow profile update for owners" ON public.campusprofile;
DROP POLICY IF EXISTS "Users can insert their own profile" ON public.campusprofile;
DROP POLICY IF EXISTS "Users can update their own profile" ON public.campusprofile;
DROP POLICY IF EXISTS "Public profiles are viewable by authenticated users" ON public.campusprofile;

CREATE POLICY "Authenticated users can view campus profiles"
    ON public.campusprofile FOR SELECT TO authenticated USING (true);

CREATE POLICY "Users can insert their own campus profile"
    ON public.campusprofile FOR INSERT TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own campus profile"
    ON public.campusprofile FOR UPDATE TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete their own campus profile"
    ON public.campusprofile FOR DELETE TO authenticated
    USING (auth.uid() = user_id);