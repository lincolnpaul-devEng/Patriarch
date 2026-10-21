-- Keep campusprofile.full_name as the authoritative source for person names.
CREATE OR REPLACE FUNCTION public.is_real_campusprofile_name(profile_name TEXT)
RETURNS BOOLEAN
LANGUAGE SQL
IMMUTABLE
SET search_path = ''
AS $$
    SELECT NULLIF(BTRIM(profile_name), '') IS NOT NULL
        AND POSITION('@' IN profile_name) = 0
        AND LOWER(BTRIM(profile_name)) NOT IN (
            'campus peer',
            'campus member',
            'campus student',
            'friend',
            'guest',
            'member',
            'new user',
            'peer',
            'student',
            'student peer',
            'student user',
            'unknown',
            'unknown user',
            'verified student',
            'you'
        );
$$;

-- Recover names from Auth metadata where possible; unresolved legacy profiles remain
-- inaccessible to name-based UI until the owner supplies a real name.
UPDATE public.campusprofile AS profile
SET full_name = COALESCE(
    NULLIF(BTRIM(auth_user.raw_user_meta_data ->> 'full_name'), ''),
    NULLIF(BTRIM(auth_user.raw_user_meta_data ->> 'name'), '')
)
FROM auth.users AS auth_user
WHERE profile.user_id = auth_user.id
  AND NOT public.is_real_campusprofile_name(profile.full_name)
  AND public.is_real_campusprofile_name(
      COALESCE(
          NULLIF(BTRIM(auth_user.raw_user_meta_data ->> 'full_name'), ''),
          NULLIF(BTRIM(auth_user.raw_user_meta_data ->> 'name'), '')
      )
  );

ALTER TABLE public.campusprofile
    ALTER COLUMN full_name DROP DEFAULT;

CREATE OR REPLACE FUNCTION public.enforce_real_campusprofile_name()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = ''
AS $$
BEGIN
    IF NEW.full_name IS NULL OR NOT public.is_real_campusprofile_name(NEW.full_name) THEN
        RAISE EXCEPTION 'A real profile full_name is required'
            USING ERRCODE = '22023';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS enforce_real_campusprofile_name_insert ON public.campusprofile;
DROP TRIGGER IF EXISTS enforce_real_campusprofile_name_update ON public.campusprofile;
CREATE TRIGGER enforce_real_campusprofile_name_insert
    BEFORE INSERT ON public.campusprofile
    FOR EACH ROW
    EXECUTE FUNCTION public.enforce_real_campusprofile_name();
CREATE TRIGGER enforce_real_campusprofile_name_update
    BEFORE UPDATE OF full_name ON public.campusprofile
    FOR EACH ROW
    EXECUTE FUNCTION public.enforce_real_campusprofile_name();

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
    auth_name TEXT;
    clean_name TEXT := NULLIF(BTRIM(p_full_name), '');
BEGIN
    IF current_user_id IS NULL THEN
        RAISE EXCEPTION 'Authentication is required' USING ERRCODE = '42501';
    END IF;

    SELECT
        LOWER(TRIM(auth_user.email)),
        COALESCE(
            NULLIF(BTRIM(auth_user.raw_user_meta_data ->> 'full_name'), ''),
            NULLIF(BTRIM(auth_user.raw_user_meta_data ->> 'name'), '')
        )
    INTO current_email, auth_name
    FROM auth.users AS auth_user
    WHERE auth_user.id = current_user_id;

    IF current_email IS NULL OR current_email = '' THEN
        RAISE EXCEPTION 'Authenticated account has no email address' USING ERRCODE = '22023';
    END IF;

    IF clean_name IS NULL THEN
        clean_name := auth_name;
    END IF;

    IF clean_name IS NULL THEN
        SELECT NULLIF(BTRIM(profile.full_name), '')
        INTO clean_name
        FROM public.campusprofile AS profile
        WHERE profile.user_id = current_user_id;
    END IF;

    IF clean_name IS NULL OR NOT public.is_real_campusprofile_name(clean_name) THEN
        RAISE EXCEPTION 'A real profile full_name is required' USING ERRCODE = '22023';
    END IF;

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
        clean_name,
        current_email,
        NULLIF(TRIM(p_avatar_url), '')
    )
    ON CONFLICT (user_id) DO UPDATE
    SET full_name = clean_name,
        avatar_url = COALESCE(NULLIF(TRIM(p_avatar_url), ''), profile.avatar_url),
        university_email = current_email
    RETURNING profile.*;
END;
$$;

CREATE OR REPLACE FUNCTION public.handle_new_auth_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    clean_email TEXT := LOWER(TRIM(COALESCE(NEW.email, '')));
    clean_name TEXT := COALESCE(
        NULLIF(BTRIM(NEW.raw_user_meta_data ->> 'full_name'), ''),
        NULLIF(BTRIM(NEW.raw_user_meta_data ->> 'name'), '')
    );
BEGIN
    IF clean_email = '' THEN
        RAISE EXCEPTION 'Email is required to create a campus profile' USING ERRCODE = '22023';
    END IF;

    IF clean_name IS NULL OR NOT public.is_real_campusprofile_name(clean_name) THEN
        RAISE EXCEPTION 'A real profile full_name is required' USING ERRCODE = '22023';
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
        clean_name,
        clean_email,
        COALESCE(
            NULLIF(TRIM(NEW.raw_user_meta_data ->> 'avatar_url'), ''),
            NULLIF(TRIM(NEW.raw_user_meta_data ->> 'picture'), '')
        )
    )
    ON CONFLICT (user_id) DO UPDATE
    SET university_email = EXCLUDED.university_email,
        full_name = CASE
        WHEN public.is_real_campusprofile_name(campusprofile.full_name)
                THEN campusprofile.full_name
            ELSE EXCLUDED.full_name
        END;

    RETURN NEW;
END;
$$;

ALTER TABLE public.campus_friends
    ALTER COLUMN friend_display_name SET DEFAULT '';

UPDATE public.campus_friends
SET friend_display_name = ''
WHERE NOT public.is_real_campusprofile_name(friend_display_name);

ALTER TABLE public.campus_friends
    DROP CONSTRAINT IF EXISTS campus_friends_real_display_name;
ALTER TABLE public.campus_friends
    ADD CONSTRAINT campus_friends_real_display_name
    CHECK (
        friend_display_name = ''
        OR public.is_real_campusprofile_name(friend_display_name)
    );

COMMENT ON COLUMN public.campus_friends.friend_display_name IS
    'Legacy display-name snapshot; current display names must be resolved from campusprofile.full_name.';

ALTER TABLE public.campus_notifications
    ALTER COLUMN actor_name SET DEFAULT '';

UPDATE public.campus_notifications
SET actor_name = ''
WHERE NOT public.is_real_campusprofile_name(actor_name);

ALTER TABLE public.campus_notifications
    DROP CONSTRAINT IF EXISTS campus_notifications_real_actor_name;
ALTER TABLE public.campus_notifications
    ADD CONSTRAINT campus_notifications_real_actor_name
    CHECK (
        actor_name = ''
        OR public.is_real_campusprofile_name(actor_name)
    );

COMMENT ON COLUMN public.campus_notifications.actor_name IS
    'Profile-name snapshot; clients resolve the current display name from campusprofile.full_name.';

ALTER TABLE public.campus_dm_typing
    ALTER COLUMN display_name SET DEFAULT '';

UPDATE public.campus_dm_typing
SET display_name = ''
WHERE NOT public.is_real_campusprofile_name(display_name);

ALTER TABLE public.campus_dm_typing
    DROP CONSTRAINT IF EXISTS campus_dm_typing_real_display_name;
ALTER TABLE public.campus_dm_typing
    ADD CONSTRAINT campus_dm_typing_real_display_name
    CHECK (
        display_name = ''
        OR public.is_real_campusprofile_name(display_name)
    );
