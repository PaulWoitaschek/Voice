# Voice Developer Guidelines (for Junie)

## Project Overview

Voice is a minimal, user‑focused audiobook player for Android, built for reliability and minimalism.

## Tech Stack

* **Language**: Kotlin
* **Build System**: Gradle (Kotlin DSL)
* **Architecture**: Modular (feature-specific modules)
* **UI**: Jetpack Compose, Material 3
* **Dependency Injection**: Metro
* **Navigation**: Navigation3
* **Media Playback**: ExoPlayer (Media3)
* **Image Loading**: Coil
* **Serialization**: Kotlin Serialization
* **Storage**: room

## Project Structure

Each module contains its own `build.gradle.kts`, `src/main/kotlin`, and `src/test/kotlin`:

**Infrastructure**:

* `:app` - Main application entry point and DI setup
* `:navigation` - Navigation framework
* `:plugins` - Gradle build plugins
* `:scripts` - Build and utility scripts

**Core Modules** (shared domain logic):

* `:core:common` - Legacy, to be removed
* `:core:ui` - UI components and theming
* `:core:data:api` & `:core:data:impl` - Data layer interfaces and implementations
* `:core:playback` - Audio playback logic
* `:core:scanner` - File scanning and metadata extraction
* `:core:strings` - Localized strings
* `:core:search` - Search functionality
* `:core:documentfile` - File system abstractions
* `:core:logging:core`, `:core:logging:crashlytics`, `:core:logging:debug` - Logging implementations
* `:core:remoteconfig:core`, `:core:remoteconfig:firebase`, `:core:remoteconfig:noop` - Remote configuration
* `:core:sleeptimer:api` & `:core:sleeptimer:impl` - Sleep timer core logic

**Feature Modules** (UI screens and features):

* `:features:playbackScreen` - Book playing interface
* `:features:bookOverview` - Library/book list
* `:features:sleepTimer` - Sleep timer functionality
* `:features:settings` - App settings
* `:features:folderPicker` - Folder selection
* `:features:cover` - Cover art management
* `:features:onboarding` - First-time user flow
* `:features:bookmark` - Bookmark management
* `:features:widget` - Home screen widget functionality
* `:features:review:play` & `:features:review:noop` - App review prompts

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
  ./gradlew :<moduleName>:testDebugUnitTest
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
