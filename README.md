Voice
![CI](https://github.com/PaulWoitaschek/Voice/actions/workflows/voice.yml/badge.svg?branch=main) <a href="https://hosted.weblate.org/engage/voice/">
<img src="https://hosted.weblate.org/widgets/voice/-/svg-badge.svg" alt="Translation status" />
=======================

<a href="https://play.google.com/store/apps/details?id=de.ph1b.audiobook"><img src="https://raw.githubusercontent.com/PaulWoitaschek/Voice/main/app/src/main/play/listings/en-US/graphics/feature-graphic/1.jpg" width="600" ></a>


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

## About

Voice is a simple, user-focused audiobook player where I explore new technologies, design ideas, and coding practices. It’s built to be
intuitive, reliable, and an all-around joy to use.

## Development

### Current Status

**Note:**
I’m currently unable to review or accept pull requests (PRs) due to life and work commitments. The project remains in "soft maintenance"
mode. Bug reports and suggestions are welcome, but PRs may not receive a response for the foreseeable future. Thank you for your
understanding!

### Ktlint

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

### Releasing

To release a new version:

1. Update versionCode and versionName in libs.versions.toml.
2. Push a corresponding Git tag to the main branch.

This triggers the CI to build and publish the release to the Play Store’s internal track. From there, promotion to production must be done
manually.

F-Droid builds are handled by their team and usually appear a few days after a stable (non-RC) release.

## License

This project is licensed under [GNU GPLv3](LICENSE.md). By contributing, you agree to license your code under the same terms.
