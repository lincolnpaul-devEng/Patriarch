# Copilot instructions

## Project shape

This is a Kotlin/Jetpack Compose Android app. `:app` is the composition root: `CampusApplication` initializes app-wide services, while `MainAppScreen` and `CampusNavHost` wire repositories, ViewModels, navigation, and route access checks. Feature UI and behavior live in `:feature:map`, `:feature:channels`, `:feature:messages`, `:feature:hub`, and `:feature:profile`. Shared code is split across `:core:model` (domain models and policies), `:core:security`, `:core:database` (Room entities/DAOs), `:core:network` (Supabase, Stream, and other network clients), `:core:data` (repositories), and `:core:ui` (shared Compose components and theme).

The usual data path is feature Composables and ViewModels → `:core:data` repositories → Room DAOs and/or `:core:network`. Repositories commonly expose Room-backed Flows and perform explicit remote synchronization or writes; keep local persistence and remote behavior consistent. Supabase schema changes belong in timestamped `supabase/migrations/`; Edge Functions are in `supabase/functions/`. Chat and calling also use Stream SDKs, with real-time transport code in the shared network layer.

Dependencies are wired manually with `remember` and `ViewModelProvider.Factory` in the app UI; there is no DI framework. When changing a repository or ViewModel constructor, update its app-level factory/wiring. To add a navigation destination, update `Screen` and register it in `CampusNavHost`; bottom-tab destinations are separately enumerated in `Screen.bottomTabs`.

## Build, test, and lint

Use the Gradle wrapper and JDK 17. Android modules target compile SDK 35 and minimum SDK 26. Dependency versions are centralized in `gradle/libs.versions.toml`.

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

Run one local unit test by module and fully qualified class name, for example:

```bash
./gradlew :feature:messages:testDebugUnitTest --tests 'com.campus.network.feature.messages.audio.OnDeviceTranscriberTest'
```

`core:network` also contains `RemoteDatabaseWritesIntegrationTest`, which writes to a live Supabase project and attempts cleanup. Run it only when those remote side effects are intended:

```bash
./gradlew :core:network:testDebugUnitTest --tests 'com.campus.network.core.network.RemoteDatabaseWritesIntegrationTest'
```

## Repository conventions

- Feature UI is Compose-based. Collect UI state with lifecycle-aware Compose APIs where appropriate, and keep screen-specific behavior in the corresponding feature module rather than expanding `:app`.
- Shared Room persistence is defined through entities, DAOs, and `CampusDatabase` in `:core:database`; repository/domain code belongs in `:core:data` and `:core:model`, not in UI modules.
- Keep Supabase migrations, repository payloads/queries, and the corresponding models aligned. Use `docs/PAYMENT_PROVIDER_SOCKET.md` for the SafeTrade provider contract and registration pattern.
- Local build configuration is read from the gitignored `secrets.properties` file and exposed through generated `BuildConfig` values. Do not commit credentials or move server-only secrets into the Android client; server credentials belong in the relevant Edge Function environment.
- Follow `.agents/rules/ui-layout-and-text-wrapping.md` for Compose rows with titles and trailing badges/pills: constrain the title with weight, line limits, and ellipsis; keep trailing labels and prices on one line to prevent narrow vertical wrapping.
