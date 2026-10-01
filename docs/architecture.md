# Architecture

## Overview

Voice is built on a **modular, layered architecture**. The design separates infrastructure, shared domain logic, and user-facing features.
This modularization improves build times, testability, and maintainability, while keeping feature ownership clear.

At a high level:

* **Infrastructure modules** provide the app entry point, build tooling, and navigation.
* **Core modules** encapsulate reusable domain and service logic (playback, scanning, data, logging, etc.).
* **Feature modules** implement user-facing screens, composed from core logic and UI components.
* **Feature flags** are defined per-feature and implemented via `:core:featureflag`, sourced through remote config.

## Layers

The module inventory lives in `settings.gradle.kts`. This section describes how modules are grouped, not every module.

### Infrastructure

* `:app` – Application entry point, dependency injection graph, and product flavor wiring (`free` / `play`)
* `:navigation` – Navigation abstractions and route definitions
* `plugins/` – Included Gradle build with convention plugins
* `scripts/` – Build and utility scripts

### Core (Shared Logic)

`:core:*` modules hold reusable services and abstractions: data and storage, playback, scanning, search, logging, analytics, remote
config, feature flags, localized strings, and shared Compose UI and theming.

### Features

`:features:*` modules are screen- or flow-based. Each owns its UI (Compose) and presentation logic and delegates to `:core` modules for
data and services.

### API / Implementation Splits

When a contract has several implementations, it is split into an `:api` module plus implementation modules (for example `noop`,
`firebase`, `play`, `free`, or `impl`). Consumers depend only on `:api`; `:app` picks the implementation, often per product flavor.

## Dependency Flow

* Features depend **only on `:core` and infrastructure abstractions**.
* `:core` modules depend on each other as needed, but never on features.
* Infrastructure modules (`:app`, `:navigation`) wire everything together at runtime.

This ensures **unidirectional dependency flow**:

```
Infrastructure → Core → Features
```

## Diagram

````mermaid
flowchart LR
    subgraph Infrastructure
        app(":app")
        navigation(":navigation")
    end

    subgraph Core
        core(":core")
    end

    subgraph Features
        features(":features")
    end

    app --> navigation
    app --> features
    features --> core
    features --> navigation
````

## Tech Decisions

* **Compose (UI)** – Declarative UI with Material 3 for consistency and accessibility
* **Metro (DI)** – Lightweight dependency injection across modules
* **Navigation3** – Type-safe, modular navigation
* **ExoPlayer (Media3)** – Robust audio playback engine
* **Room** – Persistent storage
* **Kotlin Serialization** – JSON parsing and object serialization
* **Coil** – Efficient image loading

## Module Lifecycle

1. **Add new functionality as a feature module.**
2. **Extract reusable logic into `:core` modules** once multiple features need it.
3. **Keep infrastructure minimal** — mainly for wiring and build configuration.
