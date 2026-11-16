// Supabase Edge Function: delete-account
// Permanently deletes a user's account, all owned data via delete_own_account_data() RPC,
// and Cloudinary assets. Requires a valid Supabase user JWT. Runs as service_role for deletion.
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

    // 1. Collect Cloudinary asset public IDs BEFORE data deletion
    const publicIds: string[] = [];

    // Avatar from campusprofile
    const profileRes = await fetch(
      `${supabaseUrl}/rest/v1/campusprofile?user_id=eq.${encodeURIComponent(userId)}&select=avatar_url`,
      { headers: adminHeaders }
    );
    if (profileRes.ok) {
      const rows = await profileRes.json();
      const avatar = rows?.[0]?.avatar_url;
      if (avatar && typeof avatar === "string") {
        const pid = cloudinaryPublicId(avatar);
        if (pid) publicIds.push(pid);
      }
    }

    // Item and stream images
    for (const table of ["campus_market_items", "campus_live_streams"]) {
      const col = table === "campus_market_items" ? "seller_id" : "broadcaster_id";
      const imageCol = table === "campus_market_items" ? "image_url" : "backdrop_image_url";
      const itemRes = await fetch(
        `${supabaseUrl}/rest/v1/${table}?${col}=eq.${encodeURIComponent(userId)}&select=${imageCol}`,
        { headers: adminHeaders }
      );
      if (itemRes.ok) {
        const rows = await itemRes.json();
        for (const row of rows) {
          const url = row?.[imageCol];
          if (url && typeof url === "string") {
            const pid = cloudinaryPublicId(url);
            if (pid) publicIds.push(pid);
          }
        }
      }
    }

    // 2. Atomically delete all user-owned database records via RPC
    const rpcRes = await fetch(`${supabaseUrl}/rest/v1/rpc/delete_own_account_data`, {
      method: "POST",
      headers: adminHeaders,
      body: JSON.stringify({ target_user_id: userId }),
    });

    if (!rpcRes.ok) {
      const errText = await rpcRes.text();
      console.error(`delete_own_account_data RPC failed: ${errText}`);
      throw new Error(`RPC_FAILED: ${errText}`);
    }

    // 3. Delete Cloudinary assets if credentials are present
    if (cloudName && cloudApiKey && cloudSecret && publicIds.length > 0) {
      const auth = btoa(`${cloudApiKey}:${cloudSecret}`);
      for (const publicId of publicIds) {
        try {
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
        } catch (cErr) {
          console.warn(`Cloudinary deletion warning for ${publicId}:`, cErr);
        }
      }
    }

    // 4. Delete the Supabase Auth user (service_role only)
    const authDelRes = await fetch(`${supabaseUrl}/auth/v1/admin/users/${encodeURIComponent(userId)}`, {
      method: "DELETE",
      headers: adminHeaders,
    });

    if (!authDelRes.ok) {
      const authErr = await authDelRes.text();
      console.error(`Admin delete user failed: ${authErr}`);
    }

    return new Response(
      JSON.stringify({ status: "deleted", userId }),
      { headers: jsonHeaders }
    );
  } catch (error) {
    const isUnauthorized = error instanceof Error && error.message === "UNAUTHORIZED";
    const status = isUnauthorized ? 401 : 500;
    const msg = isUnauthorized ? "Sign in required" : "Deletion failed";
    return new Response(
      JSON.stringify({ error: msg, details: error instanceof Error ? error.message : String(error) }),
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
