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
- Test dependencies come from the convention plugin: every module's unit tests already have kotlin.test, JUnit, mockk,
  Turbine, coroutines-test, Robolectric, and AndroidX Test, and Compose modules also have Molecule. Don't add these to a
  module's build file. Only add what a single module needs, such as `room-testing`.
- Mock collaborators with mockk:
  - `mockk<X>(relaxUnitFun = true)`, with `every`/`coEvery` for each value the code under test reads.
  - `relaxed = true` only when the returned values don't matter; plain `mockk<X>()` for constructor fillers that are
    never called.
  - Assert interactions with `verify`/`coVerify`, using `slot`/`capture` for arguments. Don't write a fake that records
    calls. The return value checker rejects verifying a call that returns a value, so discard it:
    `coVerify { val _ = billing.consume(token) }`.
- Use the real class when it's cheap: pure classes, NoOp implementations, `MemoryFeatureFlag(value)` from
  `:core:featureflag`, and `MemoryDataStore(initial)`. `MemoryDataStore` is copied once per module into test sources
  (e.g. `core/playback/src/test/kotlin/voice/core/playback/MemoryDataStore.kt`); don't create a shared module.
- Write a fake only when the test needs real state, such as writes that later reads observe or flows that must re-emit
  after a change. Reuse one the module's tests already have; otherwise keep one fake per type per module, in its own
  file. A fake that only returns canned values, records calls, or copies production logic should be a mock.
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
