# Product specs

Listeners press play on their own audiobooks, stay in the story, and never lose their place.

Most of them drop in their audiobook files and just want them to play ([#3162]). Power users matter too, as long as the
basics stay simple ([#1034]).

## Principles

- **Clean, not minimal.** A feature has to make listening better for many listeners, because each one is maintained for
  years ([FAQ](../docs/faq.md)).
- **Good defaults before settings.** Every setting makes Voice a bit harder to use ([#860]).
- **The data stays with the listener.** No account, no ads, nothing behind a paywall, and no Voice server.
- **Never interrupt listening.** Prompts wait for a calm moment, ask once, and accept no.
- **Reliable before new.** A feature that breaks listening waits until it has tests.

Decisions follow upvotes on ideas ([#1330]) and opt-in analytics in the Play build ([#3212]). Voice is neither a podcast
or music player ([#970]) nor an app for other platforms than Android ([#83]).

## Areas

| Area | What it covers |
|---|---|
| [Getting started](getting-started.md) | Adding folders and how books are found |
| [Library](library.md) | Finding and managing books |
| [Player](player.md) | Listening: speed, skipping, sound, your place |
| [Sleep timer](sleep-timer.md) | Falling asleep to a book |
| [Bookmarks and history](bookmarks.md) | Saving moments and finding your way back |
| [Outside the app](outside-the-app.md) | Headphones, car, lock screen, widgets |
| [Servers, sync and backup](servers-and-sync.md) | Your own server, more devices, a new phone |
| [Look and feel](look-and-feel.md) | Themes, colors, accessibility, languages |
| [Feedback and support](feedback-and-support.md) | Getting help, privacy, supporting Voice |

## Writing a spec

A spec lists the problems listeners have, under **Solved**, **Planned**, **Open** and **Won't solve**.

- Write the problem the way a listener would say it, and what Voice does about it in a sentence or two.
- Link the one issue or discussion that matters, for open problems and decisions.
- Only add something under **Won't solve** if it would otherwise be proposed again, and give the reason.
- Leave out history, releases, counts, bugs, one-off opinions and how it's built.
- Update the spec in the pull request that changes what it describes.

[#83]: https://github.com/PaulWoitaschek/Voice/issues/83
[#860]: https://github.com/PaulWoitaschek/Voice/issues/860
[#970]: https://github.com/PaulWoitaschek/Voice/issues/970
[#1034]: https://github.com/PaulWoitaschek/Voice/issues/1034
[#1330]: https://github.com/PaulWoitaschek/Voice/discussions/1330
[#3162]: https://github.com/PaulWoitaschek/Voice/discussions/3162
[#3212]: https://github.com/PaulWoitaschek/Voice/pull/3212
