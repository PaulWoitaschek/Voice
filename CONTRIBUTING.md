# Contributing

Thanks for wanting to help with Voice! Please read this page before you open a pull request.

## Quick overview

| You want to…                                            | Do this                                                                                          |
|---------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| Report a bug                                            | [Open a bug report](https://github.com/PaulWoitaschek/Voice/issues/new?template=bug.yml)         |
| Suggest a feature or change                             | [Open a feature request](https://github.com/PaulWoitaschek/Voice/discussions/new?category=ideas) |
| Fix a bug                                               | Open a pull request                                                                              |
| Build anything else (features, UI, settings, refactors) | Get approval from the maintainer **first**, then open a pull request                             |

## Bug reports and feature requests

These are among the most useful contributions. A good bug report with clear steps to reproduce saves more time than most pull requests.
Search the existing [issues](https://github.com/PaulWoitaschek/Voice/issues) and [discussions](https://github.com/PaulWoitaschek/Voice/discussions) first and upvote what's already there.

For a feature request, describe the problem you want solved, not only the solution you have in mind.

## Pull requests

### Bug fixes: welcome

You can open a pull request for a bug fix right away.

- Link the bug report, or describe the bug and how to reproduce it.
- Keep the fix focused on the bug. Leave out unrelated refactors, renames and formatting changes.
- Add a test when the bug can be covered by one.

### Everything else: discuss first, then wait for an OK

New features and UI changes are built by the maintainer. Voice is [designed](https://voice.woitaschek.de/about/) to stay clean and focused, and every feature that gets merged has to be maintained for years.
Writing code has become cheap. Reviewing and maintaining it has not.

So for anything that is **not a bug fix** (features, UI and UX changes, new settings, refactors, architecture changes):

1. Open a [feature request](https://github.com/PaulWoitaschek/Voice/discussions/new?category=ideas) or comment on an existing one, and say that you'd like to work on it.
2. Wait until [@PaulWoitaschek](https://github.com/PaulWoitaschek) explicitly agrees that **you** implement it.
3. Then open the pull request and link to that approval.

An existing feature request, upvotes, or a thread without a reply from the maintainer is **not** approval.

Pull requests that aren't bug fixes and don't link to an approval will be closed without review.
This is nothing personal. Please open a feature request instead, that really helps.

#### What is likely to get approved

Technical work that needs real expertise, for example playback and Media3 internals, audio formats and metadata parsing, performance, or device and platform compatibility.
If that's your area, your help is very welcome. Please still ask first.

### Using AI

Using AI tools is fine. You are still the author: understand every change you submit, test it on a device yourself, and be ready to answer review questions.
Pull requests that look like unreviewed AI output will be closed.

### Dependency updates

Dependencies are updated by Renovate. Please don't open pull requests for version bumps.

### Before you open a pull request

- Read [Development](https://voice.woitaschek.de/development/) and [Architecture](https://voice.woitaschek.de/architecture/).
- Run `./gradlew voiceUnitTest lintKotlin` and make sure it passes.
- Fill out the pull request template.
