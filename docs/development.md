# Development

## Project Setup

To run the project, open it in the latest Version of Android Studio and build the project as usual.

By default, there is not enough memory configured for gradle. You can fix this by running.

```sh
scripts/gradle_bootstrap.sh
```

This will configure your global `~/.gradle/gradle.properties` to use more memory, depending on your machine.

Example of the generated properties, check the `gradle_bootstrap.sh` script for exact details.

```properties
# Begin: Gradle JVM bootstrap-generated properties
org.gradle.jvmargs=-Dfile.encoding=UTF-8 -XX:+ExitOnOutOfMemoryError -Xms4g -Xmx16g
kotlin.daemon.jvm.options=-Dfile.encoding=UTF-8 -XX:+ExitOnOutOfMemoryError -Xms4g -Xmx16g
# End: Gradle JVM bootstrap-generated properties
```

## Tests

### Unit tests

To run the unit tests, run the following command:

```sh
./gradlew voiceUnitTest
```

### Instrumentation tests

To run the instrumentation tests, run the following command:

```sh
./gradlew voiceDeviceGithubDebugAndroidTest
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

## Pages Deployment

To projects [Website](https://voice.woitaschek.de/) uses Github Pages and Mkdocs.
To deploy a new website, use dispatch a workflow
manually.

[👉 Dispatch Workflow](https://github.com/PaulWoitaschek/Voice/actions/workflows/deploy_pages.yml)
