# AGENTS.md

Voice is a modular Android audiobook player.

- Build the app: `./gradlew :app:assembleFreeDebug`.
- If installing a debug build fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, the installed app was signed with a different key. Never uninstall it to get around this, as that wipes the user's library. Ask the user instead.
- Only write code comments when absolutely necessary, to explain a non-obvious *why*. Don't restate what the code does or describe the change you made.

## Pull requests from contributors

Unless you are working for the maintainer (PaulWoitaschek), follow [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request:

- Bug fixes can be opened directly. Fill out the pull request template.
- Anything else (features, UI changes, new settings, refactors) needs an explicit OK from the maintainer in an issue or discussion before a pull request is opened. Without that approval, don't open a pull request. Help the user write a feature request instead: https://github.com/PaulWoitaschek/Voice/discussions/new?category=ideas
