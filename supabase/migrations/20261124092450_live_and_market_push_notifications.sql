-- =============================================================================
-- Live stream + marketplace interest push notifications
-- Fan-out rows into campus_notifications for the Android poller / deep links.
-- =============================================================================

CREATE TABLE IF NOT EXISTS public.campus_market_search_interests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    query_term TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT '',
    hit_count INTEGER NOT NULL DEFAULT 1 CHECK (hit_count > 0),
    last_searched_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT uq_campus_market_search_interests_user_term
        UNIQUE (user_id, query_term)
);

CREATE INDEX IF NOT EXISTS idx_campus_market_search_interests_user
    ON public.campus_market_search_interests(user_id, last_searched_at DESC);
CREATE INDEX IF NOT EXISTS idx_campus_market_search_interests_term
    ON public.campus_market_search_interests(query_term);

ALTER TABLE public.campus_market_search_interests ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Owners manage market search interests" ON public.campus_market_search_interests;
CREATE POLICY "Owners manage market search interests"
    ON public.campus_market_search_interests FOR ALL TO authenticated
    USING (user_id = (SELECT auth.uid()))
    WITH CHECK (user_id = (SELECT auth.uid()));

GRANT SELECT, INSERT, UPDATE, DELETE ON public.campus_market_search_interests TO authenticated;

CREATE OR REPLACE FUNCTION public.normalize_market_search_term(p_raw TEXT)
RETURNS TEXT
LANGUAGE sql
IMMUTABLE
SET search_path = public
AS $$
    SELECT lower(trim(regexp_replace(coalesce(p_raw, ''), '\s+', ' ', 'g')));
$$;

REVOKE ALL ON FUNCTION public.normalize_market_search_term(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.normalize_market_search_term(TEXT) TO authenticated;

CREATE OR REPLACE FUNCTION public.record_market_search_interest(
    p_query TEXT,
    p_category TEXT DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_term TEXT := public.normalize_market_search_term(p_query);
    v_category TEXT := coalesce(nullif(trim(p_category), ''), '');
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Sign in required' USING ERRCODE = '42501';
    END IF;
    IF v_term IS NULL OR length(v_term) < 2 THEN
        RETURN jsonb_build_object('recorded', false, 'reason', 'query_too_short');
    END IF;
    -- Ignore ultra-generic noise.
    IF v_term IN ('a', 'an', 'the', 'of', 'for', 'and', 'or', 'to', 'in', 'on') THEN
        RETURN jsonb_build_object('recorded', false, 'reason', 'ignored_term');
    END IF;

    INSERT INTO public.campus_market_search_interests (user_id, query_term, category, hit_count, last_searched_at)
    VALUES (v_uid, v_term, v_category, 1, timezone('utc'::text, now()))
    ON CONFLICT (user_id, query_term) DO UPDATE
    SET hit_count = campus_market_search_interests.hit_count + 1,
        category = CASE
            WHEN EXCLUDED.category <> '' THEN EXCLUDED.category
            ELSE campus_market_search_interests.category
        END,
        last_searched_at = timezone('utc'::text, now());

    RETURN jsonb_build_object('recorded', true, 'query_term', v_term);
END;
$$;

REVOKE ALL ON FUNCTION public.record_market_search_interest(TEXT, TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.record_market_search_interest(TEXT, TEXT) TO authenticated;

-- Resolve campus identity aliases for the authenticated user.
CREATE OR REPLACE FUNCTION public.current_campus_identity_keys()
RETURNS TEXT[]
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT ARRAY(
        SELECT DISTINCT lower(trim(key))
        FROM (
            SELECT auth.uid()::text AS key
            UNION ALL
            SELECT p.user_id::text
            FROM public.campusprofile p
            WHERE p.user_id = auth.uid()
            UNION ALL
            SELECT p.student_id
            FROM public.campusprofile p
            WHERE p.user_id = auth.uid() AND p.student_id IS NOT NULL AND length(trim(p.student_id)) > 0
            UNION ALL
            SELECT p.university_email
            FROM public.campusprofile p
            WHERE p.user_id = auth.uid() AND p.university_email IS NOT NULL AND length(trim(p.university_email)) > 0
        ) keys
        WHERE key IS NOT NULL AND length(trim(key)) > 0
    );
$$;

REVOKE ALL ON FUNCTION public.current_campus_identity_keys() FROM PUBLIC;

CREATE OR REPLACE FUNCTION public.notify_friends_of_live_stream(
    p_stream_id TEXT,
    p_title TEXT,
    p_category TEXT DEFAULT 'CAMPUS LIVE',
    p_viewer_count INTEGER DEFAULT 1
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_actor_name TEXT;
    v_keys TEXT[];
    v_friend_ids TEXT[];
    v_inserted INT := 0;
    v_now_ms BIGINT := (extract(epoch from timezone('utc'::text, now())) * 1000)::bigint;
    v_title TEXT := coalesce(nullif(trim(p_title), ''), 'Campus Live Stream');
    v_category TEXT := coalesce(nullif(trim(p_category), ''), 'CAMPUS LIVE');
    v_stream_id TEXT := nullif(trim(p_stream_id), '');
    v_viewers INT := GREATEST(coalesce(p_viewer_count, 1), 1);
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Sign in required' USING ERRCODE = '42501';
    END IF;
    IF v_stream_id IS NULL THEN
        RAISE EXCEPTION 'stream_id is required' USING ERRCODE = '22023';
    END IF;

    SELECT coalesce(nullif(trim(full_name), ''), 'Campus peer')
    INTO v_actor_name
    FROM public.campusprofile
    WHERE user_id = v_uid;

    v_keys := public.current_campus_identity_keys();

    SELECT coalesce(array_agg(DISTINCT lower(trim(friend_id))), ARRAY[]::text[])
    INTO v_friend_ids
    FROM (
        SELECT f.friend_user_id AS friend_id
        FROM public.campus_friends f
        WHERE f.status = 'FRIENDS'
          AND lower(trim(f.user_id)) = ANY (v_keys)
        UNION
        SELECT f.user_id AS friend_id
        FROM public.campus_friends f
        WHERE f.status = 'FRIENDS'
          AND lower(trim(f.friend_user_id)) = ANY (v_keys)
    ) friends
    WHERE friend_id IS NOT NULL
      AND length(trim(friend_id)) > 0
      AND lower(trim(friend_id)) <> ALL (v_keys);

    IF coalesce(array_length(v_friend_ids, 1), 0) = 0 THEN
        RETURN jsonb_build_object('notified', 0, 'stream_id', v_stream_id);
    END IF;

    INSERT INTO public.campus_notifications (
        id, recipient_user_id, actor_user_id, actor_name, type, title, body,
        thread_id, payload_json, is_read, created_at_epoch_ms
    )
    SELECT
        'notif_' || gen_random_uuid()::text,
        friend_id,
        v_uid::text,
        v_actor_name,
        'LIVE_STREAM',
        v_actor_name || ' is live',
        v_actor_name || ' started "' || v_title || '". Tap to watch live.',
        v_stream_id,
        jsonb_build_object(
            'stream_id', v_stream_id,
            'title', v_title,
            'category', v_category,
            'viewer_count', v_viewers
        ),
        false,
        v_now_ms
    FROM unnest(v_friend_ids) AS friend_id;

    GET DIAGNOSTICS v_inserted = ROW_COUNT;
    RETURN jsonb_build_object('notified', v_inserted, 'stream_id', v_stream_id);
END;
$$;

REVOKE ALL ON FUNCTION public.notify_friends_of_live_stream(TEXT, TEXT, TEXT, INTEGER) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.notify_friends_of_live_stream(TEXT, TEXT, TEXT, INTEGER) TO authenticated;

CREATE OR REPLACE FUNCTION public.notify_market_interest_matches(
    p_item_id TEXT,
    p_title TEXT,
    p_category TEXT DEFAULT '',
    p_price DOUBLE PRECISION DEFAULT 0,
    p_currency TEXT DEFAULT 'KES',
    p_image_url TEXT DEFAULT NULL,
    p_is_update BOOLEAN DEFAULT false
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_actor_name TEXT;
    v_keys TEXT[];
    v_item_id TEXT := nullif(trim(p_item_id), '');
    v_title TEXT := coalesce(nullif(trim(p_title), ''), 'Campus listing');
    v_category TEXT := coalesce(nullif(trim(p_category), ''), '');
    v_currency TEXT := coalesce(nullif(trim(p_currency), ''), 'KES');
    v_now_ms BIGINT := (extract(epoch from timezone('utc'::text, now())) * 1000)::bigint;
    v_inserted INT := 0;
    v_haystack TEXT;
BEGIN
    IF v_uid IS NULL THEN
        RAISE EXCEPTION 'Sign in required' USING ERRCODE = '42501';
    END IF;
    IF v_item_id IS NULL THEN
        RAISE EXCEPTION 'item_id is required' USING ERRCODE = '22023';
    END IF;

    SELECT coalesce(nullif(trim(full_name), ''), 'Campus seller')
    INTO v_actor_name
    FROM public.campusprofile
    WHERE user_id = v_uid;

    v_keys := public.current_campus_identity_keys();
    v_haystack := lower(v_title || ' ' || v_category);

    WITH matched AS (
        SELECT DISTINCT i.user_id::text AS recipient_id, i.query_term
        FROM public.campus_market_search_interests i
        WHERE i.user_id IS DISTINCT FROM v_uid
          AND i.last_searched_at >= (timezone('utc'::text, now()) - interval '90 days')
          AND (
              position(i.query_term in v_haystack) > 0
              OR (i.category <> '' AND lower(i.category) = lower(v_category))
          )
        UNION
        -- Also match onboarding favorite categories stored on campusprofile.
        SELECT p.user_id::text AS recipient_id, fav AS query_term
        FROM public.campusprofile p
        CROSS JOIN LATERAL jsonb_array_elements_text(
            CASE
                WHEN jsonb_typeof(p.onboarding_preferences -> 'favoriteCategories') = 'array'
                    THEN p.onboarding_preferences -> 'favoriteCategories'
                ELSE '[]'::jsonb
            END
        ) AS fav
        WHERE p.user_id IS DISTINCT FROM v_uid
          AND length(trim(fav)) >= 2
          AND (
              position(lower(trim(fav)) in v_haystack) > 0
              OR lower(trim(fav)) = lower(v_category)
          )
    ),
    deduped AS (
        SELECT DISTINCT recipient_id
        FROM matched
        WHERE recipient_id IS NOT NULL
          AND length(trim(recipient_id)) > 0
          AND lower(trim(recipient_id)) <> ALL (v_keys)
    )
    INSERT INTO public.campus_notifications (
        id, recipient_user_id, actor_user_id, actor_name, type, title, body,
        thread_id, payload_json, is_read, created_at_epoch_ms
    )
    SELECT
        'notif_' || gen_random_uuid()::text,
        d.recipient_id,
        v_uid::text,
        v_actor_name,
        'MARKET_LISTING',
        CASE WHEN p_is_update THEN 'Market update' ELSE 'New on Campus Market' END,
        CASE
            WHEN p_is_update THEN
                v_actor_name || ' updated "' || v_title || '" · ' || v_currency || ' ' ||
                trim(to_char(coalesce(p_price, 0), 'FM999999990.00'))
            ELSE
                '"' || v_title || '" just listed · ' || v_currency || ' ' ||
                trim(to_char(coalesce(p_price, 0), 'FM999999990.00')) ||
                CASE WHEN v_category <> '' THEN ' · ' || v_category ELSE '' END
        END,
        v_item_id,
        jsonb_build_object(
            'item_id', v_item_id,
            'title', v_title,
            'category', v_category,
            'price', coalesce(p_price, 0),
            'currency', v_currency,
            'image_url', p_image_url,
            'is_update', coalesce(p_is_update, false)
        ),
        false,
        v_now_ms
    FROM deduped d;

    GET DIAGNOSTICS v_inserted = ROW_COUNT;
    RETURN jsonb_build_object('notified', v_inserted, 'item_id', v_item_id);
END;
$$;

REVOKE ALL ON FUNCTION public.notify_market_interest_matches(TEXT, TEXT, TEXT, DOUBLE PRECISION, TEXT, TEXT, BOOLEAN) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.notify_market_interest_matches(TEXT, TEXT, TEXT, DOUBLE PRECISION, TEXT, TEXT, BOOLEAN) TO authenticated;
