-- Migration: 20261020000004_create_server_atomic_rpc.sql
-- Enables atomic "one-send" server creation with all default members, channels, rules, and invites.

CREATE OR REPLACE FUNCTION public.create_server_atomic(
    p_server_id TEXT,
    p_name TEXT,
    p_category TEXT,
    p_description TEXT DEFAULT '',
    p_owner_id TEXT DEFAULT '',
    p_owner_display_name TEXT DEFAULT 'Campus Founder',
    p_owner_handle TEXT DEFAULT '@founder',
    p_owner_avatar_url TEXT DEFAULT NULL,
    p_avatar_res_name TEXT DEFAULT 'server_codesistency',
    p_banner_res_name TEXT DEFAULT NULL,
    p_invite_code TEXT DEFAULT ''
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_now_ms BIGINT;
    v_clean_name TEXT;
    v_invite_code TEXT;
    v_server RECORD;
    v_result jsonb;
BEGIN
    v_now_ms := (EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::BIGINT;
    v_clean_name := TRIM(COALESCE(p_name, 'New Server'));
    IF v_clean_name = '' THEN
        v_clean_name := 'New Server';
    END IF;

    IF p_invite_code IS NULL OR p_invite_code = '' THEN
        v_invite_code := UPPER(REGEXP_REPLACE(v_clean_name, '[^a-zA-Z0-9]', '-', 'g')) || '-' || UPPER(SUBSTRING(gen_random_uuid()::TEXT, 1, 6));
    ELSE
        v_invite_code := p_invite_code;
    END IF;

    -- 1. Insert Server
    INSERT INTO public.servers (
        id,
        name,
        category,
        description,
        owner_id,
        owner_display_name,
        owner_handle,
        owner_avatar_url,
        avatar_res_name,
        banner_res_name,
        is_verified,
        member_count,
        created_at_epoch_ms
    ) VALUES (
        p_server_id,
        v_clean_name,
        COALESCE(NULLIF(p_category, ''), 'Community'),
        COALESCE(p_description, ''),
        COALESCE(p_owner_id, ''),
        COALESCE(p_owner_display_name, 'Campus Founder'),
        COALESCE(p_owner_handle, '@founder'),
        p_owner_avatar_url,
        COALESCE(NULLIF(p_avatar_res_name, ''), 'server_codesistency'),
        p_banner_res_name,
        false,
        1,
        v_now_ms
    )
    ON CONFLICT (id) DO UPDATE SET
        name = EXCLUDED.name,
        category = EXCLUDED.category,
        description = EXCLUDED.description,
        owner_id = EXCLUDED.owner_id,
        owner_display_name = EXCLUDED.owner_display_name,
        owner_handle = EXCLUDED.owner_handle,
        owner_avatar_url = EXCLUDED.owner_avatar_url,
        avatar_res_name = EXCLUDED.avatar_res_name,
        banner_res_name = EXCLUDED.banner_res_name;

    -- 2. Insert Owner as Member
    IF p_owner_id IS NOT NULL AND p_owner_id <> '' THEN
        INSERT INTO public.server_members (
            id,
            server_id,
            user_id,
            display_name,
            handle,
            role,
            role_title,
            role_color_hex,
            avatar_url,
            avatar_res_name,
            is_online,
            custom_status,
            joined_at_epoch_ms,
            badge
        ) VALUES (
            p_server_id || ':' || p_owner_id,
            p_server_id,
            p_owner_id,
            COALESCE(p_owner_display_name, 'Campus Founder'),
            COALESCE(p_owner_handle, '@founder'),
            'OWNER',
            'Server Owner',
            4278205288, -- 0xFF003B68
            p_owner_avatar_url,
            COALESCE(NULLIF(p_avatar_res_name, ''), 'server_codesistency'),
            true,
            'Server Founder',
            v_now_ms,
            'Owner'
        )
        ON CONFLICT (server_id, user_id) DO UPDATE SET
            role = 'OWNER',
            role_title = 'Server Owner',
            display_name = EXCLUDED.display_name,
            handle = EXCLUDED.handle,
            avatar_url = EXCLUDED.avatar_url,
            avatar_res_name = EXCLUDED.avatar_res_name;
    END IF;

    -- 3. Insert Default Channels (Welcome: rules, announcements; Text: general)
    INSERT INTO public.server_channels (
        id,
        server_id,
        name,
        category_name,
        channel_type,
        topic,
        is_unread,
        order_index,
        created_at_epoch_ms
    ) VALUES 
    (
        'ch_' || p_server_id || '_rules',
        p_server_id,
        'rules',
        'WELCOME',
        'rules',
        'Official server rules and safety policies.',
        false,
        1,
        v_now_ms
    ),
    (
        'ch_' || p_server_id || '_announcements',
        p_server_id,
        'announcements',
        'WELCOME',
        'announcements',
        'Updates, events, and broadcast announcements.',
        false,
        2,
        v_now_ms
    ),
    (
        'ch_' || p_server_id || '_general',
        p_server_id,
        'general',
        'TEXT CHANNELS',
        'text',
        'General discussion and hangout lounge.',
        false,
        3,
        v_now_ms
    )
    ON CONFLICT (server_id, name) DO NOTHING;

    -- 4. Insert Default Rules
    INSERT INTO public.server_rules (
        id,
        server_id,
        rule_index,
        title,
        description
    ) VALUES
    (
        'rule_' || p_server_id || '_1',
        p_server_id,
        1,
        'Be Respectful',
        'Maintain civility and respect toward all campus peers and community members.'
    ),
    (
        'rule_' || p_server_id || '_2',
        p_server_id,
        2,
        'No Spam or Unsolicited Promotion',
        'Keep chat channels relevant to the server topics and avoid spamming links.'
    ),
    (
        'rule_' || p_server_id || '_3',
        p_server_id,
        3,
        'Follow Moderation Directions',
        'Direct any concerns to server admins or moderators.'
    )
    ON CONFLICT (server_id, rule_index) DO NOTHING;

    -- 5. Insert Server Invite
    INSERT INTO public.server_invites (
        code,
        server_id,
        server_name,
        inviter_id,
        inviter_name,
        expires_at_epoch_ms,
        max_uses,
        uses_count,
        is_revoked
    ) VALUES (
        v_invite_code,
        p_server_id,
        v_clean_name,
        COALESCE(p_owner_id, ''),
        COALESCE(p_owner_display_name, 'Campus Founder'),
        9223372036854775807,
        0,
        1,
        false
    )
    ON CONFLICT (code) DO NOTHING;

    -- Return the unified server bundle
    SELECT jsonb_build_object(
        'id', p_server_id,
        'name', v_clean_name,
        'category', COALESCE(NULLIF(p_category, ''), 'Community'),
        'description', COALESCE(p_description, ''),
        'owner_id', COALESCE(p_owner_id, ''),
        'owner_display_name', COALESCE(p_owner_display_name, 'Campus Founder'),
        'owner_handle', COALESCE(p_owner_handle, '@founder'),
        'owner_avatar_url', p_owner_avatar_url,
        'avatar_res_name', COALESCE(NULLIF(p_avatar_res_name, ''), 'server_codesistency'),
        'banner_res_name', p_banner_res_name,
        'invite_code', v_invite_code,
        'member_count', 1,
        'created_at_epoch_ms', v_now_ms
    ) INTO v_result;

    RETURN v_result;
END;
$$;
