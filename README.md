# Campus Network ("Patriarch")

Campus Network is a Kotlin multi-module Android app (Jetpack Compose, Navigation Compose,
Room, Ktor) backed by Supabase (auth + PostgREST + Realtime + Edge Functions), Stream (chat/video),
Google Maps, Firebase Cloud Messaging, Cloudinary, and an optional payment-provider adapter for SafeTrade escrow.

## Modules

| Module | Purpose |
|--------|---------|
| `:app` | Startup (`CampusApplication`), auth screens, navigation host (`CampusNavHost`), FCM/push handling |
| `:core:model` | Domain models (profile, escrow/payment, market, server, place, call signaling) |
| `:core:database` | Room local cache (entities, DAOs, versioned migrations) |
| `:core:security` | Runtime integrity + session-access policy checks |
| `:core:network` | Supabase/Ktor clients, Stream chat & video engines, Cloudinary/OpenRouter/payment adapters, encrypted session store |
| `:core:data` | Repositories (servers, channels, messages, marketplace, escrow, flash meets, live stream, profile) |
| `:core:ui` | Shared Compose theme/components |
| `:feature:map` | Campus map, flash meets, events |
| `:feature:channels` | Discord-like servers, channels, chat |
| `:feature:messages` | Direct chats, typing presence, audio/video calling via Stream |
| `:feature:hub` | Marketplace, micro-gigs, live feed, SafeTrade escrow |
| `:feature:profile` | Gamified student profile, wallet |

## Build

```bash
./gradlew :app:assembleDebug
```

Do **not** use `--offline` after adding new dependencies; Gradle must resolve them from MavenCentral/google().

### Local secrets (`secrets.properties` — gitignored, never commit)

Create `secrets.properties` at the repo root. Only client-safe values go here:

```
GOOGLE_MAPS_API_KEY=...
STREAM_API_KEY=...
SUPABASE_URL=...
SUPABASE_ANON_KEY=...
GOOGLE_CLIENT_ID_ANDROID=...
GOOGLE_CLIENT_ID_WEB=...
```

Privileged keys (`STREAM_SECRET_KEY`, `CLOUDINARY_API_SECRET`, `OPENROUTER_API_KEY`,
`PAYMENT_API_*`) are **not** shipped in the APK. They belong in Supabase Edge Function environment
variables (see "Supabase secrets to upload" below).

## Architecture

- `CampusApplication` initializes Room, notification polling, Stream Chat, and Stream Video.
- `MainActivity` renders `MainAppScreen`, which builds repositories and ViewModels manually
  (no DI framework). `CampusNavHost` wires the 5-tab graph plus auth/onboarding routes.
- Data flow: **Compose screen → ViewModel (`StateFlow`/`UiState`) → `core:data` repository → Room cache and/or `core:network` integration**.
- Supabase requests are made with the signed-in user's JWT (refreshed automatically via
  `SupabaseSessionStore.validAccessToken`). The public anon key is never used as a fallback for
  authenticated data access.
- Session tokens are stored in `EncryptedSharedPreferences` (AES256-GCM key, AES256-SIV pref keys),
  migrated from the legacy plaintext `socialpulse-auth-token` SharedPreferences.
- Room uses an additive migration (`13 → 14`) with `exportSchema = true`; destructive migration is removed.

## Supabase secrets to upload

Run once per environment (replace `${PROJECT_REF}`):

```bash
supabase secrets set \
  STREAM_API_KEY=... \
  STREAM_SECRET_KEY=... \
  SUPABASE_URL=https://${PROJECT_REF}.supabase.co \
  SUPABASE_ANON_KEY=... \
  SUPABASE_SERVICE_ROLE_KEY=... \
  RESEND_API_KEY=... \
  CLOUDINARY_CLOUD_NAME=... \
  CLOUDINARY_API_KEY=... \
  CLOUDINARY_API_SECRET=... \
  OPENROUTER_API_KEY=...
```

These back the Edge Functions:
- `supabase/functions/stream-token` — mints Stream JWTs, caller identity verified against Supabase Auth + `campusprofile`
- `supabase/functions/send-verification-otp` — Resend OTP email (needs `RESEND_API_KEY`)
- `supabase/functions/cloudinary-signature` — short-lived signed Cloudinary upload params (needs Cloudinary creds)
- `supabase/functions/ai-completions` — bounded OpenRouter proxy (needs `OPENROUTER_API_KEY`)

## Database migrations

Apply migrations in `supabase/migrations/` in filename order to the target Supabase project
(Supabase CLI `supabase db push` or the SQL Editor). Do **not** edit a migration after it has been
deployed; add a new forward-only migration instead.

The security-hardening migration
`20261023000000_restrict_identity_and_financial_writes.sql` narrows the previously open RLS policies
on chat messages, escrow orders, server entities, events, flash meets, marketplace, live streams,
micro-gigs, stream comments, notifications, and friendships to authenticated, ownership-scoped access.
It depends on the Android release that sends Supabase user JWTs (this change).

## Tests

- `core/network/src/test/.../RemoteDatabaseWritesIntegrationTest.kt` — live Supabase write tests.
  Opt in with `RUN_LIVE_SUPABASE_TESTS=1` plus disposable `SUPABASE_TEST_URL`,
  `SUPABASE_TEST_ANON_KEY`, and `SUPABASE_TEST_USER_JWT` env vars. It is skipped by default and must
  never point at production.
- `feature/messages/src/test/.../OnDeviceTranscriberTest.kt` — local audio transcription test.

## Security notes

- Stream/Cloudinary/OpenRouter/payment secrets are **not** in `BuildConfig` and must never be added
  to `secrets.properties` for the client build.
- Payment checkout is gated on `PaymentProviderRegistry.installHttpProvider(...)`, which must be
  wired from out-of-band configuration; the client never holds payment API secrets.
- Google sign-in requires a Supabase-accepted ID token; the previous Google-account-profile fallback
  that fabricated a local session was removed.
- OTP verification requires a real Supabase Auth session; fabricated access/refresh tokens were removed.
