# Look and feel

**Status:** Outline.

## Outcome

Voice feels calm and personal, day and night, and works for everyone in their own language.

## Who and when

Everyone, but especially at night in bed, and for listeners with large fonts or a screen reader.

The rule: "I don't want voice to be really customizable. I want to provide good defaults and don't distract the user."
([#795]) The details are in the [design principles].

## Opportunities

### "Let me pick dark or light myself"

"I like to have the system in light mode but certain apps in dark mode." ([#1012])

- ✅ **Light, dark or follow the system.**
- ❌ **Turned down in 2022**: "Voice will follow the system dark / light theme." ([#1012]) This was reversed in 2026.

### "Pure black for bedtime"

"I mostly listen to audiobooks at bedtime and an AMOLED theme would be really helpful." ([#1284])

- ⭕ **Dark mode isn't black enough for bed.** In 2018: "I'm all in for creating an AMOLED theme and using it as the
  night theme on AMOLED devices." ([#795])

### "More color, and colors that match my book"

"change the color of the program's theme (including the background) depending on the cover" ([#2627])

- ✅ **Dynamic color** from the wallpaper (2022).
- ✅ **Six color schemes** to choose from.
- ✅ **The player and book screens take their colors from the cover.** App-wide screens like settings don't, on purpose
  ([design principles]).

### "It should feel modern and calm"

The 2022 redesign got pushback: "The color schemes both for light and dark theme were more beautiful before." ([#1444])

- ✅ **The Material 3 Expressive redesign.**

Assumptions:

- **Listeners will like the redesign.** Ratings and feedback will tell.

### "I can't read it or reach it"

There's little evidence here. "my sight is quite restricted" ([#766]) "with talkback on, there is no way this popup can
be closed" ([#1898])

- ✅ **Bigger skip buttons**, and **layouts that work with large fonts and right-to-left languages**.

### "Voice in my language, translated well"

"an awkward auto-translated mess of Swedish" ([#943], 2020)

- ✅ **Translations with one shared tone**, for the languages Voice ships.
- ❌ **13 languages dropped**: "Their speakers mostly use another language on their phones, or the translations were
  incomplete." ([#3827]) Volunteer translation on Weblate also ended ([#3811]). No reason for that was written down.

Assumptions:

- **AI translations are good enough.** In 2020 the answer to [#943] was "It's not auto translated, it's translated by
  real humans." How is quality checked now?

## Open questions

1. Pure black: an option, or the automatic night look, for example while the sleep timer runs?
2. Accessibility: has anyone tested Voice with TalkBack? Can every popup be closed with it ([#1898])?
3. Which languages to support, and how to check that the translations are good?

[design principles]: ../.agents/skills/design/SKILL.md
[#766]: https://github.com/PaulWoitaschek/Voice/issues/766
[#795]: https://github.com/PaulWoitaschek/Voice/issues/795
[#943]: https://github.com/PaulWoitaschek/Voice/issues/943
[#1012]: https://github.com/PaulWoitaschek/Voice/issues/1012
[#1284]: https://github.com/PaulWoitaschek/Voice/discussions/1284
[#1444]: https://github.com/PaulWoitaschek/Voice/issues/1444
[#1898]: https://github.com/PaulWoitaschek/Voice/discussions/1898
[#2627]: https://github.com/PaulWoitaschek/Voice/discussions/2627
[#3811]: https://github.com/PaulWoitaschek/Voice/pull/3811
[#3827]: https://github.com/PaulWoitaschek/Voice/pull/3827
