-- =============================================================================
-- Account storage usage: ledger + RPCs for real Patriarch cloud quota reporting.
-- Tracks Cloudinary-backed user media against campusprofile storage quotas.
-- =============================================================================

CREATE TABLE IF NOT EXISTS public.campus_user_media_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    category TEXT NOT NULL DEFAULT 'other'
        CHECK (category IN (
            'avatar',
            'market',
            'chat_media',
            'server',
            'stream',
            'document',
            'cache',
            'other'
        )),
    asset_url TEXT NOT NULL,
    public_id TEXT,
    bytes BIGINT NOT NULL DEFAULT 0 CHECK (bytes >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT uq_campus_user_media_assets_user_url UNIQUE (user_id, asset_url)
);

CREATE INDEX IF NOT EXISTS idx_campus_user_media_assets_user
    ON public.campus_user_media_assets(user_id);
CREATE INDEX IF NOT EXISTS idx_campus_user_media_assets_user_category
    ON public.campus_user_media_assets(user_id, category);

ALTER TABLE public.campus_user_media_assets ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Owners read own media assets" ON public.campus_user_media_assets;
CREATE POLICY "Owners read own media assets"
    ON public.campus_user_media_assets FOR SELECT TO authenticated
    USING (user_id = (SELECT auth.uid()));

DROP POLICY IF EXISTS "Owners insert own media assets" ON public.campus_user_media_assets;
CREATE POLICY "Owners insert own media assets"
    ON public.campus_user_media_assets FOR INSERT TO authenticated
    WITH CHECK (user_id = (SELECT auth.uid()));

DROP POLICY IF EXISTS "Owners update own media assets" ON public.campus_user_media_assets;
CREATE POLICY "Owners update own media assets"
    ON public.campus_user_media_assets FOR UPDATE TO authenticated
    USING (user_id = (SELECT auth.uid()))
    WITH CHECK (user_id = (SELECT auth.uid()));

DROP POLICY IF EXISTS "Owners delete own media assets" ON public.campus_user_media_assets;
CREATE POLICY "Owners delete own media assets"
    ON public.campus_user_media_assets FOR DELETE TO authenticated
    USING (user_id = (SELECT auth.uid()));

GRANT SELECT, INSERT, UPDATE, DELETE ON public.campus_user_media_assets TO authenticated;

-- Keep storage_used_bytes / storage_quota_bytes server-managed (clients cannot forge usage).
CREATE OR REPLACE FUNCTION public.reject_client_profile_privilege_changes()
RETURNS TRIGGER LANGUAGE plpgsql SET search_path = '' AS $$
BEGIN
  IF auth.uid() IS NOT NULL AND (
    NEW.user_id IS DISTINCT FROM OLD.user_id OR
    NEW.student_id IS DISTINCT FROM OLD.student_id OR
    NEW.university_email IS DISTINCT FROM OLD.university_email OR
    NEW.wallet_balance IS DISTINCT FROM OLD.wallet_balance OR
    NEW.vault_balance IS DISTINCT FROM OLD.vault_balance OR
    NEW.campus_merit_points IS DISTINCT FROM OLD.campus_merit_points OR
    NEW.behavioral_trust_score IS DISTINCT FROM OLD.behavioral_trust_score OR
    NEW.is_verified_student IS DISTINCT FROM OLD.is_verified_student OR
    NEW.storage_used_bytes IS DISTINCT FROM OLD.storage_used_bytes OR
    NEW.storage_quota_bytes IS DISTINCT FROM OLD.storage_quota_bytes
  ) THEN
    RAISE EXCEPTION 'Server-managed profile fields cannot be updated by clients' USING ERRCODE = '42501';
  END IF;
  RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION public.sync_profile_storage_used(p_user_id UUID)
RETURNS BIGINT
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_used BIGINT;
BEGIN
    SELECT COALESCE(SUM(bytes), 0)::BIGINT
    INTO v_used
    FROM public.campus_user_media_assets
    WHERE user_id = p_user_id;

    UPDATE public.campusprofile
    SET storage_used_bytes = v_used,
        updated_at = timezone('utc'::text, now())
    WHERE user_id = p_user_id;

    RETURN v_used;
END;
$$;

REVOKE ALL ON FUNCTION public.sync_profile_storage_used(UUID) FROM PUBLIC;

CREATE OR REPLACE FUNCTION public.register_user_media_asset(
    p_asset_url TEXT,
    p_bytes BIGINT DEFAULT 0,
    p_category TEXT DEFAULT 'other',
    p_public_id TEXT DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_category TEXT := lower(coalesce(nullif(trim(p_category), ''), 'other'));
    v_url TEXT := nullif(trim(p_asset_url), '');
    v_bytes BIGINT := GREATEST(coalesce(p_bytes, 0), 0);
    v_quota BIGINT;
    v_used BIGINT;
    v_row public.campus_user_media_assets%ROWTYPE;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Sign in required' USING ERRCODE = '42501';
    END IF;
    IF v_url IS NULL THEN
        RAISE EXCEPTION 'asset_url is required' USING ERRCODE = '22023';
    END IF;
    IF v_category NOT IN ('avatar', 'market', 'chat_media', 'server', 'stream', 'document', 'cache', 'other') THEN
        v_category := 'other';
    END IF;

    SELECT storage_quota_bytes, storage_used_bytes
    INTO v_quota, v_used
    FROM public.campusprofile
    WHERE user_id = v_uid
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Campus profile not found' USING ERRCODE = 'P0002';
    END IF;

    INSERT INTO public.campus_user_media_assets (user_id, category, asset_url, public_id, bytes)
    VALUES (v_uid, v_category, v_url, nullif(trim(p_public_id), ''), v_bytes)
    ON CONFLICT (user_id, asset_url) DO UPDATE
    SET
        category = EXCLUDED.category,
        public_id = COALESCE(EXCLUDED.public_id, campus_user_media_assets.public_id),
        bytes = CASE
            WHEN EXCLUDED.bytes > 0 THEN EXCLUDED.bytes
            ELSE campus_user_media_assets.bytes
        END,
        updated_at = timezone('utc'::text, now())
    RETURNING * INTO v_row;

    v_used := public.sync_profile_storage_used(v_uid);

    SELECT storage_quota_bytes INTO v_quota
    FROM public.campusprofile
    WHERE user_id = v_uid;

    RETURN jsonb_build_object(
        'id', v_row.id,
        'category', v_row.category,
        'asset_url', v_row.asset_url,
        'bytes', v_row.bytes,
        'storage_used_bytes', v_used,
        'storage_quota_bytes', COALESCE(v_quota, 1073741824)
    );
END;
$$;

REVOKE ALL ON FUNCTION public.register_user_media_asset(TEXT, BIGINT, TEXT, TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.register_user_media_asset(TEXT, BIGINT, TEXT, TEXT) TO authenticated;

CREATE OR REPLACE FUNCTION public.set_user_media_asset_bytes(
    p_asset_url TEXT,
    p_bytes BIGINT
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_url TEXT := nullif(trim(p_asset_url), '');
    v_bytes BIGINT := GREATEST(coalesce(p_bytes, 0), 0);
    v_used BIGINT;
    v_quota BIGINT;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Sign in required' USING ERRCODE = '42501';
    END IF;
    IF v_url IS NULL THEN
        RAISE EXCEPTION 'asset_url is required' USING ERRCODE = '22023';
    END IF;

    UPDATE public.campus_user_media_assets
    SET bytes = v_bytes,
        updated_at = timezone('utc'::text, now())
    WHERE user_id = v_uid
      AND asset_url = v_url;

    v_used := public.sync_profile_storage_used(v_uid);
    SELECT storage_quota_bytes INTO v_quota FROM public.campusprofile WHERE user_id = v_uid;

    RETURN jsonb_build_object(
        'storage_used_bytes', v_used,
        'storage_quota_bytes', COALESCE(v_quota, 1073741824)
    );
END;
$$;

REVOKE ALL ON FUNCTION public.set_user_media_asset_bytes(TEXT, BIGINT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.set_user_media_asset_bytes(TEXT, BIGINT) TO authenticated;

-- Discover owned Cloudinary / media URLs already referenced in product tables.
CREATE OR REPLACE FUNCTION public.reconcile_my_media_assets()
RETURNS INT
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_student_id TEXT;
    v_email TEXT;
    v_inserted INT := 0;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Sign in required' USING ERRCODE = '42501';
    END IF;

    SELECT student_id, university_email
    INTO v_student_id, v_email
    FROM public.campusprofile
    WHERE user_id = v_uid;

    WITH discovered AS (
        SELECT 'avatar'::TEXT AS category, avatar_url AS asset_url
        FROM public.campusprofile
        WHERE user_id = v_uid
          AND avatar_url IS NOT NULL
          AND avatar_url LIKE 'http%'

        UNION ALL
        SELECT 'market', image_url
        FROM public.campus_market_items
        WHERE image_url IS NOT NULL
          AND image_url LIKE 'http%'
          AND lower(seller_id) IN (
              lower(v_uid::text),
              lower(coalesce(v_student_id, '')),
              lower(coalesce(v_email, ''))
          )

        UNION ALL
        SELECT 'stream', backdrop_image_url
        FROM public.campus_live_streams
        WHERE backdrop_image_url IS NOT NULL
          AND backdrop_image_url LIKE 'http%'
          AND lower(broadcaster_id) IN (
              lower(v_uid::text),
              lower(coalesce(v_student_id, '')),
              lower(coalesce(v_email, ''))
          )

        UNION ALL
        SELECT 'chat_media', image_url
        FROM public.campus_chat_messages
        WHERE image_url IS NOT NULL
          AND image_url LIKE 'http%'
          AND lower(sender_id) IN (
              lower(v_uid::text),
              lower(coalesce(v_student_id, '')),
              lower(coalesce(v_email, ''))
          )

        UNION ALL
        SELECT 'chat_media', gif_url
        FROM public.campus_chat_messages
        WHERE gif_url IS NOT NULL
          AND gif_url LIKE 'http%'
          AND lower(sender_id) IN (
              lower(v_uid::text),
              lower(coalesce(v_student_id, '')),
              lower(coalesce(v_email, ''))
          )

        UNION ALL
        SELECT 'chat_media', elem
        FROM public.campus_server_channel_messages m
        CROSS JOIN LATERAL jsonb_array_elements_text(
            CASE
                WHEN m.image_urls IS NULL THEN '[]'::jsonb
                WHEN jsonb_typeof(m.image_urls) = 'array' THEN m.image_urls
                ELSE '[]'::jsonb
            END
        ) AS elem
        WHERE elem LIKE 'http%'
          AND lower(m.sender_id) IN (
              lower(v_uid::text),
              lower(coalesce(v_student_id, '')),
              lower(coalesce(v_email, ''))
          )

        UNION ALL
        SELECT 'chat_media', gif_url
        FROM public.campus_server_channel_messages
        WHERE gif_url IS NOT NULL
          AND gif_url LIKE 'http%'
          AND lower(sender_id) IN (
              lower(v_uid::text),
              lower(coalesce(v_student_id, '')),
              lower(coalesce(v_email, ''))
          )

        UNION ALL
        SELECT 'server', owner_avatar_url
        FROM public.servers
        WHERE owner_avatar_url IS NOT NULL
          AND owner_avatar_url LIKE 'http%'
          AND lower(owner_id) IN (
              lower(v_uid::text),
              lower(coalesce(v_student_id, '')),
              lower(coalesce(v_email, ''))
          )

        UNION ALL
        SELECT 'server', avatar_res_name
        FROM public.servers
        WHERE avatar_res_name IS NOT NULL
          AND avatar_res_name LIKE 'http%'
          AND lower(owner_id) IN (
              lower(v_uid::text),
              lower(coalesce(v_student_id, '')),
              lower(coalesce(v_email, ''))
          )

        UNION ALL
        SELECT 'server', banner_res_name
        FROM public.servers
        WHERE banner_res_name IS NOT NULL
          AND banner_res_name LIKE 'http%'
          AND lower(owner_id) IN (
              lower(v_uid::text),
              lower(coalesce(v_student_id, '')),
              lower(coalesce(v_email, ''))
          )
    ),
    cleaned AS (
        SELECT DISTINCT category, asset_url
        FROM discovered
        WHERE asset_url IS NOT NULL
          AND length(trim(asset_url)) > 8
          AND asset_url LIKE 'http%'
    ),
    upserted AS (
        INSERT INTO public.campus_user_media_assets (user_id, category, asset_url, bytes)
        SELECT v_uid, category, asset_url, 0
        FROM cleaned
        ON CONFLICT (user_id, asset_url) DO UPDATE
        SET category = EXCLUDED.category,
            updated_at = timezone('utc'::text, now())
        WHERE campus_user_media_assets.category IS DISTINCT FROM EXCLUDED.category
        RETURNING 1
    )
    SELECT COUNT(*)::INT INTO v_inserted FROM upserted;

    PERFORM public.sync_profile_storage_used(v_uid);
    RETURN v_inserted;
END;
$$;

REVOKE ALL ON FUNCTION public.reconcile_my_media_assets() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.reconcile_my_media_assets() TO authenticated;

CREATE OR REPLACE FUNCTION public.get_my_account_storage()
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_used BIGINT;
    v_quota BIGINT;
    v_categories JSONB;
    v_pending JSONB;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Sign in required' USING ERRCODE = '42501';
    END IF;

    PERFORM public.reconcile_my_media_assets();

    SELECT storage_used_bytes, storage_quota_bytes
    INTO v_used, v_quota
    FROM public.campusprofile
    WHERE user_id = v_uid;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Campus profile not found' USING ERRCODE = 'P0002';
    END IF;

    SELECT COALESCE(jsonb_agg(
        jsonb_build_object(
            'category', category,
            'bytes', bytes,
            'item_count', item_count
        )
        ORDER BY bytes DESC
    ), '[]'::jsonb)
    INTO v_categories
    FROM (
        SELECT category,
               COALESCE(SUM(bytes), 0)::BIGINT AS bytes,
               COUNT(*)::INT AS item_count
        FROM public.campus_user_media_assets
        WHERE user_id = v_uid
        GROUP BY category
    ) s;

    SELECT COALESCE(jsonb_agg(asset_url), '[]'::jsonb)
    INTO v_pending
    FROM (
        SELECT asset_url
        FROM public.campus_user_media_assets
        WHERE user_id = v_uid
          AND bytes = 0
        ORDER BY created_at DESC
        LIMIT 50
    ) pending;

    RETURN jsonb_build_object(
        'storage_used_bytes', COALESCE(v_used, 0),
        'storage_quota_bytes', COALESCE(v_quota, 1073741824),
        'categories', v_categories,
        'pending_byte_urls', v_pending
    );
END;
$$;

REVOKE ALL ON FUNCTION public.get_my_account_storage() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.get_my_account_storage() TO authenticated;

CREATE OR REPLACE FUNCTION public.upgrade_my_storage_quota(
    p_additional_bytes BIGINT DEFAULT 1073741824
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_additional BIGINT := GREATEST(coalesce(p_additional_bytes, 0), 0);
    v_used BIGINT;
    v_quota BIGINT;
    v_max_quota BIGINT := 5 * 1073741824; -- hard cap 5 GB for student tier upgrades
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Sign in required' USING ERRCODE = '42501';
    END IF;
    IF v_additional <= 0 OR v_additional > 1073741824 THEN
        RAISE EXCEPTION 'Invalid upgrade amount' USING ERRCODE = '22023';
    END IF;

    UPDATE public.campusprofile
    SET storage_quota_bytes = LEAST(storage_quota_bytes + v_additional, v_max_quota),
        updated_at = timezone('utc'::text, now())
    WHERE user_id = v_uid
    RETURNING storage_used_bytes, storage_quota_bytes
    INTO v_used, v_quota;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Campus profile not found' USING ERRCODE = 'P0002';
    END IF;

    RETURN jsonb_build_object(
        'storage_used_bytes', v_used,
        'storage_quota_bytes', v_quota
    );
END;
$$;

REVOKE ALL ON FUNCTION public.upgrade_my_storage_quota(BIGINT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.upgrade_my_storage_quota(BIGINT) TO authenticated;
