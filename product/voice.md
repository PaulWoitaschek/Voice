# Voice

**Status:** Outline. This is the overview for the whole app. Each area has its own spec, linked below.

Listeners press play on their own audiobooks, stay in the story, and never lose their place.

**Who:** Mostly people who drop in their audiobook files and just want them to play ([#3044]). Power users matter too,
as long as the basics stay simple and their features don't get in the way ([#1034]).

## Areas

| Area | The problems in it |
|---|---|
| [Getting started](getting-started.md) | Picking the folder layout, wrong names, missing chapters, losing everything after a move |
| [Library](library.md) | Sorting, series, progress, editing details, stats |
| [Player](player.md) | A position you can trust, speed for every book, time left, seeking in long books |
| [Sleep timer](sleep-timer.md) | Stopping reliably, end of chapter, more time without unlocking, where you fell asleep |
| [Bookmarks and history](bookmarks.md) | Saving without stopping, finding bookmarks, undoing a slip, export |
| [Outside the app](outside-the-app.md) | Car, headphone buttons, the lock screen slider, widgets, casting |
| [Servers, sync and backup](servers-and-sync.md) | Your own server, cloud storage, two devices, a new phone |
| [Look and feel](look-and-feel.md) | Dark mode, pure black, color, accessibility, languages |
| [Feedback and support](feedback-and-support.md) | Supporting Voice, getting help, privacy, which build to install |

Each spec starts with a table of its problems and where each one stands: ✅ solved, 🔜 planned, ⭕ open or ❌ won't
solve.

## Principles

These limit every solution in every area.

- **Clean, not minimal.** A feature has to make listening better for many listeners. Each one is maintained for years,
  and every new setting makes the app a bit harder to use ([FAQ](../docs/faq.md)).
- **Good defaults before settings.** Voice isn't meant to be highly customizable. Good defaults and a simple UI matter
  more than the cost of maintaining a setting ([#860]).
- **The data stays with the listener.** No account, no ads, nothing behind a paywall, and no Voice server
  ([design principles]).
- **Never interrupt listening.** Prompts wait for a calm moment, ask once, and accept no ([design principles]).
- **Reliable before new.** The goal is an app that's both reliable and a joy to use ([About](../docs/about.md)). End of
  chapter was reverted until it had tests ([Sleep timer](sleep-timer.md)).
- **Product before code.** A solid product concept comes before the technical solution ([#3044]).

## What Voice isn't

- **A podcast or music player.** It's an audiobook player ([#970]). Podcasts are better served by dedicated apps like
  AntennaPod ([#648]).
- **An app for other platforms.** The focus is the Android app ([#83]).
- **A store or a way to find books.** Requests exist, for LibriVox for example ([#1296]), but no decision is on record.
- **Undecided: reading along**, with ebooks ([#1635]) or subtitles ([#1671]). Both are open, with no position from the
  maintainer.

## How decisions get made

- **Demand**: upvotes on ideas ([#1330]).
- **Data**: opt-in analytics in the Play build ([#3212]).
- **Tests before release**: tester builds, feature flags and previews, like Audiobookshelf ([#3863]).
- **Who builds**: Voice only takes pull requests the maintainer asked for, because reviewing outside code takes more
  time than there is ([CONTRIBUTING.md](../CONTRIBUTING.md)).

## Open questions for the whole app

1. **What does "many listeners" mean in numbers?** There's no threshold. Analytics is the only measure mentioned.
2. **A book identity that survives moves.** One root problem behind renaming, backup and syncing without a server. See
   [Servers, sync and backup](servers-and-sync.md).
3. **Reading along**: in or out?

[design principles]: ../.agents/skills/design/SKILL.md
[#83]: https://github.com/PaulWoitaschek/Voice/issues/83
[#648]: https://github.com/PaulWoitaschek/Voice/issues/648
[#860]: https://github.com/PaulWoitaschek/Voice/issues/860
[#970]: https://github.com/PaulWoitaschek/Voice/issues/970
[#1034]: https://github.com/PaulWoitaschek/Voice/issues/1034
[#1296]: https://github.com/PaulWoitaschek/Voice/discussions/1296
[#1330]: https://github.com/PaulWoitaschek/Voice/discussions/1330
[#1635]: https://github.com/PaulWoitaschek/Voice/discussions/1635
[#1671]: https://github.com/PaulWoitaschek/Voice/discussions/1671
[#3044]: https://github.com/PaulWoitaschek/Voice/issues/3044
[#3212]: https://github.com/PaulWoitaschek/Voice/pull/3212
[#3863]: https://github.com/PaulWoitaschek/Voice/discussions/3863
