import { requireUser, jsonHeaders } from "../_shared/auth.ts";

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response(null, { status: 405 });
  try {
    await requireUser(req);
    const cloudName = Deno.env.get("CLOUDINARY_CLOUD_NAME");
    const apiKey = Deno.env.get("CLOUDINARY_API_KEY");
    const secret = Deno.env.get("CLOUDINARY_API_SECRET");
    if (!cloudName || !apiKey || !secret) throw new Error("CONFIGURATION");
    const timestamp = String(Math.floor(Date.now() / 1000));
    const folder = "campus";
    // Cloudinary signs lexicographically sorted params (excluding file/api_key).
    const input = new TextEncoder().encode(`folder=${folder}&timestamp=${timestamp}${secret}`);
    const hash = await crypto.subtle.digest("SHA-1", input);
    const signature = Array.from(new Uint8Array(hash), (byte) => byte.toString(16).padStart(2, "0")).join("");
    return new Response(JSON.stringify({ cloudName, apiKey, folder, timestamp, signature }), { headers: jsonHeaders });
  } catch (error) {
    const status = error instanceof Error && error.message === "UNAUTHORIZED" ? 401 : 503;
    return new Response(JSON.stringify({ error: status === 401 ? "Sign in required" : "Upload unavailable" }),
      { status, headers: jsonHeaders });
  }
});
