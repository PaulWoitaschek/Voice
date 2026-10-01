# AGENTS.md

## Scope

- This file is the root agent contract for the whole repository.
- Keep it short, current, and repo-specific. Do not add task-only prompts, secrets, generic Kotlin/Android advice, or rules already enforced
  by ktlint/lint.
- If a rule only applies under one subtree, prefer a nested `AGENTS.md` there instead of growing this file.

## Source Of Truth

- Architecture and module boundaries: `docs/architecture.md`. Read it before changing dependencies, DI wiring, navigation, playback,
  scanning, data, or module structure.
- Module inventory: `settings.gradle.kts`.
- Dependency versions and plugin aliases: `gradle/libs.versions.toml`.
- Build conventions: `plugins/src/main/kotlin`.

## Commands

- Assemble the free debug app: `./gradlew :app:assembleFreeDebug`.
- Run all unit tests: `./gradlew voiceUnitTest`.
- Run unit tests for a library module: `./gradlew :<moduleName>:testDebugUnitTest`.
- Run app unit tests: `./gradlew :app:testFreeDebugUnitTest`.
- Create and register a new module: `./scripts/new_module.main.kts :features:<name>`.
- Discover available Gradle tasks with `./gradlew tasks --all` when unsure.

## Architecture Rules

- Put user-facing screens and flows in `:features:*`.
- Put reusable services, data contracts, playback, scanning, logging, strings, and shared UI in `:core:*`.
- Keep `:app` and `:navigation` focused on app wiring and navigation integration.
- Do not introduce feature-to-feature dependencies. Feature modules should depend on core modules and infrastructure abstractions.
  Exceptions: a feature's own `:api` module (e.g. `:features:support:api`), and the legacy `:features:playbackScreen` →
  `:features:sleepTimer`.
- Core modules must not depend on feature modules.
- Define project dependencies in Gradle files using version catalog entries; do not hardcode dependency versions outside
  `gradle/libs.versions.toml`.

## Implementation Rules

- Make the smallest change that satisfies the request and fits the existing module boundary.
- Prefer existing project patterns, fakes, stores, dispatchers, DI modules, and UI components over new abstractions.
- Keep functions small and names clear. Avoid comments unless they explain a non-obvious reason, workaround, or side effect.
- Don't hand-format. Run `./gradlew formatKotlin` before committing. The optional pre-commit hook (`docs/development.md`) runs
  `lintKotlin`; on failure it auto-formats and aborts the commit, so re-stage and commit again.
- Do not touch signing, release, Fastlane, CI, dependency upgrades, or large generated assets unless the task requires it.

## Testing

- Add or update focused tests for changed behavior, especially domain logic, state reducers/view models, persistence, navigation, and bug
  fixes.
- Prefer lightweight in-memory fakes: `MemoryFeatureFlag` (in `:core:featureflag`), and `MemoryDataStore` (copied per module into test
  sources; copy an existing one rather than adding a shared module).
- For Compose view state tests, use Molecule plus Turbine:

  ```kotlin
  backgroundScope.launchMolecule(RecompositionMode.Immediate) {
    viewModel.viewState()
  }.test {
    awaitItem()
  }
  ```

- Run the narrowest meaningful test first. Broaden to `./gradlew voiceUnitTest` when touching shared behavior, cross-module contracts, or
  risky refactors.
- If tests cannot be run, state the concrete reason and the command that should be run.

## Done Criteria

- Relevant tests or build checks have passed, or the reason they were not run is explicit.
- The diff is scoped to the requested behavior and does not rewrite unrelated code.
- New public behavior is covered by tests or clearly justified if not.
- Instructions in this file stay accurate. Fix stale commands or misleading guidance when found.
