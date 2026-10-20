# SafeTrade Payment Provider Socket

Campus SafeTrade never fakes a successful escrow lock. Checkout calls a pluggable
`PaymentProvider`. Until credentials are set, the UI refuses to confirm escrow.

## Plug in a free / production API

1. Add to `secrets.properties` (gitignored):

```properties
PAYMENT_API_BASE_URL=https://your-adapter.example/v1
PAYMENT_API_KEY=pk_...
PAYMENT_API_SECRET=sk_...
PAYMENT_PROVIDER_ID=paystack
PAYMENT_PROVIDER_DISPLAY_NAME=Paystack Escrow
```

2. Rebuild the app (`./gradlew :app:assembleDebug`).
3. `PaymentProviderRegistry` will select `HttpEscrowPaymentProvider` automatically.

## Required REST contract

Your adapter (Edge Function, Cloudflare Worker, or vendor wrapper) must expose:

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/escrow/holds` | Authorize / lock buyer funds |
| `POST` | `/escrow/holds/{holdId}/release` | Pay seller after QR verify |
| `POST` | `/escrow/holds/{holdId}/refund` | Return funds to buyer |
| `GET`  | `/escrow/holds/{holdId}` | Poll hold status |

Auth headers sent by the app:

- `Authorization: Bearer {PAYMENT_API_SECRET}`
- `X-Api-Key: {PAYMENT_API_KEY}`
- `X-Payment-Provider: {PAYMENT_PROVIDER_ID}`

### `POST /escrow/holds` request (JSON)

```json
{
  "orderId": "escrow_…",
  "orderReference": "ORD-1234",
  "marketItemId": "item_…",
  "itemTitle": "…",
  "amount": 2500,
  "currency": "KES",
  "buyerUserId": "…",
  "sellerUserId": "…",
  "paymentMethodId": "mobile_money_stk",
  "buyerPhone": "+2547…",
  "rendezvousSpot": "…",
  "buyerNote": "",
  "metadata": {}
}
```

### Success response

```json
{
  "holdId": "hold_abc",
  "status": "FUNDS_HELD",
  "nextActionUrl": null,
  "reference": "optional-provider-ref"
}
```

Use `status: "PENDING_PAYMENT"` + `nextActionUrl` when the buyer must complete STK/3DS.

## Native SDK later

Implement `PaymentProvider` for a vendor SDK and register it in
`PaymentProviderRegistry.install(...)`. Hub UI stays unchanged.

## Database

Orders persist to `campus_escrow_orders` (migration
`20261020000010_create_campus_escrow_orders.sql`).
