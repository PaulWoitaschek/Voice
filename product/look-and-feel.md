# Look and feel

**Status:** Outline.

Voice feels calm and personal, day and night, and works for everyone in their own language.

**When:** Everyone, but especially at night in bed, and for listeners with large fonts or a screen reader.

The rule: Voice isn't meant to be very customizable. It offers good defaults and doesn't distract the listener
([#795]). The details are in the [design principles].

| Problem | Status |
|---|---|
| [I can't pick dark or light for Voice alone](#i-cant-pick-dark-or-light-for-voice-alone) | ✅ Solved |
| [Dark mode isn't black enough for bed](#dark-mode-isnt-black-enough-for-bed) | ✅ Solved |
| [The colors don't match my phone, my taste or my book](#the-colors-dont-match-my-phone-my-taste-or-my-book) | ✅ Solved |
| [Voice doesn't feel modern and calm](#voice-doesnt-feel-modern-and-calm) | ✅ Solved |
| [I can't read or use Voice with poor sight](#i-cant-read-or-use-voice-with-poor-sight) | ✅ Solved |
| [In split screen, the play controls go off screen](#in-split-screen-the-play-controls-go-off-screen) | ✅ Solved |
| [The translation reads awkwardly](#the-translation-reads-awkwardly) | ✅ Solved |
| [Voice isn't in my language](#voice-isnt-in-my-language) | ✅ Solved |

## I can't pick dark or light for Voice alone

✅ **Solved** · [#1012]

Some listeners keep their phone light and want certain apps dark.

**Solution:** Light, dark or follow the system.

**Turned down:**

- Always following the system's theme, decided in 2022 ([#1012]). This was reversed in 2026.

## Dark mode isn't black enough for bed

✅ **Solved** · [#1284]

Listeners who listen at bedtime want a pure black theme for their AMOLED screens. In 2018, the maintainer was all for
an AMOLED theme, used as the night theme on AMOLED devices ([#795]).

**Solution:** A pure black switch, off by default, turns the dark theme's background black, also when following the
system. Cards, sheets and bars keep their tones so they still stand out.

## The colors don't match my phone, my taste or my book

✅ **Solved** · [#2627]

Listeners want more color. Some want the whole app, background included, to take its colors from the book's cover.

**Solution:**

- Dynamic color from the wallpaper, since 2022.
- Six color schemes to choose from.
- The player and book screens take their colors from the cover. App-wide screens like settings don't, on purpose
  ([design principles]).

## Voice doesn't feel modern and calm

✅ **Solved** · [#1444]

The 2022 redesign got pushback: some listeners found the old light and dark color schemes more beautiful.

**Solution:** The Material 3 Expressive redesign.

**Assumption:** Listeners will like the redesign. Ratings and feedback will tell.

## I can't read or use Voice with poor sight

✅ **Solved** · [#766]

There's little evidence here. One listener's sight is quite restricted. With TalkBack on, another couldn't close a
popup ([#1898]).

**Solution:** Bigger skip buttons, and layouts that work with large fonts and right-to-left languages.

## In split screen, the play controls go off screen

✅ **Solved** · [#3637]

Next to a navigation app in split screen, listeners want the play controls on screen all the time.

**Solution:** The play controls stay on screen in any window size, down to a short split screen pane. The cover gives
way first, then the other controls, one at a time.

## The translation reads awkwardly

✅ **Solved** · [#943]

In 2020, a listener found the Swedish translation awkward and thought a machine had made it.

**Solution:** Translations with one shared tone, for the languages Voice ships.

**Turned down:**

- Volunteer translation on Weblate: ended ([#3811]). No reason was written down.

**Assumption:** AI translations are good enough. In 2020, the answer to [#943] was that real people translated Voice,
not a machine. How is quality checked now?

## Voice isn't in my language

✅ **Solved** · [#3931]

Voice dropped 13 languages ([#3827]), and a listener who used it in Sardinian asked for it back.

**Solution:** Since agents translate every string, another language costs little. Voice ships the languages people
run their phones in, and adds a language when a listener asks for it.

**Turned down:**

- Only the languages whose speakers mostly use them on their phones ([#3827]): it left out listeners who picked their
  language on purpose.
- Weblate for the languages AI translates less well ([#3931]): a second place where translations change would
  conflict with translating in each pull request.

## Open questions

1. Accessibility: has anyone tested Voice with TalkBack? Can every popup be closed with it ([#1898])?
2. How to check that the translations are good?

[design principles]: ../.agents/skills/design/SKILL.md
[#766]: https://github.com/PaulWoitaschek/Voice/issues/766
[#795]: https://github.com/PaulWoitaschek/Voice/issues/795
[#943]: https://github.com/PaulWoitaschek/Voice/issues/943
[#1012]: https://github.com/PaulWoitaschek/Voice/issues/1012
[#1284]: https://github.com/PaulWoitaschek/Voice/discussions/1284
[#1444]: https://github.com/PaulWoitaschek/Voice/issues/1444
[#1898]: https://github.com/PaulWoitaschek/Voice/discussions/1898
[#2627]: https://github.com/PaulWoitaschek/Voice/discussions/2627
[#3637]: https://github.com/PaulWoitaschek/Voice/issues/3637
[#3811]: https://github.com/PaulWoitaschek/Voice/pull/3811
[#3827]: https://github.com/PaulWoitaschek/Voice/pull/3827
[#3931]: https://github.com/PaulWoitaschek/Voice/discussions/3931
