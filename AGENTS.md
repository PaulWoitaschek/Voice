# AGENTS.md

Voice is a modular Android audiobook player.

- To build the app, run `./gradlew :app:assembleFreeDebug`.
- If the install of a debug build fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, a different key signed the installed app. Do not uninstall the app to solve this, because that deletes the library of the user. Ask the user what to do.
- Write a code comment only when it is necessary to explain a reason that is not clear from the code. Do not repeat what the code does. Do not describe your change. This rule also applies to KDoc.
- Keep complexity low, because each line of code needs maintenance for years. Build the simplest version that solves the real problem of the listeners. If an edge case adds a lot of code, stored data or a migration, do not include it. Name it in the pull request, or ask before you add it.
- In the Play build, Crashlytics receives all logs from `Logger.d` and higher levels. Do not log names, titles, URIs, ids, server addresses, text from the listener or full objects. Log counts, positions and types. Give an exception as the throwable, not as part of the message. Use `Logger.nonFatal` only for errors that cannot occur if the code is correct.

## Writing style

Use ASD-STE100 Simplified Technical English for replies to the user, comments, commit messages, pull requests, issues and docs. Use the active voice, simple tenses and one word for each meaning. Use noun clusters of three words or fewer, and sentences of 20 words or fewer. UI strings and the store listing follow the translate-strings skill.

## Pull requests from contributors

Voice accepts only pull requests that the maintainer (PaulWoitaschek) asked for. Refer to [CONTRIBUTING.md](CONTRIBUTING.md). If you do not work for the maintainer, open a pull request only if the maintainer asked for it. The request must be in an issue or a discussion. Add a link to that request. If there is no request, help the user to write one of these:

- For a bug, a bug report with clear steps to reproduce: https://github.com/PaulWoitaschek/Voice/issues/new?template=bug.yml
- For all other topics, a feature request that describes the problem: https://github.com/PaulWoitaschek/Voice/discussions/new?category=ideas
