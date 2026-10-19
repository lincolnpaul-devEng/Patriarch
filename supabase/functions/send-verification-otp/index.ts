// Supabase Edge Function: send-verification-otp
// Communicates with Resend API using RESEND_API_KEY, RESEND_FROM_EMAIL, and RESEND_FROM_NAME
// to deliver 6-digit OTP email verification codes for Patriarch student authentication.

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

interface SendOtpRequest {
  action?: "send" | "verify" | "resend";
  email?: string;
  code?: string;
  fullName?: string;
  // Supabase Auth Hook format support
  user?: {
    email: string;
    user_metadata?: { full_name?: string };
  };
  email_data?: {
    token: string;
    token_hash?: string;
    redirect_to?: string;
    email_action_type?: string;
  };
}

function generateOtp(): string {
  // 6-digit cryptographically secure numeric OTP
  const array = new Uint32Array(1);
  crypto.getRandomValues(array);
  const code = (array[0] % 900000) + 100000;
  return code.toString();
}

function createEmailHtml(otp: string, recipientName?: string): string {
  const greeting = recipientName ? `Hi ${recipientName},` : "Hello Student,";
  return `
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Your Patriarch Verification Code</title>
</head>
<body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #F8FAFC; color: #0F172A;">
  <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%" style="background-color: #F8FAFC; padding: 40px 16px;">
    <tr>
      <td align="center">
        <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width: 540px; background-color: #FFFFFF; border-radius: 20px; border: 1px solid #E2E8F0; box-shadow: 0 4px 12px rgba(15, 23, 42, 0.05); overflow: hidden;">
          
          <!-- Header Banner -->
          <tr>
            <td style="background-color: #0A1F36; padding: 36px 32px; text-align: center;">
              <h1 style="margin: 0; font-size: 26px; font-weight: 800; color: #FFFFFF; letter-spacing: -0.5px;">Patriarch</h1>
              <p style="margin: 6px 0 0; font-size: 13px; color: #94A3B8; font-weight: 500;">Campus Network &amp; SafeTrade Treasury</p>
            </td>
          </tr>

          <!-- Main Content -->
          <tr>
            <td style="padding: 36px 32px;">
              <p style="margin: 0 0 16px; font-size: 16px; font-weight: 600; color: #0F172A;">${greeting}</p>
              <p style="margin: 0 0 24px; font-size: 14.5px; line-height: 22px; color: #475569;">
                Enter the 6-digit verification code below on your device to confirm your campus email and activate your student account.
              </p>

              <!-- OTP Code Display Card -->
              <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%" style="margin: 24px 0;">
                <tr>
                  <td align="center" style="background-color: #EFF6FF; border: 1.5px dashed #3B82F6; border-radius: 14px; padding: 22px;">
                    <div style="font-size: 36px; font-weight: 800; letter-spacing: 10px; color: #007AFF; font-family: monospace;">
                      ${otp}
                    </div>
                    <div style="margin-top: 8px; font-size: 12px; font-weight: 600; color: #64748B;">
                      EXPIRES IN 15 MINUTES
                    </div>
                  </td>
                </tr>
              </table>

              <p style="margin: 24px 0 0; font-size: 13px; line-height: 20px; color: #64748B;">
                If you did not request this code, you can safely ignore this email. Someone may have entered your university email address by mistake.
              </p>
            </td>
          </tr>

          <!-- Footer -->
          <tr>
            <td style="background-color: #F8FAFC; border-top: 1px solid #E2E8F0; padding: 20px 32px; text-align: center;">
              <p style="margin: 0; font-size: 12px; color: #94A3B8;">
                &copy; ${new Date().getFullYear()} Patriarch Campus Network. All rights reserved.
              </p>
            </td>
          </tr>

        </table>
      </td>
    </tr>
  </table>
</body>
</html>
  `.trim();
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const resendApiKey = Deno.env.get("RESEND_API_KEY");
    const resendFromEmail = Deno.env.get("RESEND_FROM_EMAIL") || "onboarding@resend.dev";
    const resendFromName = Deno.env.get("RESEND_FROM_NAME") || "Patriarch";
    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || Deno.env.get("SUPABASE_ANON_KEY");

    if (!resendApiKey) {
      return new Response(
        JSON.stringify({ error: "Missing RESEND_API_KEY in server secrets" }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const body: SendOtpRequest = await req.json().catch(() => ({}));

    // Detect if invoked by Supabase Auth hook or direct app invocation
    const email = body.email || body.user?.email;
    const action = body.action || (body.email_data ? "send" : "send");
    const code = body.code;
    const fullName = body.fullName || body.user?.user_metadata?.full_name;

    if (!email || typeof email !== "string" || !email.includes("@")) {
      return new Response(
        JSON.stringify({ error: "A valid email address is required" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const cleanEmail = email.trim().toLowerCase();

    // ---------------------------------------------------------------------------
    // Action: Verify OTP Code
    // ---------------------------------------------------------------------------
    if (action === "verify") {
      if (!code || typeof code !== "string" || code.trim().length !== 6) {
        return new Response(
          JSON.stringify({ error: "A 6-digit numeric verification code is required" }),
          { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      const cleanCode = code.trim();

      // Query database for valid unverified OTP
      if (supabaseUrl && serviceRoleKey) {
        const queryUrl = `${supabaseUrl}/rest/v1/campus_email_verifications?email=eq.${encodeURIComponent(cleanEmail)}&otp_code=eq.${cleanCode}&verified=eq.false&order=created_at.desc&limit=1`;
        const checkRes = await fetch(queryUrl, {
          headers: {
            "apikey": serviceRoleKey,
            "Authorization": `Bearer ${serviceRoleKey}`,
            "Accept": "application/json",
          },
        });

        if (checkRes.ok) {
          const records = await checkRes.json();
          if (records && records.length > 0) {
            const record = records[0];
            const expiresAt = new Date(record.expires_at).getTime();
            if (Date.now() > expiresAt) {
              return new Response(
                JSON.stringify({ success: false, error: "Verification code has expired. Please request a new one." }),
                { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
              );
            }

            // Mark as verified
            await fetch(`${supabaseUrl}/rest/v1/campus_email_verifications?id=eq.${record.id}`, {
              method: "PATCH",
              headers: {
                "apikey": serviceRoleKey,
                "Authorization": `Bearer ${serviceRoleKey}`,
                "Content-Type": "application/json",
              },
              body: JSON.stringify({ verified: true }),
            });

            // Update campusprofile is_verified_student = true
            await fetch(`${supabaseUrl}/rest/v1/campusprofile?university_email=eq.${encodeURIComponent(cleanEmail)}`, {
              method: "PATCH",
              headers: {
                "apikey": serviceRoleKey,
                "Authorization": `Bearer ${serviceRoleKey}`,
                "Content-Type": "application/json",
              },
              body: JSON.stringify({ is_verified_student: true }),
            }).catch((err) => console.error("Failed to update campusprofile verification status:", err));

            return new Response(
              JSON.stringify({
                success: true,
                verified: true,
                email: cleanEmail,
                message: "Email successfully verified",
              }),
              { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
            );
          }
        }
      }

      return new Response(
        JSON.stringify({ success: false, error: "Invalid verification code. Please check your email and try again." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // ---------------------------------------------------------------------------
    // Action: Send / Resend OTP Code
    // ---------------------------------------------------------------------------
    // Use token from Supabase Auth hook if present, else generate fresh 6-digit OTP
    const otp = body.email_data?.token || generateOtp();

    // 1. Store OTP record in campus_email_verifications table
    if (supabaseUrl && serviceRoleKey) {
      const expiresAt = new Date(Date.now() + 15 * 60 * 1000).toISOString(); // 15 mins expiry
      await fetch(`${supabaseUrl}/rest/v1/campus_email_verifications`, {
        method: "POST",
        headers: {
          "apikey": serviceRoleKey,
          "Authorization": `Bearer ${serviceRoleKey}`,
          "Content-Type": "application/json",
          "Prefer": "return=minimal",
        },
        body: JSON.stringify({
          email: cleanEmail,
          otp_code: otp,
          expires_at: expiresAt,
          verified: false,
        }),
      }).catch((err) => console.error("Database OTP store error:", err));
    }

    // 2. Dispatch email via Resend API
    const resendPayload = {
      from: `${resendFromName} <${resendFromEmail}>`,
      to: [cleanEmail],
      subject: `${otp} is your Patriarch verification code`,
      html: createEmailHtml(otp, fullName),
    };

    const resendResponse = await fetch("https://api.resend.com/emails", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${resendApiKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(resendPayload),
    });

    const resendResult = await resendResponse.json();

    if (!resendResponse.ok) {
      console.error("Resend API error:", resendResult);
      return new Response(
        JSON.stringify({
          error: resendResult.message || "Failed to send verification email via Resend",
          details: resendResult,
        }),
        { status: resendResponse.status, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    return new Response(
      JSON.stringify({
        success: true,
        email: cleanEmail,
        message: `6-digit verification code sent to ${cleanEmail}`,
        id: resendResult.id,
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : "Internal server error";
    return new Response(
      JSON.stringify({ error: message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
