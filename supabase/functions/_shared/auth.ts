export const jsonHeaders = { "Content-Type": "application/json" };

/** Verify the user JWT with Supabase Auth; anon/service keys are not user identities. */
export async function requireUser(req: Request): Promise<{ id: string; email: string }> {
  const url = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const token = req.headers.get("authorization")?.match(/^Bearer (.+)$/i)?.[1];
  if (!url || !anonKey || !token || token === anonKey || token === Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")) {
    throw new Error("UNAUTHORIZED");
  }
  const response = await fetch(`${url}/auth/v1/user`, {
    headers: { apikey: anonKey, Authorization: `Bearer ${token}` },
  });
  if (!response.ok) throw new Error("UNAUTHORIZED");
  const user = await response.json();
  if (!user?.id || !user?.email) throw new Error("UNAUTHORIZED");
  return { id: user.id, email: user.email };
}
