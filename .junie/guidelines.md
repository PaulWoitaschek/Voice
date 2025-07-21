# Voice Developer Guidelines (for Junie)

## Project Overview

Voice is a minimal, user‑focused audiobook player for Android, built for reliability and minimalism.

## Tech Stack

* **Language**: Kotlin
* **Build System**: Gradle (Kotlin DSL)
* **Architecture**: Modular (feature-specific modules)
* **UI**: Jetpack Compose, Material 3
* **Dependency Injection**: Metro
* **Navigation**: Conductor (migrating to Compose Navigation)
* **Media Playback**: ExoPlayer (Media3)
* **Image Loading**: Coil
* **Serialization**: Kotlin Serialization
* **Storage**: foom

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
   ./gradlew :app:assemblePlayDebug
   ```

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
