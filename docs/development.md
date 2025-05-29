# Development

## Tests

### Unit tests

To run the unit tests, run the following command:

```sh
./gradlew voiceUnitTests
```

### Instrumentation tests

To run the instrumentation tests, run the following command:

```sh
./gradlew voiceDeviceGithubLibreDebugAndroidTest
```

## Ktlint

Voice uses **Ktlint** to enforce consistent code formatting.

- Check for formatting issues:

```sh
./gradlew lintKotlin
```

- Auto-fix formatting:

```sh
./gradlew formatKotlin
```

- To make commits fail on formatting errors, set up a pre-commit hook:

```sh
echo "./gradlew lintKotlin" > .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

## Releasing

To release a new version:

1. Update versionCode and versionName in libs.versions.toml.
2. Push a corresponding Git tag to the main branch.

This triggers the CI to build and publish the release to the Play Store’s internal track. From there, promotion to production must be done
manually.

F-Droid builds are handled by their team and usually appear a few days after a stable (non-RC) release.
