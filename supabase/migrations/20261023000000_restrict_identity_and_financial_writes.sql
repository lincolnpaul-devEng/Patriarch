-- Forward-only hardening. Apply after the Android release that sends Supabase user JWTs.
-- Previously deployed public policies are removed, never edited in place.

CREATE OR REPLACE FUNCTION public.is_current_campus_user(p_user_id TEXT)
RETURNS BOOLEAN LANGUAGE sql STABLE SECURITY DEFINER SET search_path = '' AS $$
  SELECT auth.uid() IS NOT NULL AND EXISTS (
    SELECT 1 FROM public.campusprofile p
    WHERE p.user_id = auth.uid()
      AND lower(trim(p_user_id)) IN (
        lower(p.user_id::text), lower(p.student_id), lower(p.university_email)
      )
  );
$$;
REVOKE ALL ON FUNCTION public.is_current_campus_user(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.is_current_campus_user(TEXT) TO authenticated;

-- Profile provisioning is handled by the auth.users trigger / authenticated RPC.
DROP POLICY IF EXISTS "Profiles can be looked up by anon and authenticated" ON public.campusprofile;
DROP POLICY IF EXISTS "Allow profile creation on registration" ON public.campusprofile;
DROP POLICY IF EXISTS "Allow profile update for owners" ON public.campusprofile;
DROP POLICY IF EXISTS "Authenticated users can view campus profiles" ON public.campusprofile;
DROP POLICY IF EXISTS "Users can insert their own campus profile" ON public.campusprofile;
DROP POLICY IF EXISTS "Users can update their own campus profile" ON public.campusprofile;
DROP POLICY IF EXISTS "Users can delete their own campus profile" ON public.campusprofile;
CREATE POLICY "Authenticated profiles" ON public.campusprofile FOR SELECT TO authenticated USING (true);
CREATE POLICY "Owner profile edits" ON public.campusprofile FOR UPDATE TO authenticated
  USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());
CREATE POLICY "Owner profile deletion" ON public.campusprofile FOR DELETE TO authenticated
  USING (user_id = auth.uid());

-- RLS alone cannot stop an owner setting their own wallet/trust balance; protect
-- server-managed and identity columns even when using PostgREST PATCH.
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
    NEW.is_verified_student IS DISTINCT FROM OLD.is_verified_student
  ) THEN
    RAISE EXCEPTION 'Server-managed profile fields cannot be updated by clients' USING ERRCODE = '42501';
  END IF;
  RETURN NEW;
END;
$$;
DROP TRIGGER IF EXISTS reject_client_profile_privilege_changes ON public.campusprofile;
CREATE TRIGGER reject_client_profile_privilege_changes BEFORE UPDATE ON public.campusprofile
  FOR EACH ROW EXECUTE FUNCTION public.reject_client_profile_privilege_changes();

-- Only members can see/write their channel's messages; sender identity is bound
-- to the authenticated profile (not a client-supplied display name).
DROP POLICY IF EXISTS "Select server channel messages" ON public.campus_server_channel_messages;
DROP POLICY IF EXISTS "Insert server channel messages" ON public.campus_server_channel_messages;
DROP POLICY IF EXISTS "Update server channel messages" ON public.campus_server_channel_messages;
DROP POLICY IF EXISTS "Delete server channel messages" ON public.campus_server_channel_messages;
CREATE POLICY "Member reads channel messages" ON public.campus_server_channel_messages
  FOR SELECT TO authenticated USING (EXISTS (
    SELECT 1 FROM public.server_members m JOIN public.server_channels c ON c.server_id = m.server_id
    WHERE c.id = channel_id AND c.server_id = campus_server_channel_messages.server_id
      AND public.is_current_campus_user(m.user_id)));
CREATE POLICY "Member sends own channel messages" ON public.campus_server_channel_messages
  FOR INSERT TO authenticated WITH CHECK (
    public.is_current_campus_user(sender_id) AND EXISTS (
      SELECT 1 FROM public.server_members m JOIN public.server_channels c ON c.server_id = m.server_id
      WHERE c.id = channel_id AND c.server_id = campus_server_channel_messages.server_id
        AND public.is_current_campus_user(m.user_id)));
CREATE POLICY "Sender edits channel messages" ON public.campus_server_channel_messages
  FOR UPDATE TO authenticated USING (public.is_current_campus_user(sender_id))
  WITH CHECK (public.is_current_campus_user(sender_id));
CREATE POLICY "Sender deletes channel messages" ON public.campus_server_channel_messages
  FOR DELETE TO authenticated USING (public.is_current_campus_user(sender_id));

DROP POLICY IF EXISTS "Select channel typing" ON public.campus_channel_typing;
DROP POLICY IF EXISTS "Upsert channel typing" ON public.campus_channel_typing;
CREATE POLICY "Member reads channel typing" ON public.campus_channel_typing
  FOR SELECT TO authenticated USING (EXISTS (
    SELECT 1 FROM public.server_members m JOIN public.server_channels c ON c.server_id = m.server_id
    WHERE c.id = channel_id AND c.server_id = campus_channel_typing.server_id
      AND public.is_current_campus_user(m.user_id)));
CREATE POLICY "Member writes own channel typing" ON public.campus_channel_typing
  FOR ALL TO authenticated USING (public.is_current_campus_user(user_id))
  WITH CHECK (public.is_current_campus_user(user_id));

-- Payment status is never written by a mobile client. The payment Edge Function
-- uses service_role only after checking the JWT, ownership and provider response.
DROP POLICY IF EXISTS "Anyone can browse escrow orders" ON public.campus_escrow_orders;
DROP POLICY IF EXISTS "Insert escrow orders" ON public.campus_escrow_orders;
DROP POLICY IF EXISTS "Update escrow orders" ON public.campus_escrow_orders;
CREATE POLICY "Participants read escrow orders" ON public.campus_escrow_orders
  FOR SELECT TO authenticated USING (
    public.is_current_campus_user(buyer_user_id) OR public.is_current_campus_user(seller_user_id));

-- Prevent anonymous / cross-user message, call, and typing access.
DROP POLICY IF EXISTS "Public can view chat messages" ON public.campus_chat_messages;
DROP POLICY IF EXISTS "Users can insert chat messages" ON public.campus_chat_messages;
DROP POLICY IF EXISTS "Users can update chat messages" ON public.campus_chat_messages;
DROP POLICY IF EXISTS "Users can delete chat messages" ON public.campus_chat_messages;
CREATE POLICY "Participants read direct messages" ON public.campus_chat_messages
  FOR SELECT TO authenticated USING (
    public.is_current_campus_user(sender_id) OR EXISTS (
      SELECT 1 FROM public.campusprofile p WHERE p.user_id = auth.uid()
        AND channel_id IN (
          'dm_' || lower(sender_id) || '_' || lower(p.student_id),
          'dm_' || lower(p.student_id) || '_' || lower(sender_id),
          'dm_' || lower(sender_id) || '_' || lower(p.university_email),
          'dm_' || lower(p.university_email) || '_' || lower(sender_id)
        )));
CREATE POLICY "Send own direct messages" ON public.campus_chat_messages
  FOR INSERT TO authenticated WITH CHECK (public.is_current_campus_user(sender_id));
CREATE POLICY "Edit own direct messages" ON public.campus_chat_messages
  FOR UPDATE TO authenticated USING (public.is_current_campus_user(sender_id))
  WITH CHECK (public.is_current_campus_user(sender_id));
CREATE POLICY "Delete own direct messages" ON public.campus_chat_messages
  FOR DELETE TO authenticated USING (public.is_current_campus_user(sender_id));

DROP POLICY IF EXISTS "Select dm typing" ON public.campus_dm_typing;
DROP POLICY IF EXISTS "Upsert dm typing" ON public.campus_dm_typing;
CREATE POLICY "Participants read dm typing" ON public.campus_dm_typing FOR SELECT TO authenticated
  USING (public.is_current_campus_user(user_id) OR EXISTS (
    SELECT 1 FROM public.campusprofile p WHERE p.user_id = auth.uid()
      AND thread_id IN (
        'dm_' || lower(user_id) || '_' || lower(p.student_id),
        'dm_' || lower(p.student_id) || '_' || lower(user_id),
        'dm_' || lower(user_id) || '_' || lower(p.university_email),
        'dm_' || lower(p.university_email) || '_' || lower(user_id))));
CREATE POLICY "Own dm typing" ON public.campus_dm_typing FOR ALL TO authenticated
  USING (public.is_current_campus_user(user_id)) WITH CHECK (public.is_current_campus_user(user_id));

DROP POLICY IF EXISTS "Select call sessions" ON public.campus_call_sessions;
DROP POLICY IF EXISTS "Insert call sessions" ON public.campus_call_sessions;
DROP POLICY IF EXISTS "Update call sessions" ON public.campus_call_sessions;
CREATE POLICY "Call participants read" ON public.campus_call_sessions FOR SELECT TO authenticated
  USING (public.is_current_campus_user(caller_id) OR public.is_current_campus_user(callee_id));
CREATE POLICY "Caller creates call" ON public.campus_call_sessions FOR INSERT TO authenticated
  WITH CHECK (public.is_current_campus_user(caller_id));
CREATE POLICY "Call participants update" ON public.campus_call_sessions FOR UPDATE TO authenticated
  USING (public.is_current_campus_user(caller_id) OR public.is_current_campus_user(callee_id))
  WITH CHECK (public.is_current_campus_user(caller_id) OR public.is_current_campus_user(callee_id));

DROP POLICY IF EXISTS "Select push tokens" ON public.campus_device_push_tokens;
DROP POLICY IF EXISTS "Upsert own push token" ON public.campus_device_push_tokens;
CREATE POLICY "Own push token" ON public.campus_device_push_tokens FOR ALL TO authenticated
  USING (public.is_current_campus_user(user_id)) WITH CHECK (public.is_current_campus_user(user_id));

DROP POLICY IF EXISTS "Select campus notifications" ON public.campus_notifications;
DROP POLICY IF EXISTS "Insert campus notifications" ON public.campus_notifications;
DROP POLICY IF EXISTS "Update campus notifications" ON public.campus_notifications;
CREATE POLICY "Recipient reads notifications" ON public.campus_notifications FOR SELECT TO authenticated
  USING (public.is_current_campus_user(recipient_user_id));
CREATE POLICY "Actor sends notification" ON public.campus_notifications FOR INSERT TO authenticated
  WITH CHECK (public.is_current_campus_user(actor_user_id));
CREATE POLICY "Recipient updates notifications" ON public.campus_notifications FOR UPDATE TO authenticated
  USING (public.is_current_campus_user(recipient_user_id))
  WITH CHECK (public.is_current_campus_user(recipient_user_id));

DROP POLICY IF EXISTS "Users can view their friendships" ON public.campus_friends;
DROP POLICY IF EXISTS "Allow anon and auth manage friendships" ON public.campus_friends;
DROP POLICY IF EXISTS "Authenticated can manage friendships" ON public.campus_friends;
CREATE POLICY "Participants read friendships" ON public.campus_friends FOR SELECT TO authenticated
  USING (public.is_current_campus_user(user_id) OR public.is_current_campus_user(friend_user_id));
CREATE POLICY "Participants create friendships" ON public.campus_friends FOR INSERT TO authenticated
  WITH CHECK (public.is_current_campus_user(user_id) OR public.is_current_campus_user(friend_user_id));
CREATE POLICY "Participants update friendships" ON public.campus_friends FOR UPDATE TO authenticated
  USING (public.is_current_campus_user(user_id) OR public.is_current_campus_user(friend_user_id))
  WITH CHECK (public.is_current_campus_user(user_id) OR public.is_current_campus_user(friend_user_id));
CREATE POLICY "Participants delete friendships" ON public.campus_friends FOR DELETE TO authenticated
  USING (public.is_current_campus_user(user_id) OR public.is_current_campus_user(friend_user_id));

DROP POLICY IF EXISTS "Insert market items requires seller_id" ON public.campus_market_items;
DROP POLICY IF EXISTS "Update market items" ON public.campus_market_items;
DROP POLICY IF EXISTS "Delete market items" ON public.campus_market_items;
DROP POLICY IF EXISTS "Anyone can browse market items" ON public.campus_market_items;
CREATE POLICY "Students browse market items" ON public.campus_market_items FOR SELECT TO authenticated USING (true);
CREATE POLICY "Seller creates listing" ON public.campus_market_items FOR INSERT TO authenticated
  WITH CHECK (public.is_current_campus_user(seller_id));
CREATE POLICY "Seller edits listing" ON public.campus_market_items FOR UPDATE TO authenticated
  USING (public.is_current_campus_user(seller_id)) WITH CHECK (public.is_current_campus_user(seller_id));
CREATE POLICY "Seller deletes listing" ON public.campus_market_items FOR DELETE TO authenticated
  USING (public.is_current_campus_user(seller_id));

-- Row-level security does not redact one column from otherwise visible rows.
REVOKE SELECT ON public.campus_market_items FROM PUBLIC, anon, authenticated;
GRANT SELECT (id, title, price, currency, category, condition, seller_name, seller_rating,
  is_campus_verified_seller, campus_hall_location, image_url, timestamp_ms, description,
  pickup_spot_name, pickup_security_level, pickup_latitude, pickup_longitude, pickup_timing,
  payout_method, requires_qr_release_code, created_at, seller_id) ON public.campus_market_items TO authenticated;
CREATE OR REPLACE FUNCTION public.get_market_item_release_code(p_item_id TEXT, p_caller_id TEXT)
RETURNS TEXT LANGUAGE sql STABLE SECURITY DEFINER SET search_path = '' AS $$
  SELECT qr_release_code FROM public.campus_market_items
  WHERE id = p_item_id AND public.is_current_campus_user(seller_id)
  LIMIT 1;
$$;
REVOKE ALL ON FUNCTION public.get_market_item_release_code(TEXT, TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.get_market_item_release_code(TEXT, TEXT) TO authenticated;
-- QR verification must happen as part of a server-side payment release, never
-- as a public oracle for six-digit codes.
REVOKE ALL ON FUNCTION public.verify_market_release_code(TEXT, TEXT) FROM PUBLIC, anon, authenticated;

DROP POLICY IF EXISTS "Allow public select on micro gigs" ON public.campus_micro_gigs;
DROP POLICY IF EXISTS "Allow public insert on micro gigs" ON public.campus_micro_gigs;
DROP POLICY IF EXISTS "Allow public update on micro gigs" ON public.campus_micro_gigs;
DROP POLICY IF EXISTS "Allow public delete on micro gigs" ON public.campus_micro_gigs;
CREATE POLICY "Students read micro gigs" ON public.campus_micro_gigs FOR SELECT TO authenticated USING (true);
CREATE POLICY "Poster creates micro gig" ON public.campus_micro_gigs FOR INSERT TO authenticated
  WITH CHECK (public.is_current_campus_user(poster_id));
CREATE POLICY "Poster edits micro gig" ON public.campus_micro_gigs FOR UPDATE TO authenticated
  USING (public.is_current_campus_user(poster_id)) WITH CHECK (public.is_current_campus_user(poster_id));
CREATE POLICY "Poster deletes micro gig" ON public.campus_micro_gigs FOR DELETE TO authenticated
  USING (public.is_current_campus_user(poster_id));

DROP POLICY IF EXISTS "Allow public select on live streams" ON public.campus_live_streams;
DROP POLICY IF EXISTS "Allow public insert on live streams" ON public.campus_live_streams;
DROP POLICY IF EXISTS "Allow public update on live streams" ON public.campus_live_streams;
DROP POLICY IF EXISTS "Allow public delete on live streams" ON public.campus_live_streams;
CREATE POLICY "Students read live streams" ON public.campus_live_streams FOR SELECT TO authenticated USING (true);
CREATE POLICY "Host creates stream" ON public.campus_live_streams FOR INSERT TO authenticated
  WITH CHECK (public.is_current_campus_user(broadcaster_id));
CREATE POLICY "Host edits stream" ON public.campus_live_streams FOR UPDATE TO authenticated
  USING (public.is_current_campus_user(broadcaster_id)) WITH CHECK (public.is_current_campus_user(broadcaster_id));
CREATE POLICY "Host deletes stream" ON public.campus_live_streams FOR DELETE TO authenticated
  USING (public.is_current_campus_user(broadcaster_id));

DROP POLICY IF EXISTS "Allow public select on stream comments" ON public.campus_stream_comments;
DROP POLICY IF EXISTS "Allow public insert on stream comments" ON public.campus_stream_comments;
DROP POLICY IF EXISTS "Allow public update on stream comments" ON public.campus_stream_comments;
DROP POLICY IF EXISTS "Allow public delete on stream comments" ON public.campus_stream_comments;
CREATE POLICY "Students read stream comments" ON public.campus_stream_comments FOR SELECT TO authenticated USING (true);
CREATE POLICY "Author creates comment" ON public.campus_stream_comments FOR INSERT TO authenticated
  WITH CHECK (public.is_current_campus_user(sender_id));
CREATE POLICY "Author edits comment" ON public.campus_stream_comments FOR UPDATE TO authenticated
  USING (public.is_current_campus_user(sender_id)) WITH CHECK (public.is_current_campus_user(sender_id));
CREATE POLICY "Author deletes comment" ON public.campus_stream_comments FOR DELETE TO authenticated
  USING (public.is_current_campus_user(sender_id));

-- Events without historical owner ids are readable, but cannot be changed by
-- arbitrary users. New events must supply creator_id after the app is updated.
ALTER TABLE public.campus_events ADD COLUMN IF NOT EXISTS creator_id TEXT;
DROP POLICY IF EXISTS "Allow public select on campus events" ON public.campus_events;
DROP POLICY IF EXISTS "Allow public insert on campus events" ON public.campus_events;
DROP POLICY IF EXISTS "Allow public update on campus events" ON public.campus_events;
DROP POLICY IF EXISTS "Allow public delete on campus events" ON public.campus_events;
CREATE POLICY "Students read events" ON public.campus_events FOR SELECT TO authenticated USING (true);
CREATE POLICY "Creator creates event" ON public.campus_events FOR INSERT TO authenticated
  WITH CHECK (public.is_current_campus_user(creator_id));
CREATE POLICY "Creator edits event" ON public.campus_events FOR UPDATE TO authenticated
  USING (public.is_current_campus_user(creator_id)) WITH CHECK (public.is_current_campus_user(creator_id));
CREATE POLICY "Creator deletes event" ON public.campus_events FOR DELETE TO authenticated
  USING (public.is_current_campus_user(creator_id));

DROP POLICY IF EXISTS "Allow public select on flash meets" ON public.campus_flash_meets;
DROP POLICY IF EXISTS "Allow public insert on flash meets" ON public.campus_flash_meets;
DROP POLICY IF EXISTS "Allow public update on flash meets" ON public.campus_flash_meets;
DROP POLICY IF EXISTS "Allow public delete on flash meets" ON public.campus_flash_meets;
CREATE POLICY "Students read meets" ON public.campus_flash_meets FOR SELECT TO authenticated USING (true);
CREATE POLICY "Creator creates meet" ON public.campus_flash_meets FOR INSERT TO authenticated
  WITH CHECK (public.is_current_campus_user(creator_id));
CREATE POLICY "Creator edits meet" ON public.campus_flash_meets FOR UPDATE TO authenticated
  USING (public.is_current_campus_user(creator_id)) WITH CHECK (public.is_current_campus_user(creator_id));
CREATE POLICY "Creator deletes meet" ON public.campus_flash_meets FOR DELETE TO authenticated
  USING (public.is_current_campus_user(creator_id));

-- All server hierarchy writes must be by an owner. Only self-joins are public.
DROP POLICY IF EXISTS "Public can insert servers" ON public.servers;
DROP POLICY IF EXISTS "Public can update servers" ON public.servers;
DROP POLICY IF EXISTS "Public can delete servers" ON public.servers;
CREATE POLICY "Owner creates server" ON public.servers FOR INSERT TO authenticated
  WITH CHECK (public.is_current_campus_user(owner_id));
CREATE POLICY "Owner edits server" ON public.servers FOR UPDATE TO authenticated
  USING (public.is_current_campus_user(owner_id)) WITH CHECK (public.is_current_campus_user(owner_id));
CREATE POLICY "Owner deletes server" ON public.servers FOR DELETE TO authenticated
  USING (public.is_current_campus_user(owner_id));
DROP POLICY IF EXISTS "Public can insert server members" ON public.server_members;
DROP POLICY IF EXISTS "Public can update server members" ON public.server_members;
DROP POLICY IF EXISTS "Public can delete server members" ON public.server_members;
CREATE POLICY "Join server as member" ON public.server_members FOR INSERT TO authenticated
  WITH CHECK (public.is_current_campus_user(user_id) AND role = 'MEMBER');
CREATE POLICY "Edit self or owned server member" ON public.server_members FOR UPDATE TO authenticated
  USING (public.is_current_campus_user(user_id) OR EXISTS (
    SELECT 1 FROM public.servers s WHERE s.id = server_id AND public.is_current_campus_user(s.owner_id)))
  WITH CHECK (public.is_current_campus_user(user_id) OR EXISTS (
    SELECT 1 FROM public.servers s WHERE s.id = server_id AND public.is_current_campus_user(s.owner_id)));
CREATE POLICY "Leave server or remove owned member" ON public.server_members FOR DELETE TO authenticated
  USING (public.is_current_campus_user(user_id) OR EXISTS (
    SELECT 1 FROM public.servers s WHERE s.id = server_id AND public.is_current_campus_user(s.owner_id)));
DROP POLICY IF EXISTS "Public can insert server channels" ON public.server_channels;
DROP POLICY IF EXISTS "Public can update server channels" ON public.server_channels;
DROP POLICY IF EXISTS "Public can delete server channels" ON public.server_channels;
CREATE POLICY "Owner creates channel" ON public.server_channels FOR INSERT TO authenticated
  WITH CHECK (EXISTS (SELECT 1 FROM public.servers s WHERE s.id = server_id AND public.is_current_campus_user(s.owner_id)));
CREATE POLICY "Owner edits channel" ON public.server_channels FOR UPDATE TO authenticated
  USING (EXISTS (SELECT 1 FROM public.servers s WHERE s.id = server_id AND public.is_current_campus_user(s.owner_id)))
  WITH CHECK (EXISTS (SELECT 1 FROM public.servers s WHERE s.id = server_id AND public.is_current_campus_user(s.owner_id)));
CREATE POLICY "Owner deletes channel" ON public.server_channels FOR DELETE TO authenticated
  USING (EXISTS (SELECT 1 FROM public.servers s WHERE s.id = server_id AND public.is_current_campus_user(s.owner_id)));
DROP POLICY IF EXISTS "Public can insert server rules" ON public.server_rules;
DROP POLICY IF EXISTS "Public can update server rules" ON public.server_rules;
DROP POLICY IF EXISTS "Public can delete server rules" ON public.server_rules;
CREATE POLICY "Owner creates rule" ON public.server_rules FOR INSERT TO authenticated
  WITH CHECK (EXISTS (SELECT 1 FROM public.servers s WHERE s.id = server_id AND public.is_current_campus_user(s.owner_id)));
CREATE POLICY "Owner edits rule" ON public.server_rules FOR UPDATE TO authenticated
  USING (EXISTS (SELECT 1 FROM public.servers s WHERE s.id = server_id AND public.is_current_campus_user(s.owner_id)))
  WITH CHECK (EXISTS (SELECT 1 FROM public.servers s WHERE s.id = server_id AND public.is_current_campus_user(s.owner_id)));
CREATE POLICY "Owner deletes rule" ON public.server_rules FOR DELETE TO authenticated
  USING (EXISTS (SELECT 1 FROM public.servers s WHERE s.id = server_id AND public.is_current_campus_user(s.owner_id)));
DROP POLICY IF EXISTS "Public can insert server invites" ON public.server_invites;
DROP POLICY IF EXISTS "Public can update server invites" ON public.server_invites;
CREATE POLICY "Member creates invite" ON public.server_invites FOR INSERT TO authenticated
  WITH CHECK (public.is_current_campus_user(inviter_id) AND EXISTS (
    SELECT 1 FROM public.server_members m WHERE m.server_id = server_invites.server_id AND public.is_current_campus_user(m.user_id)));
CREATE POLICY "Inviter edits invite" ON public.server_invites FOR UPDATE TO authenticated
  USING (public.is_current_campus_user(inviter_id)) WITH CHECK (public.is_current_campus_user(inviter_id));

-- Remove unscoped, role-wide policies inherited from the original schema.
DROP POLICY IF EXISTS "Authenticated can manage academic channels" ON public.campus_academic_channels;
DROP POLICY IF EXISTS "Authenticated can upsert streamer profiles" ON public.campus_streamer_profiles;
DROP POLICY IF EXISTS "Authenticated can update streamer profiles" ON public.campus_streamer_profiles;
CREATE POLICY "Own streamer profile insert" ON public.campus_streamer_profiles FOR INSERT TO authenticated
  WITH CHECK (public.is_current_campus_user(student_id));
CREATE POLICY "Own streamer profile update" ON public.campus_streamer_profiles FOR UPDATE TO authenticated
  USING (public.is_current_campus_user(student_id)) WITH CHECK (public.is_current_campus_user(student_id));

-- A bounded, row-locked QR challenge for payment release. Only the order buyer
-- may attempt verification; the answer never reveals the seller's code.
ALTER TABLE public.campus_escrow_orders ADD COLUMN IF NOT EXISTS release_attempts INTEGER NOT NULL DEFAULT 0;
CREATE OR REPLACE FUNCTION public.verify_escrow_release_code(p_order_id TEXT, p_scanned_code TEXT)
RETURNS BOOLEAN LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE
  v_order public.campus_escrow_orders%ROWTYPE;
  v_code TEXT;
BEGIN
  SELECT * INTO v_order FROM public.campus_escrow_orders WHERE id = p_order_id FOR UPDATE;
  IF NOT FOUND OR NOT public.is_current_campus_user(v_order.buyer_user_id) OR
     v_order.status <> 'FUNDS_HELD' OR v_order.release_attempts >= 5 THEN
    RETURN FALSE;
  END IF;
  UPDATE public.campus_escrow_orders SET release_attempts = release_attempts + 1 WHERE id = p_order_id;
  SELECT upper(trim(qr_release_code)) INTO v_code FROM public.campus_market_items
    WHERE id = v_order.market_item_id;
  RETURN length(coalesce(v_code, '')) > 0 AND
    v_code = upper(trim(coalesce(p_scanned_code, '')));
END;
$$;
REVOKE ALL ON FUNCTION public.verify_escrow_release_code(TEXT, TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.verify_escrow_release_code(TEXT, TEXT) TO authenticated;

-- Self-service membership changes must never grant OWNER/ADMIN by changing a role.
CREATE OR REPLACE FUNCTION public.protect_server_member_role()
RETURNS TRIGGER LANGUAGE plpgsql SET search_path = '' AS $$
BEGIN
  IF auth.uid() IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM public.servers s WHERE s.id = NEW.server_id
      AND public.is_current_campus_user(s.owner_id)
  ) AND (NEW.role IS DISTINCT FROM OLD.role OR NEW.server_id IS DISTINCT FROM OLD.server_id OR
         NEW.user_id IS DISTINCT FROM OLD.user_id) THEN
    RAISE EXCEPTION 'Only the server owner can change member roles' USING ERRCODE = '42501';
  END IF;
  RETURN NEW;
END;
$$;
DROP TRIGGER IF EXISTS protect_server_member_role ON public.server_members;
CREATE TRIGGER protect_server_member_role BEFORE UPDATE ON public.server_members
  FOR EACH ROW EXECUTE FUNCTION public.protect_server_member_role();
