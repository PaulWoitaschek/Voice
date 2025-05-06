# Voice Developer Guidelines (for Junie)

## Project Overview

Voice is a minimal, user‑focused audiobook player for Android, built for reliability and simplicity. Licensed under GNU
GPLv3.

## Tech Stack

* **Language**: Kotlin
* **Build System**: Gradle (Kotlin DSL)
* **Architecture**: Modular (feature-specific modules)
* **UI**: Jetpack Compose, Material 3
* **Dependency Injection**: Dagger + Anvil
* **Navigation**: Conductor (migrating to Compose Navigation)
* **Media Playback**: ExoPlayer (Media3)
* **Image Loading**: Coil
* **Serialization**: Kotlin Serialization
* **Storage**: DataStore, SQL
* **Analytics**: Firebase (proprietary) / no‑op (libre)

## Project Structure

Each module contains its own `build.gradle.kts`, `src/main/kotlin`, and `src/test/kotlin`:

* **app**: Main application
* **common**: Shared utilities
* **data**: Repositories and data layer
* **playback**: Audio playback logic
* **scanner**: File scanning and metadata extraction
* **cover**: Cover art handling
* **settings**: Configuration UI
* …additional feature modules

## Build & Run

   ```bash
   ./gradlew :app:assemblePlayProprietaryDebug
   ```

**Build Variants**

* `proprietary`: Includes Firebase analytics and crash reporting
* `libre`: No proprietary dependencies (F-Droid compatible)

## Testing

* Frameworks: JUnit, Mockk, Kotest (Only assertions), Robolectric
* Run all unit tests:

  ```bash
  ./gradlew voiceUnitTest
  ```
* Run tests for a specific module:

  ```bash
  ./gradlew :<moduleName>:test
  ```

## Code Style

* Enforce with Ktlint:
    * Check: `./gradlew lintKotlin`
    * Auto‑format: `./gradlew formatKotlin`
* Prefer small functions and clear identifiers
* Minimal inline comments

## Useful Scripts

* Build project: `./gradlew build`
* Install debug APK: `./gradlew installDebug`
* Custom scripts: `./scripts/run.sh`

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
