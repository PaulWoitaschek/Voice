---
name: format-and-test
description: Formatting Kotlin and writing or running tests in Voice. Use when adding or changing tests, before committing, or when checking that a change is done.
---

# Formatting and testing

## Formatting

- Don't hand-format. Run `./gradlew formatKotlin`, then `./gradlew lintKotlin` and fix what it can't auto-fix.
- Rules come from ktlint with the Compose rule set, configured in `.editorconfig`.

## Running tests

Start narrow, then broaden:

- One class: `./gradlew :features:settings:testDebugUnitTest --tests "voice.features.settings.SettingsViewModelTest"`
- One library module: `./gradlew :<module>:testDebugUnitTest`
- App module: `./gradlew :app:testFreeDebugUnitTest`
- All unit tests: `./gradlew voiceUnitTest`. Run this when touching shared behavior, cross-module contracts, or broad
  refactors.

CI runs `./gradlew voiceUnitTest lintKotlin :app:assembleFreeDebug`. If you can't run something, say why and give the
command.

## Writing tests

- Cover changed behavior: domain logic, view models, persistence, navigation. Bug fixes get a regression test.
- Use `kotlin.test` with backtick names that describe behavior. Power-assert is enabled for `assert`, `assertEquals`,
  `assertTrue`, and `assertNull`, so skip message strings.
- Prefer in-memory fakes over mocks:
  - `MemoryFeatureFlag(value)` from `:core:featureflag`.
  - `MemoryDataStore(initial)`, copied per module into test sources. Copy an existing one (e.g.
    `core/playback/src/test/kotlin/voice/core/playback/MemoryDataStore.kt`); don't create a shared module.
  - mockk only for collaborators without a fake, such as `Navigator`.
- Coroutines: create a `TestScope`, run with `scope.runTest { }`, and inject
  `DispatcherProvider(scope.coroutineContext, scope.coroutineContext, scope.coroutineContext)`.
- If the Android framework is needed, use Robolectric via `@RunWith(AndroidJUnit4::class)`. The build sets the SDK.
- Compose view state: Molecule plus Turbine.

  ```kotlin
  backgroundScope.launchMolecule(RecompositionMode.Immediate) {
    viewModel.viewState()
  }.test {
    awaitItem()
  }
  ```
