# Feedback and support

**Status:** Outline.

## Outcome

Listeners who need help get it, listeners who want to give back can, and Voice stays maintained. None of it gets in the
way of listening.

## Who and when

- A listener who is stuck.
- A happy listener, right after finishing a book.
- A listener who cares about privacy.
- The maintainer, who needs to know what listeners want.

## Opportunities

### "I love it and want to give something back"

"As this is the best audio-book player, would be happy to pay for it" ([#874]). The answer in 2020: "I don't need
donations, a thanks is enough!" ([#874])

- ✅ **GitHub Sponsors** ([#3525]) and **Ko-fi** in the free build ([#3597]), both in 2026.
- ✅ **Support inside the Play build**: monthly tiers, one-time tips and badges ([#3821]). "It's a thank-you: it
  unlocks nothing." *(next release, behind a flag)*
- ✅ **A rating prompt after a win**: a finished book or a listening milestone. It only appears after 3 days, 5 hours
  of listening and 3 listening days, at most 3 times, and "It's never shown during playback". "Everyone gets the same
  three, so there's no gating" ([#3820]). *(next release, behind a flag)*
- ❌ **Anything behind a paywall** ([design principles]).

### "Something's wrong and I need help"

- ✅ **Help and feedback in Settings**: the FAQ, getting help, reporting a problem and suggesting an idea. In the
  next release, the rating prompt offers the same choices, plus email.

Assumptions:

- **Listeners can use GitHub.** Evidence against: the links can ask you to log in ([#1711]).

### "Why does an audiobook app talk to the internet?"

"if only I could go without cover images and not have an app talking to the internet" ([#1232])

- ✅ **A free build without Google libraries**, on F-Droid: "If you don't like that use the fdroid version." ([#944])
- ✅ **Analytics only with consent**, asked during onboarding and changeable in Settings ([#3212], 2025). The reason: "I
  need user data to make decisions." "If for example one is a super edge case, I want to think about removing it to
  reduce complexity." ([#3212])

### "Which version should I install?"

"it's just better to rely on apk from GitHub than installing app from Play store" ([#2610])

- ✅ **A page comparing the download sources** ([#3603], 2026).
- The F-Droid build can't offer Android Auto. F-Droid also updates on its own schedule: "It's not me who's updating it,
  it's them." ([#526])
- 💡 **Reproducible builds** ([#2862]), and **a separate free APK on GitHub** ([#2300]).

### How the maintainer hears from listeners

- **Upvotes on ideas**: "Please upvotes ideas that you like. This helps with prioritizing and determining, how many
  users would benefit from a feature." ([#1330])
- **Opt-in analytics** ([#3212]).
- **Not a beta channel**: "Voice does not have that many users and Beta releases don't work because there is just too
  little data." ([#959])

## Open questions

1. Does [PRIVACY.md](../PRIVACY.md) describe what Voice actually does? It reads like a generic template: it mentions
   location data and marketing, which doesn't match opt-in, anonymous analytics.
2. Crash reporting was removed in 2024 ([#2439]) and added back in 2025. Why?
3. Is email enough as a way to get help without a GitHub account?
4. What decides whether support in the app and the new rating prompt get turned on for everyone?
5. The [design principles] say prompts "ask once", but the rating prompt can ask up to 3 times. Which one is right?

[design principles]: ../.agents/skills/design/SKILL.md
[#526]: https://github.com/PaulWoitaschek/Voice/issues/526
[#874]: https://github.com/PaulWoitaschek/Voice/issues/874
[#944]: https://github.com/PaulWoitaschek/Voice/issues/944
[#959]: https://github.com/PaulWoitaschek/Voice/issues/959
[#1232]: https://github.com/PaulWoitaschek/Voice/issues/1232
[#1330]: https://github.com/PaulWoitaschek/Voice/discussions/1330
[#1711]: https://github.com/PaulWoitaschek/Voice/discussions/1711
[#2300]: https://github.com/PaulWoitaschek/Voice/discussions/2300
[#2439]: https://github.com/PaulWoitaschek/Voice/pull/2439
[#2610]: https://github.com/PaulWoitaschek/Voice/issues/2610
[#2862]: https://github.com/PaulWoitaschek/Voice/discussions/2862
[#3212]: https://github.com/PaulWoitaschek/Voice/pull/3212
[#3525]: https://github.com/PaulWoitaschek/Voice/pull/3525
[#3597]: https://github.com/PaulWoitaschek/Voice/pull/3597
[#3603]: https://github.com/PaulWoitaschek/Voice/pull/3603
[#3820]: https://github.com/PaulWoitaschek/Voice/pull/3820
[#3821]: https://github.com/PaulWoitaschek/Voice/pull/3821
