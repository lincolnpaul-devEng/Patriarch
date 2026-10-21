-- Names on social content are snapshots only; clients must resolve them from campusprofile.
ALTER TABLE public.campus_flash_meets
    ADD COLUMN IF NOT EXISTS creator_id TEXT;
ALTER TABLE public.campus_micro_gigs
    ADD COLUMN IF NOT EXISTS poster_id TEXT;

ALTER TABLE public.campus_market_items
    ALTER COLUMN seller_name SET DEFAULT '';
UPDATE public.campus_market_items
SET seller_name = ''
WHERE NOT public.is_real_campusprofile_name(seller_name);
ALTER TABLE public.campus_market_items
    DROP CONSTRAINT IF EXISTS campus_market_items_real_seller_name;
ALTER TABLE public.campus_market_items
    ADD CONSTRAINT campus_market_items_real_seller_name
    CHECK (seller_name = '' OR public.is_real_campusprofile_name(seller_name));

ALTER TABLE public.campus_micro_gigs
    ALTER COLUMN poster_name SET DEFAULT '';
UPDATE public.campus_micro_gigs
SET poster_name = ''
WHERE NOT public.is_real_campusprofile_name(poster_name);
ALTER TABLE public.campus_micro_gigs
    DROP CONSTRAINT IF EXISTS campus_micro_gigs_real_poster_name;
ALTER TABLE public.campus_micro_gigs
    ADD CONSTRAINT campus_micro_gigs_real_poster_name
    CHECK (poster_name = '' OR public.is_real_campusprofile_name(poster_name));

ALTER TABLE public.campus_flash_meets
    ALTER COLUMN creator_name SET DEFAULT '';
UPDATE public.campus_flash_meets
SET creator_name = ''
WHERE NOT public.is_real_campusprofile_name(creator_name);
ALTER TABLE public.campus_flash_meets
    DROP CONSTRAINT IF EXISTS campus_flash_meets_real_creator_name;
ALTER TABLE public.campus_flash_meets
    ADD CONSTRAINT campus_flash_meets_real_creator_name
    CHECK (creator_name = '' OR public.is_real_campusprofile_name(creator_name));

ALTER TABLE public.campus_live_streams
    ALTER COLUMN broadcaster_name SET DEFAULT '';
UPDATE public.campus_live_streams
SET broadcaster_name = ''
WHERE NOT public.is_real_campusprofile_name(broadcaster_name);
ALTER TABLE public.campus_live_streams
    DROP CONSTRAINT IF EXISTS campus_live_streams_real_broadcaster_name;
ALTER TABLE public.campus_live_streams
    ADD CONSTRAINT campus_live_streams_real_broadcaster_name
    CHECK (broadcaster_name = '' OR public.is_real_campusprofile_name(broadcaster_name));

ALTER TABLE public.campus_stream_comments
    ALTER COLUMN sender_name SET DEFAULT '';
UPDATE public.campus_stream_comments
SET sender_name = ''
WHERE NOT public.is_real_campusprofile_name(sender_name);
ALTER TABLE public.campus_stream_comments
    DROP CONSTRAINT IF EXISTS campus_stream_comments_real_sender_name;
ALTER TABLE public.campus_stream_comments
    ADD CONSTRAINT campus_stream_comments_real_sender_name
    CHECK (sender_name = '' OR public.is_real_campusprofile_name(sender_name));

ALTER TABLE public.campus_server_channel_messages
    ALTER COLUMN sender_name SET DEFAULT '';
UPDATE public.campus_server_channel_messages
SET sender_name = ''
WHERE NOT public.is_real_campusprofile_name(sender_name);
ALTER TABLE public.campus_server_channel_messages
    DROP CONSTRAINT IF EXISTS campus_server_channel_messages_real_sender_name;
ALTER TABLE public.campus_server_channel_messages
    ADD CONSTRAINT campus_server_channel_messages_real_sender_name
    CHECK (sender_name = '' OR public.is_real_campusprofile_name(sender_name));

ALTER TABLE public.campus_chat_messages
    ALTER COLUMN sender_name SET DEFAULT '';
UPDATE public.campus_chat_messages
SET sender_name = ''
WHERE NOT public.is_real_campusprofile_name(sender_name);
ALTER TABLE public.campus_chat_messages
    DROP CONSTRAINT IF EXISTS campus_chat_messages_real_sender_name;
ALTER TABLE public.campus_chat_messages
    ADD CONSTRAINT campus_chat_messages_real_sender_name
    CHECK (sender_name = '' OR public.is_real_campusprofile_name(sender_name));

ALTER TABLE public.campus_channel_typing
    ALTER COLUMN display_name SET DEFAULT '';
UPDATE public.campus_channel_typing
SET display_name = ''
WHERE NOT public.is_real_campusprofile_name(display_name);
ALTER TABLE public.campus_channel_typing
    DROP CONSTRAINT IF EXISTS campus_channel_typing_real_display_name;
ALTER TABLE public.campus_channel_typing
    ADD CONSTRAINT campus_channel_typing_real_display_name
    CHECK (display_name = '' OR public.is_real_campusprofile_name(display_name));

ALTER TABLE public.server_members
    ALTER COLUMN display_name SET DEFAULT '';
UPDATE public.server_members
SET display_name = ''
WHERE NOT public.is_real_campusprofile_name(display_name);
ALTER TABLE public.server_members
    DROP CONSTRAINT IF EXISTS server_members_real_display_name;
ALTER TABLE public.server_members
    ADD CONSTRAINT server_members_real_display_name
    CHECK (display_name = '' OR public.is_real_campusprofile_name(display_name));

ALTER TABLE public.servers
    ALTER COLUMN owner_display_name SET DEFAULT '';
UPDATE public.servers
SET owner_display_name = ''
WHERE NOT public.is_real_campusprofile_name(owner_display_name);
ALTER TABLE public.servers
    DROP CONSTRAINT IF EXISTS servers_real_owner_display_name;
ALTER TABLE public.servers
    ADD CONSTRAINT servers_real_owner_display_name
    CHECK (owner_display_name = '' OR public.is_real_campusprofile_name(owner_display_name));

ALTER TABLE public.server_invites
    ALTER COLUMN inviter_name SET DEFAULT '';
UPDATE public.server_invites
SET inviter_name = ''
WHERE NOT public.is_real_campusprofile_name(inviter_name);
ALTER TABLE public.server_invites
    DROP CONSTRAINT IF EXISTS server_invites_real_inviter_name;
ALTER TABLE public.server_invites
    ADD CONSTRAINT server_invites_real_inviter_name
    CHECK (inviter_name = '' OR public.is_real_campusprofile_name(inviter_name));

ALTER TABLE public.campus_call_sessions
    ALTER COLUMN caller_name SET DEFAULT '';
ALTER TABLE public.campus_call_sessions
    ALTER COLUMN callee_name SET DEFAULT '';
UPDATE public.campus_call_sessions
SET caller_name = CASE
        WHEN public.is_real_campusprofile_name(caller_name) THEN caller_name
        ELSE ''
    END,
    callee_name = CASE
        WHEN public.is_real_campusprofile_name(callee_name) THEN callee_name
        ELSE ''
    END;
ALTER TABLE public.campus_call_sessions
    DROP CONSTRAINT IF EXISTS campus_call_sessions_real_names;
ALTER TABLE public.campus_call_sessions
    ADD CONSTRAINT campus_call_sessions_real_names
    CHECK (
        (caller_name = '' OR public.is_real_campusprofile_name(caller_name))
        AND (callee_name = '' OR public.is_real_campusprofile_name(callee_name))
    );

COMMENT ON COLUMN public.campus_flash_meets.creator_id IS
    'Stable profile identifier used to resolve creator_name from campusprofile.full_name.';
COMMENT ON COLUMN public.campus_micro_gigs.poster_id IS
    'Stable profile identifier used to resolve poster_name from campusprofile.full_name.';
