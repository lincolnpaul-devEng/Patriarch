import { requireUser, jsonHeaders } from "../_shared/auth.ts";

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response(null, { status: 405 });
  try {
    await requireUser(req);
    const apiKey = Deno.env.get("OPENROUTER_API_KEY");
    if (!apiKey) throw new Error("CONFIGURATION");
    const payload = await req.json();
    // Never proxy arbitrary models, roles, token counts, or endpoints to a paid API.
    if (payload.model !== "google/gemini-2.0-flash-exp:free" ||
        !Array.isArray(payload.messages) || payload.messages.length !== 2 ||
        payload.messages[0]?.role !== "system" || payload.messages[1]?.role !== "user" ||
        typeof payload.messages[1]?.content !== "string" || payload.messages[1].content.length > 1000) {
      return new Response(JSON.stringify({ error: "Invalid AI request" }), { status: 400, headers: jsonHeaders });
    }
    const response = await fetch("https://openrouter.ai/api/v1/chat/completions", {
      method: "POST",
      headers: { Authorization: `Bearer ${apiKey}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        model: "google/gemini-2.0-flash-exp:free",
        messages: payload.messages,
        max_tokens: Math.min(Math.max(Number(payload.max_tokens) || 150, 1), 220),
        temperature: 0.3,
      }),
    });
    if (!response.ok) throw new Error("PROVIDER_ERROR");
    return new Response(await response.text(), { headers: jsonHeaders });
  } catch (error) {
    const status = error instanceof Error && error.message === "UNAUTHORIZED" ? 401 : 503;
    return new Response(JSON.stringify({ error: status === 401 ? "Sign in required" : "AI unavailable" }),
      { status, headers: jsonHeaders });
  }
});
