## Project Overview

Voice is a minimal, user‑focused audiobook player for Android, built for reliability and minimalism.

## Architecture

The project architecture and gradle module structure is defined in [the Architecture Docs](docs/architecture.md)

## Commands

- Assemble the app: `./gradlew :app:assemblePlayDebug`
- Run all tests `./gradlew voiceUnitTest`
- Run tests of a module: `./gradlew :<moduleName>:testDebugUnitTest`
- Create and register a new gradle module: `./scripts/new_module.kts :features:<name>`

## Information Lookup

- Project Dependencies are declared in `gradle/libs.versions.toml`
- Get an overview of all current modules from the `settings.gradle.kts` file

## Code Style

* Ignore formatting, this is done by ktlint
* Prefer small functions and clear identifiers
* Minimal inline comments

## Minimal Code Documentation

* **Self‑Describing Code**
  * Clear, concise names; one purpose per function/variable.
* **Comment Only “Why”**
  * Don’t explain “what” or “how.” Refactor if unclear.
  * Use comments solely for rationale, workarounds, non‑obvious side‑effects.
* **KDoc Sparingly**
  * Public API: only when names and signatures don’t fully convey behavior or contracts.
* **Tests as Documentation**
  * Write descriptive tests illustrating usage and edge cases.
* **Continuous Pruning**
  * Remove or update any comments that outlive their usefulness.
