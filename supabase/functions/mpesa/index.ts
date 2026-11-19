// Supabase Edge Function: mpesa
// Campus SafeTrade escrow over M-Pesa Daraja (sandbox-first, production = credential swap).
//
//   POST /functions/v1/mpesa
//     { action: "initiate", itemId, amount, buyerUserId, buyerPhone, orderNumber,
//       rendezvousSpot, buyerNote, paymentMethodId }           -> { order }
//     { action: "release",  itemId, scannedCode }              -> { order }
//     { action: "refund",   itemId, reason? }                  -> { order }
//   POST /functions/v1/mpesa/callback                            (Daraja result webhook)
//
// Order lifecycle (strings match the Android EscrowOrderStatus model):
//   PENDING_PAYMENT -> FUNDS_HELD (callback ok)     item -> RESERVED
//                   -> FAILED      (callback fail) item stays ACTIVE
//   FUNDS_HELD      -> RELEASED_TO_SELLER (release) item -> SOLD
//   FUNDS_HELD      -> REFUNDED_TO_BUYER  (refund) item -> ACTIVE
//
// Secrets: MPESA_CONSUMER_KEY, MPESA_CONSUMER_SECRET, MPESA_PASSKEY,
//          MPESA_SHORTCODE, MPESA_ENVIRONMENT ("sandbox" | "production").
// With MPESA_ENVIRONMENT=sandbox no real B2C payout occurs; state transitions
// still run so the full SafeTrade flow is exercisable end-to-end.

import { requireUser } from "../_shared/auth.ts";

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const consumerKey = Deno.env.get("MPESA_CONSUMER_KEY") ?? "";
const consumerSecret = Deno.env.get("MPESA_CONSUMER_SECRET") ?? "";
const passkey = Deno.env.get("MPESA_PASSKEY") ?? "";
const shortcode = Deno.env.get("MPESA_SHORTCODE") ?? "174379";
const environment = (Deno.env.get("MPESA_ENVIRONMENT") ?? "sandbox").toLowerCase();
const darajaBase = environment === "production"
  ? "https://api.safaricom.co.ke"
  : "https://sandbox.safaricom.co.ke";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

function fail(code: string, message: string, status: number): Response {
  return json({ error: code, message }, status);
}

const adminHeaders = {
  apikey: serviceKey,
  Authorization: `Bearer ${serviceKey}`,
  "Content-Type": "application/json",
};

let cachedToken: { token: string; expiresAt: number } | null = null;

async function darajaToken(): Promise<string> {
  if (cachedToken && Date.now() < cachedToken.expiresAt) return cachedToken.token;
  if (!consumerKey || !consumerSecret) throw new Error("MPESA_NOT_CONFIGURED");
  const basic = btoa(`${consumerKey}:${consumerSecret}`);
  const res = await fetch(`${darajaBase}/oauth/v1/generate?grant_type=client_credentials`, {
    headers: { Authorization: `Basic ${basic}` },
  });
  if (!res.ok) throw new Error(`DARAJA_TOKEN_FAILED: ${await res.text()}`);
  const data = await res.json();
  cachedToken = { token: data.access_token, expiresAt: Date.now() + (Number(data.expires_in) - 60) * 1000 };
  return cachedToken.token;
}

/** Normalize 07XX / +2547XX / 2547XX into Daraja's 2547XXXXXXXX. */
function normalizePhone(raw: string): string | null {
  let p = (raw ?? "").replace(/[\s-]/g, "");
  if (p.startsWith("+")) p = p.slice(1);
  if (p.startsWith("0")) p = `254${p.slice(1)}`;
  return /^254(7|1)\d{8}$/.test(p) ? p : null;
}

/** Daraja timestamps are Kenyan local time (UTC+3), shared with the password. */
function darajaTimestamp(): string {
  const nairobi = new Date(Date.now() + 3 * 60 * 60 * 1000);
  const p = (n: number) => String(n).padStart(2, "0");
  return `${nairobi.getUTCFullYear()}${p(nairobi.getUTCMonth() + 1)}${p(nairobi.getUTCDate())}` +
    `${p(nairobi.getUTCHours())}${p(nairobi.getUTCMinutes())}${p(nairobi.getUTCSeconds())}`;
}

async function stkPush(opts: {
  amount: number;
  phone: string;
  orderNumber: string;
  itemTitle: string;
}): Promise<{ checkoutRequestId: string; merchantRequestId: string; customerMessage: string }> {
  const token = await darajaToken();
  const ts = darajaTimestamp();
  const password = btoa(`${shortcode}${passkey}${ts}`);
  const res = await fetch(`${darajaBase}/mpesa/stkpush/v1/processrequest`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      BusinessShortCode: shortcode,
      Password: password,
      Timestamp: ts,
      TransactionType: "CustomerPayBillOnline",
      Amount: Math.round(opts.amount),
      PartyA: opts.phone,
      PartyB: shortcode,
      PhoneNumber: opts.phone,
      CallBackURL: `${supabaseUrl}/functions/v1/mpesa/callback`,
      AccountReference: opts.orderNumber.slice(0, 12),
      TransactionDesc: opts.itemTitle.slice(0, 40),
    }),
  });
  const data = await res.json();
  if (data.ResponseCode !== "0" || !data.CheckoutRequestID) {
    throw new Error(`STK_PUSH_REJECTED: ${data.errorMessage ?? data.ResponseDescription ?? "unknown"}`);
  }
  return {
    checkoutRequestId: data.CheckoutRequestID,
    merchantRequestId: data.MerchantRequestID,
    customerMessage: data.CustomerMessage ?? "Check your phone to complete payment",
  };
}

/** Resolve the caller's campus identity triple (user_id, student_id, university_email). */
async function callerIdentity(userId: string): Promise<string[]> {
  const res = await fetch(
    `${supabaseUrl}/rest/v1/campusprofile?user_id=eq.${encodeURIComponent(userId)}&select=user_id,student_id,university_email`,
    { headers: adminHeaders },
  );
  if (!res.ok) throw new Error("PROFILE_LOOKUP_FAILED");
  const rows = await res.json();
  const row = rows?.[0];
  if (!row) return [userId];
  return [row.user_id, row.student_id, row.university_email]
    .filter((v: unknown): v is string => typeof v === "string" && v.trim().length > 0)
    .map((v: string) => v.toLowerCase().trim());
}

function identityMatches(value: string, triple: string[]): boolean {
  const v = (value ?? "").toLowerCase().trim();
  if (!v) return false;
  return triple.includes(v);
}

async function getItem(itemId: string): Promise<Record<string, unknown> | null> {
  const res = await fetch(
    `${supabaseUrl}/rest/v1/campus_market_items?id=eq.${encodeURIComponent(itemId)}&select=*`,
    { headers: adminHeaders },
  );
  if (!res.ok) return null;
  const rows = await res.json();
  return rows?.[0] ?? null;
}

async function getOrderByHoldId(holdId: string): Promise<Record<string, unknown> | null> {
  const res = await fetch(
    `${supabaseUrl}/rest/v1/campus_escrow_orders?provider_hold_id=eq.${encodeURIComponent(holdId)}&select=*&order=created_at.desc&limit=1`,
    { headers: adminHeaders },
  );
  if (!res.ok) return null;
  const rows = await res.json();
  return rows?.[0] ?? null;
}

async function getActiveOrderForItem(itemId: string, statuses: string[]): Promise<Record<string, unknown> | null> {
  const statusFilter = `(${statuses.map((s) => `"${s}"`).join(",")})`;
  const res = await fetch(
    `${supabaseUrl}/rest/v1/campus_escrow_orders?market_item_id=eq.${encodeURIComponent(itemId)}&status=in.${statusFilter}&select=*&order=created_at.desc&limit=1`,
    { headers: adminHeaders },
  );
  if (!res.ok) return null;
  const rows = await res.json();
  return rows?.[0] ?? null;
}

async function updateOrder(id: string, patch: Record<string, unknown>): Promise<void> {
  const res = await fetch(
    `${supabaseUrl}/rest/v1/campus_escrow_orders?id=eq.${encodeURIComponent(id)}`,
    {
      method: "PATCH",
      headers: adminHeaders,
      body: JSON.stringify({ ...patch, updated_at: new Date().toISOString() }),
    },
  );
  if (!res.ok) throw new Error(`ORDER_UPDATE_FAILED: ${await res.text()}`);
}

async function setItemStatus(itemId: string, status: string): Promise<void> {
  const res = await fetch(
    `${supabaseUrl}/rest/v1/campus_market_items?id=eq.${encodeURIComponent(itemId)}`,
    {
      method: "PATCH",
      headers: adminHeaders,
      body: JSON.stringify({ status }),
    },
  );
  if (!res.ok) throw new Error(`ITEM_STATUS_UPDATE_FAILED: ${await res.text()}`);
}

/** Shape an escrow row into the Android EscrowOrder model (camelCase). */
function toOrderModel(row: Record<string, any>): Record<string, unknown> {
  return {
    id: row.id,
    orderNumber: row.order_number,
    marketItemId: row.market_item_id,
    buyerUserId: row.buyer_user_id,
    sellerUserId: row.seller_user_id,
    itemTitle: row.item_title,
    amount: row.amount,
    currencyCode: row.currency_code,
    paymentProvider: row.payment_provider,
    paymentMethodId: row.payment_method,
    providerHoldId: row.provider_hold_id ?? null,
    status: row.status,
    rendezvousSpot: row.rendezvous_spot ?? "",
    buyerNote: row.buyer_note ?? "",
    createdAtEpochMs: row.created_at_epoch_ms ?? 0,
    fundedAtEpochMs: row.funded_at_epoch_ms ?? null,
    releasedAtEpochMs: row.released_at_epoch_ms ?? null,
  };
}

async function handleInitiate(req: Request): Promise<Response> {
  const user = await requireUser(req);
  const body = await req.json().catch(() => null);
  if (!body) return fail("BAD_REQUEST", "JSON body required", 400);

  const itemId: string = (body.itemId ?? "").trim();
  const buyerUserId: string = (body.buyerUserId ?? "").trim();
  const buyerPhoneRaw: string = (body.buyerPhone ?? "").trim();
  const amount = Number(body.amount);
  const orderNumber: string = (body.orderNumber ?? "").trim();
  if (!itemId || !buyerUserId || !orderNumber || !Number.isFinite(amount) || amount <= 0) {
    return fail("INVALID_REQUEST", "itemId, buyerUserId, orderNumber and a positive amount are required", 400);
  }

  const triple = await callerIdentity(user.id);
  if (!identityMatches(buyerUserId, triple)) {
    return fail("FORBIDDEN", "Only the buyer may start payment for this order", 403);
  }

  const item = await getItem(itemId);
  if (!item) return fail("ITEM_NOT_FOUND", "Listing does not exist", 404);
  if (item.status !== "ACTIVE") return fail("ITEM_NOT_AVAILABLE", "Listing is no longer available", 409);
  if (Math.round(amount) !== Math.round(Number(item.price))) {
    return fail("AMOUNT_MISMATCH", "Payment amount must equal the listing price", 400);
  }

  const phone = normalizePhone(buyerPhoneRaw);
  if (!phone) return fail("INVALID_PHONE", "A valid Safaricom number (2547XXXXXXXX) is required", 400);

  const push = await stkPush({
    amount,
    phone,
    orderNumber,
    itemTitle: String(item.title ?? "Campus purchase"),
  });

  const nowMs = Date.now();
  const orderId = `esc_${crypto.randomUUID().replace(/-/g, "").slice(0, 20)}`;
  const insertRes = await fetch(`${supabaseUrl}/rest/v1/campus_escrow_orders`, {
    method: "POST",
    headers: adminHeaders,
    body: JSON.stringify({
      id: orderId,
      order_number: orderNumber,
      market_item_id: itemId,
      item_title: item.title ?? "",
      buyer_user_id: buyerUserId,
      seller_user_id: item.seller_id ?? "",
      amount,
      currency_code: item.currency ?? "KES",
      payment_provider: "mpesa",
      payment_method: String(body.paymentMethodId ?? "MOBILE_MONEY_STK"),
      provider_hold_id: push.checkoutRequestId,
      status: "PENDING_PAYMENT",
      rendezvous_spot: String(body.rendezvousSpot ?? item.pickup_spot_name ?? ""),
      buyer_note: String(body.buyerNote ?? ""),
      created_at: new Date(nowMs).toISOString(),
      updated_at: new Date(nowMs).toISOString(),
      created_at_epoch_ms: nowMs,
    }),
  });
  if (!insertRes.ok) {
    throw new Error(`ORDER_INSERT_FAILED: ${await insertRes.text()}`);
  }

  return json({
    order: {
      id: orderId,
      orderNumber,
      marketItemId: itemId,
      buyerUserId,
      sellerUserId: item.seller_id ?? "",
      itemTitle: item.title ?? "",
      amount,
      currencyCode: item.currency ?? "KES",
      paymentProvider: "mpesa",
      paymentMethodId: String(body.paymentMethodId ?? "MOBILE_MONEY_STK"),
      providerHoldId: push.checkoutRequestId,
      status: "PENDING_PAYMENT",
      rendezvousSpot: String(body.rendezvousSpot ?? item.pickup_spot_name ?? ""),
      buyerNote: String(body.buyerNote ?? ""),
      createdAtEpochMs: nowMs,
      fundedAtEpochMs: null,
      releasedAtEpochMs: null,
    },
    customerMessage: push.customerMessage,
  });
}

async function handleRelease(req: Request): Promise<Response> {
  const user = await requireUser(req);
  const body = await req.json().catch(() => null);
  const itemId: string = (body?.itemId ?? "").trim();
  const scannedCode: string = (body?.scannedCode ?? "").trim();
  if (!itemId || !scannedCode) return fail("INVALID_REQUEST", "itemId and scannedCode are required", 400);

  const order = await getActiveOrderForItem(itemId, ["FUNDS_HELD"]);
  if (!order) return fail("NO_ACTIVE_ORDER", "No funded escrow order exists for this listing", 404);

  // Bounded, row-locked buyer-only challenge (release_attempts capped server-side).
  const verifyRes = await fetch(`${supabaseUrl}/rest/v1/rpc/verify_escrow_release_code`, {
    method: "POST",
    headers: {
      apikey: req.headers.get("apikey") ?? "",
      Authorization: req.headers.get("Authorization") ?? "",
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ p_order_id: order.id, p_scanned_code: scannedCode }),
  });
  const verified = verifyRes.ok && (await verifyRes.json()) === true;
  if (!verified) return fail("CODE_INVALID", "Release code rejected", 403);

  const nowMs = Date.now();
  await updateOrder(order.id, {
    status: "RELEASED_TO_SELLER",
    released_at_epoch_ms: nowMs,
  });
  await setItemStatus(itemId, "SOLD");

  return json({
    order: { ...toOrderModel({ ...order, status: "RELEASED_TO_SELLER", released_at_epoch_ms: nowMs }) },
  });
}

async function handleRefund(req: Request): Promise<Response> {
  const user = await requireUser(req);
  const body = await req.json().catch(() => null);
  const itemId: string = (body?.itemId ?? "").trim();
  const orderId: string = (body?.orderId ?? "").trim();
  if (!itemId && !orderId) return fail("INVALID_REQUEST", "itemId or orderId is required", 400);

  // Resolve the escrow order either by listing id or by order id.
  let order: Record<string, unknown> | null = null;
  if (orderId) {
    const byId = await fetch(`${supabaseUrl}/rest/v1/campus_escrow_orders?id=eq.${encodeURIComponent(orderId)}&select=*`, {
      method: "GET",
      headers: adminHeaders,
    });
    const rows: unknown = byId.ok ? await byId.json() : [];
    order = Array.isArray(rows) && rows.length > 0 ? (rows[0] as Record<string, unknown>) : null;
  }
  if (!order) {
    order = await getActiveOrderForItem(itemId, ["FUNDS_HELD"]);
  }
  if (!order) return fail("NO_ACTIVE_ORDER", "No funded escrow order exists for this listing", 404);

  const triple = await callerIdentity(user.id);
  const isBuyer = identityMatches(String(order.buyer_user_id ?? ""), triple);
  const isSeller = identityMatches(String(order.seller_user_id ?? ""), triple);
  if (!isBuyer && !isSeller) {
    return fail("FORBIDDEN", "Only the buyer or seller may cancel this escrow", 403);
  }

  // Sandbox: no reverse transfer exists; production adds the Daraja B2C reversal
  // here once MPESA_B2C_* secrets (initiator, security credential, certificate) are set.
  const nowMs = Date.now();
  await updateOrder(order.id, {
    status: "REFUNDED_TO_BUYER",
    updated_at: new Date(nowMs).toISOString(),
  });
  await setItemStatus(String(order.market_item_id ?? itemId), "ACTIVE");

  return json({ order: { ...toOrderModel({ ...order, status: "REFUNDED_TO_BUYER" }) } });
}

async function handleCallback(req: Request): Promise<Response> {
  const body = await req.json().catch(() => null);
  const stk = body?.Body?.stkCallback;
  const holdId: string = stk?.CheckoutRequestID ?? "";
  if (!holdId) return json({ ResultCode: 0, ResultDesc: "Ignored" });

  const order = await getOrderByHoldId(holdId);
  if (!order || order.status !== "PENDING_PAYMENT") {
    return json({ ResultCode: 0, ResultDesc: "Ignored" });
  }

  if (stk.ResultCode === 0) {
    const nowMs = Date.now();
    await updateOrder(order.id, {
      status: "FUNDS_HELD",
      funded_at_epoch_ms: nowMs,
    });
    await setItemStatus(String(order.market_item_id), "RESERVED");
  } else {
    await updateOrder(order.id, { status: "FAILED" });
  }

  // Daraja requires a 200 acknowledgement or it retries the callback.
  return json({ ResultCode: 0, ResultDesc: "Accepted" });
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response(null, { status: 204, headers: corsHeaders });
  if (req.method !== "POST") return new Response(null, { status: 405, headers: corsHeaders });

  const url = new URL(req.url);
  try {
    if (url.pathname.endsWith("/callback")) return await handleCallback(req);
    if (url.pathname.endsWith("/mpesa")) {
      const body = await req.json().catch(() => null);
      switch (body?.action) {
        case "initiate":
          return await handleInitiate(req);
        case "release":
          return await handleRelease(req);
        case "refund":
          return await handleRefund(req);
        default:
          return fail("UNKNOWN_ACTION", "action must be initiate, release or refund", 400);
      }
    }
    return fail("NOT_FOUND", "Unknown path", 404);
  } catch (error) {
    if (error?.message === "UNAUTHORIZED") return fail("UNAUTHORIZED", "A signed-in user is required", 401);
    if (error?.message === "MPESA_NOT_CONFIGURED") {
      return fail("PROVIDER_NOT_CONFIGURED", "M-Pesa credentials are not set", 503);
    }
    const message = error instanceof Error ? error.message : "UNEXPECTED_ERROR";
    if (message.startsWith("STK_PUSH_REJECTED")) return fail("PROVIDER_REJECTED", message, 502);
    if (message.startsWith("DARAJA_TOKEN_FAILED")) return fail("PROVIDER_TOKEN_FAILED", message, 502);
    return fail("UNEXPECTED_ERROR", message, 500);
  }
});
