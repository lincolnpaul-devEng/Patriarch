-- Migration: 20261023000004_discord_server_lifecycle_and_transfer.sql
-- Implements Discord-standard server lifecycle:
-- 1. If an account being deleted owns a server with other members, ownership is transferred
--    to the next senior member/moderator.
-- 2. If a server has no other members (solo owner), the server is cleanly deleted.
-- 3. Departing members are cleanly removed from server_members without affecting the server.

CREATE OR REPLACE FUNCTION public.delete_own_account_data(target_user_id UUID DEFAULT auth.uid())
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, pg_temp AS $$
DECLARE
  v_uid UUID;
  v_student_id TEXT;
  v_email TEXT;
  v_owned_server RECORD;
  v_successor_id TEXT;
BEGIN
  -- Determine target user ID (defaults to current authenticated user)
  v_uid := coalesce(target_user_id, auth.uid());
  IF v_uid IS NULL THEN
    RAISE EXCEPTION 'Not authenticated' USING ERRCODE = '42501';
  END IF;

  -- Allow users to delete their own data, or allow service_role to delete any user
  IF auth.role() IS DISTINCT FROM 'service_role' AND auth.uid() IS DISTINCT FROM v_uid THEN
    RAISE EXCEPTION 'Cannot delete another user''s data' USING ERRCODE = '42501';
  END IF;

  -- Fetch student_id and university_email before profile removal
  SELECT student_id, university_email INTO v_student_id, v_email
  FROM public.campusprofile
  WHERE user_id = v_uid;

  -- 1. Ephemeral & notification data
  DELETE FROM public.campus_device_push_tokens WHERE user_id = v_uid::text;
  DELETE FROM public.campus_dm_typing WHERE user_id = v_uid::text;
  DELETE FROM public.campus_channel_typing WHERE user_id = v_uid::text;
  DELETE FROM public.campus_notifications WHERE recipient_user_id = v_uid::text;
  UPDATE public.campus_notifications
  SET actor_user_id = '', actor_name = 'Campus peer'
  WHERE actor_user_id = v_uid::text
     OR (v_student_id IS NOT NULL AND actor_user_id = v_student_id);

  -- 2. Social & messaging
  DELETE FROM public.campus_friends
  WHERE user_id = v_uid::text OR friend_user_id = v_uid::text
     OR (v_student_id IS NOT NULL AND (user_id = v_student_id OR friend_user_id = v_student_id));

  DELETE FROM public.campus_chat_messages
  WHERE sender_id = v_uid::text
     OR (v_student_id IS NOT NULL AND sender_id = v_student_id);

  DELETE FROM public.campus_server_channel_messages
  WHERE sender_id = v_uid::text
     OR (v_student_id IS NOT NULL AND sender_id = v_student_id);

  DELETE FROM public.campus_call_sessions
  WHERE caller_id = v_uid::text OR callee_id = v_uid::text;

  -- 3. Market & commerce
  DELETE FROM public.campus_escrow_orders
  WHERE buyer_user_id = v_uid::text OR seller_user_id = v_uid::text
     OR (v_student_id IS NOT NULL AND (buyer_user_id = v_student_id OR seller_user_id = v_student_id));

  DELETE FROM public.campus_saved_listings WHERE user_id = v_uid;

  DELETE FROM public.campus_market_items
  WHERE seller_id = v_uid::text
     OR (v_student_id IS NOT NULL AND seller_id = v_student_id);

  DELETE FROM public.campus_micro_gigs
  WHERE poster_id = v_uid::text
     OR (v_student_id IS NOT NULL AND poster_id = v_student_id);

  -- 4. Events, Meets, and Streams
  DELETE FROM public.campus_events
  WHERE creator_id = v_uid::text
     OR (v_student_id IS NOT NULL AND creator_id = v_student_id);

  DELETE FROM public.campus_flash_meets
  WHERE creator_id = v_uid::text
     OR (v_student_id IS NOT NULL AND creator_id = v_student_id);

  DELETE FROM public.campus_stream_comments
  WHERE sender_id = v_uid::text
     OR (v_student_id IS NOT NULL AND sender_id = v_student_id);

  DELETE FROM public.campus_live_streams
  WHERE broadcaster_id = v_uid::text
     OR (v_student_id IS NOT NULL AND broadcaster_id = v_student_id);

  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'campus_streamer_profiles'
  ) THEN
    DELETE FROM public.campus_streamer_profiles
    WHERE student_id = v_uid::text
       OR (v_student_id IS NOT NULL AND student_id = v_student_id);
  END IF;

  -- 5. Servers: Discord-standard decoupling of Server entity from creator account
  -- Revoke invites created by departing user
  DELETE FROM public.server_invites
  WHERE inviter_id = v_uid::text
     OR (v_student_id IS NOT NULL AND inviter_id = v_student_id);

  FOR v_owned_server IN
    SELECT id FROM public.servers
    WHERE owner_id = v_uid::text
       OR (v_student_id IS NOT NULL AND owner_id = v_student_id)
  LOOP
    -- Look for the next senior member (prioritizing MODERATOR, then earliest joined)
    SELECT user_id INTO v_successor_id
    FROM public.server_members
    WHERE server_id = v_owned_server.id
      AND user_id <> v_uid::text
      AND (v_student_id IS NULL OR user_id <> v_student_id)
    ORDER BY (role = 'MODERATOR') DESC, joined_at_epoch_ms ASC
    LIMIT 1;

    IF v_successor_id IS NOT NULL THEN
      -- Option A (Discord Strict): Transfer ownership to successor
      UPDATE public.servers
      SET owner_id = v_successor_id,
          updated_at = timezone('utc'::text, now())
      WHERE id = v_owned_server.id;

      UPDATE public.server_members
      SET role = 'OWNER',
          role_title = 'Server Owner',
          updated_at = timezone('utc'::text, now())
      WHERE server_id = v_owned_server.id AND user_id = v_successor_id;

      -- Remove departing user from this server
      DELETE FROM public.server_members
      WHERE server_id = v_owned_server.id
        AND (user_id = v_uid::text OR (v_student_id IS NOT NULL AND user_id = v_student_id));

      DELETE FROM public.per_server_profiles
      WHERE server_id = v_owned_server.id
        AND (user_id = v_uid::text OR (v_student_id IS NOT NULL AND user_id = v_student_id));
    ELSE
      -- Option B (Solo Server Deletion): No other members exist, delete the empty server
      DELETE FROM public.server_invites WHERE server_id = v_owned_server.id;
      DELETE FROM public.server_rules WHERE server_id = v_owned_server.id;
      DELETE FROM public.campus_server_channel_messages
      WHERE channel_id IN (SELECT id FROM public.server_channels WHERE server_id = v_owned_server.id);
      DELETE FROM public.server_channels WHERE server_id = v_owned_server.id;
      DELETE FROM public.per_server_profiles WHERE server_id = v_owned_server.id;
      DELETE FROM public.server_members WHERE server_id = v_owned_server.id;
      DELETE FROM public.servers WHERE id = v_owned_server.id;
    END IF;
  END LOOP;

  -- Remove departing user from any servers they joined as a member
  DELETE FROM public.server_members
  WHERE user_id = v_uid::text OR (v_student_id IS NOT NULL AND user_id = v_student_id);

  DELETE FROM public.per_server_profiles
  WHERE user_id = v_uid::text OR (v_student_id IS NOT NULL AND user_id = v_student_id);

  -- 6. Email verifications
  IF v_email IS NOT NULL AND v_email <> '' THEN
    DELETE FROM public.campus_email_verifications WHERE email = v_email;
  END IF;

  -- 7. Profile row
  DELETE FROM public.campusprofile WHERE user_id = v_uid;

  RETURN json_build_object(
    'status', 'success',
    'user_id', v_uid
  );
END;
$$;

REVOKE ALL ON FUNCTION public.delete_own_account_data(UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.delete_own_account_data(UUID) TO authenticated, service_role;
