// Supabase Edge Function: stream-token
// Generates standard HS256 signed JWT tokens accepted by Stream Chat, Video, and Audio SDKs.
// Uses native Web Crypto HMAC-SHA256 (zero external npm dependency issues in Deno).

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

// Base64URL encode utility
function base64UrlEncode(data: Uint8Array | string): string {
  const binary = typeof data === "string" ? new TextEncoder().encode(data) : data;
  let str = "";
  for (let i = 0; i < binary.length; i++) {
    str += String.fromCharCode(binary[i]);
  }
  return btoa(str).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

// Generate HS256 JWT using Web Crypto API
async function createStreamJwt(userId: string, secretKey: string, validitySeconds = 86400): Promise<string> {
  const header = {
    alg: "HS256",
    typ: "JWT",
  };

  const now = Math.floor(Date.now() / 1000);
  const payload = {
    user_id: userId,
    iat: now,
    exp: now + validitySeconds,
  };

  const encodedHeader = base64UrlEncode(JSON.stringify(header));
  const encodedPayload = base64UrlEncode(JSON.stringify(payload));
  const signingInput = `${encodedHeader}.${encodedPayload}`;

  const keyData = new TextEncoder().encode(secretKey);
  const cryptoKey = await crypto.subtle.importKey(
    "raw",
    keyData,
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  );

  const signature = await crypto.subtle.sign(
    "HMAC",
    cryptoKey,
    new TextEncoder().encode(signingInput)
  );

  const encodedSignature = base64UrlEncode(new Uint8Array(signature));
  return `${signingInput}.${encodedSignature}`;
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const apiKey = Deno.env.get("STREAM_API_KEY");
    const secretKey = Deno.env.get("STREAM_SECRET_KEY");

    if (!apiKey || !secretKey) {
      return new Response(
        JSON.stringify({ error: "Missing STREAM_API_KEY or STREAM_SECRET_KEY in server secrets" }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
    const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    const jwt = req.headers.get("Authorization")?.match(/^Bearer (.+)$/i)?.[1];
    if (!supabaseUrl || !anonKey || !serviceKey || !jwt || jwt === anonKey) {
      return new Response(JSON.stringify({ error: "Authentication required" }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } });
    }
    const authResponse = await fetch(`${supabaseUrl}/auth/v1/user`, {
      headers: { apikey: anonKey, Authorization: `Bearer ${jwt}` },
    });
    if (!authResponse.ok) {
      return new Response(JSON.stringify({ error: "Invalid session" }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } });
    }
    const authenticatedUser = await authResponse.json();
    const { userId } = await req.json();

    if (!userId || typeof userId !== "string") {
      return new Response(
        JSON.stringify({ error: "userId is required" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Stream ids are the app's stable campusprofile student_id (or email before sync).
    // Never trust a requested id, even with a valid JWT for another account.
    const profileResponse = await fetch(
      `${supabaseUrl}/rest/v1/campusprofile?user_id=eq.${encodeURIComponent(authenticatedUser.id)}&select=student_id,university_email&limit=1`,
      { headers: { apikey: serviceKey, Authorization: `Bearer ${serviceKey}` } },
    );
    if (!profileResponse.ok) throw new Error("Profile lookup failed");
    const [profile] = await profileResponse.json();
    const allowedIds = [authenticatedUser.id, profile?.student_id, profile?.university_email]
      .filter((id): id is string => typeof id === "string" && id.length > 0);
    if (!allowedIds.some((id) => id.toLowerCase() === userId.toLowerCase())) {
      return new Response(JSON.stringify({ error: "User id does not belong to this session" }),
        { status: 403, headers: { ...corsHeaders, "Content-Type": "application/json" } });
    }

    // Generate compliant Stream User Token signed with secretKey
    const token = await createStreamJwt(userId, secretKey);

    return new Response(
      JSON.stringify({
        token,
        userId,
        apiKey,
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (err: unknown) {
    return new Response(
      JSON.stringify({ error: "Unable to create Stream token" }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
