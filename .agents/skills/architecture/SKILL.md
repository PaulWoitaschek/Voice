---
name: architecture
description: Voice module boundaries and wiring. Use before adding or moving code between modules, creating a module, changing Gradle project dependencies, Metro DI wiring, or Navigation3 destinations.
---

# Voice architecture

Background and diagram: `docs/architecture.md`. Module inventory: `settings.gradle.kts`.

## Where code goes

- `:features:*`: user-facing screens and flows (Compose UI and presentation logic).
- `:core:*`: reusable services and contracts: data, playback, scanning, search, logging, strings, shared UI.
- `:app`: DI graph and product flavor wiring (`free` / `play`). `:navigation`: `Destination` and navigation abstractions.
  Keep both thin.
- New functionality starts in a feature module. Extract to `:core` once a second feature needs it.

## Dependency rules

- Features depend on `:core:*` and `:navigation`, never on other features. Existing exceptions: a feature's own `:api`
  module (e.g. `:features:support:api`) and the legacy `:features:playbackScreen` → `:features:sleepTimer`. Don't add more.
- Core never depends on features.
- A contract with several implementations is split into `:api` plus implementation modules (`noop`, `firebase`, `play`,
  `free`, `impl`). Consumers depend on `:api` only; `:app` picks the implementation, often per flavor
  (`playImplementation` / `freeImplementation`).
- Reference modules via type-safe accessors (`implementation(projects.core.data.api)`) and libraries via
  `gradle/libs.versions.toml` aliases. No hardcoded versions.

## New module

1. `./scripts/new_module.main.kts :features:<name>` creates `build.gradle.kts` with `voice.library`, the
   `src/{main,test}/kotlin/voice/...` folders, and registers the module in `settings.gradle.kts`.
2. Add `id("voice.compose")` for Compose and `alias(libs.plugins.metro)` for DI.
3. Add it to `app/build.gradle.kts` so its DI contributions reach the graph.

Convention plugins (`voice.library`, `voice.app`, `voice.compose`, `voice.ktlint`) live in `plugins/src/main/kotlin`.

## DI (Metro)

- Constructor injection with `@Inject`; bind implementations with `@ContributesBinding(AppScope::class)`.
- Providers go in `@BindingContainer @ContributesTo(AppScope::class) object …`.
- The graph is assembled in `app/src/main/kotlin/voice/app/di`.

## Navigation

1. Add the destination to `Destination` in `:navigation`.
2. In the feature, provide a `NavEntryProvider<Destination.X>` `@IntoSet` from a binding container. Example:
   `BookmarkProvider` in `features/bookmark/src/main/kotlin/voice/features/bookmark/BookmarkScreen.kt`.
3. Navigate through the injected `Navigator`.
