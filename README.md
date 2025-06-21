Voice
![CI](https://github.com/PaulWoitaschek/Voice/actions/workflows/voice.yml/badge.svg?branch=main) <a href="https://hosted.weblate.org/engage/voice/">
<img src="https://hosted.weblate.org/widgets/voice/-/svg-badge.svg" alt="Translation status" />
=======================

<a href="https://play.google.com/store/apps/details?id=de.ph1b.audiobook"><img src="https://raw.githubusercontent.com/PaulWoitaschek/Voice/main/app/src/main/play/listings/en-US/graphics/feature-graphic/1.jpg" width="600" ></a>

**Voice** is a minimalistic, user-focused audiobook player built for reliability and simplicity.

<a href="https://f-droid.org/packages/de.ph1b.audiobook/">
  <img alt="Get it on F-Droid"
       height="80"
       src="https://f-droid.org/badge/get-it-on.png" />
</a>
<a href="https://play.google.com/store/apps/details?id=de.ph1b.audiobook">
  <img alt="Get it on Google Play"
       height="80"
       src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" />
</a>

For complete details on features, development, and licensing, please visit our [documentation website](https://voice.woitaschek.de).

## Development Setup

### GitHub Codespaces

This repository is configured for development using GitHub Codespaces. A `.devcontainer/devcontainer.json` file is included, which defines a development environment with all necessary dependencies for Android development, including:

*   Java Development Kit (JDK)
*   Android SDK and build tools
*   Gradle
*   Commonly used VS Code extensions for Kotlin and Android development

**How to use:**

1.  Navigate to the main page of the repository on GitHub.
2.  Click the "**<> Code**" button.
3.  Go to the "**Codespaces**" tab.
4.  Click "**Create codespace on main**" (or your current branch).

GitHub will set up a new Codespace based on the configuration. Once the Codespace is ready, it will open in a browser-based VS Code editor, or you can connect to it from your local VS Code instance.

The Android SDK path and `local.properties` file should be automatically configured by the `postCreateCommand` in the `devcontainer.json`.

You can then use the integrated terminal in VS Code to run Gradle commands as outlined in the [Developer Guidelines](.junie/guidelines.md), such as:

*   Build the app: `./gradlew :app:assemblePlayProprietaryDebug`
*   Run unit tests: `./gradlew voiceUnitTest`
*   Lint and format: `./gradlew lintKotlin` / `./gradlew formatKotlin`

### Local Setup

For local development, ensure you have a compatible Java Development Kit (JDK) and the Android SDK installed. You will also need to create a `local.properties` file in the root of the project, pointing to your Android SDK installation, e.g.:

```
sdk.dir=/path/to/your/android/sdk
```

Refer to the [Developer Guidelines](.junie/guidelines.md) for more details on build variants and project structure.

## License

This project is licensed under [GNU GPLv3](docs/license). By contributing, you agree to license your code under the same terms.
