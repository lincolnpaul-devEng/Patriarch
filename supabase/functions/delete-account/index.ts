// Supabase Edge Function: delete-account
// Permanently deletes a user's account, all owned data, and Cloudinary assets.
// Requires a valid Supabase user JWT. Runs as service_role for deletion.
import { requireUser, jsonHeaders } from "../_shared/auth.ts";

const supabaseUrl = Deno.env.get("SUPABASE_URL");
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
const cloudName = Deno.env.get("CLOUDINARY_CLOUD_NAME");
const cloudApiKey = Deno.env.get("CLOUDINARY_API_KEY");
const cloudSecret = Deno.env.get("CLOUDINARY_API_SECRET");

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response(null, { status: 405 });

  try {
    const { id: userId, email } = await requireUser(req);
    if (!supabaseUrl || !serviceKey) throw new Error("CONFIGURATION");

    const adminHeaders = {
      apikey: serviceKey,
      Authorization: `Bearer ${serviceKey}`,
      "Content-Type": "application/json",
    };

    // 1. Delete all user-owned application data across tables.
    //    Order matters: child rows first, then parents.
    const tables = [
      "campus_device_push_tokens",
      "campus_dm_typing",
      "campus_channel_typing",
      "campus_stream_comments",
      "campus_notifications",
      "campus_friends",
      "campus_escrow_orders",
      "campus_server_channel_messages",
      "campus_chat_messages",
      "campus_server_members",
      "campus_per_server_profiles",
      "campus_server_channels",
      "campus_server_rules",
      "campus_server_invites",
      "campus_servers",
      "campus_market_items",
      "campus_micro_gigs",
      "campus_live_streams",
      "campus_flash_meets",
      "campus_events",
      "campus_call_sessions",
      "campus_email_verifications",
      "campus_saved_listings",
      "campus_rendezvous_points",
      "campusprofile",
    ];

    for (const table of tables) {
      await fetch(
        `${supabaseUrl}/rest/v1/${table}?${
          table === "campusprofile" ? "user_id" : "user_id"
        }=eq.${encodeURIComponent(userId)}`,
        {
          method: "DELETE",
          headers: adminHeaders,
        }
      );
    }

    // 2. Delete Cloudinary assets referenced in the profile and user-owned items.
    //    Collect public_ids from avatar_url and image_url fields before deletion.
    const profileRes = await fetch(
      `${supabaseUrl}/rest/v1/campusprofile?user_id=eq.${encodeURIComponent(userId)}&select=avatar_url`,
      { headers: adminHeaders }
    );
    const publicIds: string[] = [];
    if (profileRes.ok) {
      const [profile] = await profileRes.json();
      const avatar = profile?.avatar_url;
      if (avatar && typeof avatar === "string") {
        const id = cloudinaryPublicId(avatar);
        if (id) publicIds.push(id);
      }
    }
    // Also scan marketplace/live stream image urls the user owned.
    for (const table of ["campus_market_items", "campus_live_streams"]) {
      const itemRes = await fetch(
        `${supabaseUrl}/rest/v1/${table}?seller_id=eq.${encodeURIComponent(userId)}&select=image_url`,
        { headers: adminHeaders }
      );
      if (itemRes.ok) {
        const rows = await itemRes.json();
        for (const row of rows) {
          const url = row?.image_url;
          if (url && typeof url === "string") {
            const id = cloudinaryPublicId(url);
            if (id) publicIds.push(id);
          }
        }
      }
    }

    if (cloudName && cloudApiKey && cloudSecret && publicIds.length > 0) {
      // Cloudinary delete by tag is not reliable per-public-id without upload preset,
      // so delete each public id individually via the Admin API (basic auth).
      const auth = btoa(`${cloudApiKey}:${cloudSecret}`);
      for (const publicId of publicIds) {
        await fetch(
          `https://api.cloudinary.com/v1_1/${cloudName}/image/destroy`,
          {
            method: "POST",
            headers: {
              Authorization: `Basic ${auth}`,
              "Content-Type": "application/json",
            },
            body: JSON.stringify({ public_id: publicId, type: "upload" }),
          }
        );
      }
    }

    // 3. Delete the Supabase Auth account (service_role only).
    await fetch(`${supabaseUrl}/auth/v1/admin/users/${encodeURIComponent(userId)}`, {
      method: "DELETE",
      headers: adminHeaders,
    });

    return new Response(
      JSON.stringify({ status: "deleted", userId }),
      { headers: jsonHeaders }
    );
  } catch (error) {
    const status =
      error instanceof Error && error.message === "UNAUTHORIZED" ? 401 : 500;
    return new Response(
      JSON.stringify({ error: status === 401 ? "Sign in required" : "Deletion failed" }),
      { status, headers: jsonHeaders }
    );
  }
});

/** Extract Cloudinary public_id from a secure_url like .../upload/v123/folder/public_id.ext */
function cloudinaryPublicId(url: string): string | null {
  try {
    const match = url.match(/\/upload\/(?:v\d+\/)?(.+?)(?:\.\w+)?$/);
    return match ? match[1] : null;
  } catch {
    return null;
  }
}
