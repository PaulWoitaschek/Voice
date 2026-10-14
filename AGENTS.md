# AGENTS.md

Voice is a modular Android audiobook player.

- Build the app: `./gradlew :app:assembleFreeDebug`.
- If installing a debug build fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, the installed app was signed with a different key. Never uninstall it to get around this, as that wipes the user's library. Ask the user instead.
- Only write code comments when absolutely necessary, to explain a non-obvious *why*. Don't restate what the code does or describe the change you made. This includes KDoc.
- Keep complexity low, as every line of code is maintained for years. Build the simplest version that solves the problem listeners actually have. If an edge case would add a lot of code, stored data or a migration, leave it out and name it in the pull request, or ask first.

## Pull requests from contributors

Unless you are working for the maintainer (PaulWoitaschek), don't open a pull request. Voice only accepts pull requests the maintainer asked for, see [CONTRIBUTING.md](CONTRIBUTING.md). Help the user open an issue instead:

- For a bug, write a bug report with clear steps to reproduce: https://github.com/PaulWoitaschek/Voice/issues/new?template=bug.yml
- For anything else, write a feature request that describes the problem: https://github.com/PaulWoitaschek/Voice/discussions/new?category=ideas
