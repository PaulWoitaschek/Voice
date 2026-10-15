# Feedback and support

**Status:** Outline.

Listeners who need help get it, listeners who want to give back can, and Voice stays maintained, without any of it
getting in the way of listening.

**When:** A listener who is stuck. A happy listener, right after finishing a book. A listener who cares about privacy.
And the maintainer, who needs to know what listeners want.

| Problem | Status |
|---|---|
| [There's no way to give something back](#theres-no-way-to-give-something-back) | ✅ Solved |
| [I'm stuck and don't know where to get help](#im-stuck-and-dont-know-where-to-get-help) | ✅ Solved |
| [I don't want an audiobook app talking to the internet](#i-dont-want-an-audiobook-app-talking-to-the-internet) | ✅ Solved |
| [I don't know which version to install](#i-dont-know-which-version-to-install) | ✅ Solved |
| [I can't check that an APK was built from the published source](#i-cant-check-that-an-apk-was-built-from-the-published-source) | ⭕ Open |
| [The maintainer doesn't know what listeners want](#the-maintainer-doesnt-know-what-listeners-want) | ✅ Solved |

## There's no way to give something back

✅ **Solved** · [#874]

Happy listeners want to pay for Voice, or thank it in some other way.

**Solution:**

- GitHub Sponsors and Ko-fi in the free build.
- Support inside the Play build: monthly tiers, one-time tips and badges. It's a thank-you and unlocks nothing
  ([#3821]).
- A rating prompt after a win: a finished book or a listening milestone. It only appears after 3 days, 5 hours of
  listening and 3 listening days, at most 3 times, and never during playback. Everyone gets the same three choices, so
  there's no gating ([#3820]).

**Turned down:**

- No way to pay, because a thanks is enough ([#874]). That was in 2020, and was reversed later.
- Anything behind a paywall: the [design principles] rule it out.

## I'm stuck and don't know where to get help

✅ **Solved**

A listener with a problem or a question needs somewhere to turn.

**Solution:** Help and feedback in Settings: the FAQ, getting help, reporting a problem and suggesting an idea. The
rating prompt offers the same choices, plus email.

**Assumption:** Listeners can use GitHub. Evidence against: the links can ask you to log in ([#1711]).

## I don't want an audiobook app talking to the internet

✅ **Solved** · [#1232]

Listeners who care about privacy would rather do without cover images than have the app go online.

**Solution:**

- A free build without Google libraries, on F-Droid, for listeners who don't want them ([#944]).
- Analytics only with consent, asked during onboarding and changeable in Settings.
- A [privacy policy](../PRIVACY.md) that lists everything Voice sends, for each build.

## I don't know which version to install

✅ **Solved** · [#2610]

Voice comes from several sources, and they differ. Some listeners trust an APK from GitHub more than the Play Store.
The F-Droid build can't offer Android Auto, and F-Droid, not the maintainer, decides when it updates ([#526]).

**Solution:**

- A page comparing the [download sources](../docs/download-sources.md).
- GitHub releases carry the free build ([#2300]).

## I can't check that an APK was built from the published source

⭕ **Open** · [#2862]

Listeners who install an APK have no way to check that it matches the source code that's published.

## The maintainer doesn't know what listeners want

✅ **Solved** · [#1330]

To decide what to build, the maintainer needs to know what listeners want and how many would benefit.

**Solution:**

- Upvotes on ideas. They help prioritize, and show how many listeners would benefit from a feature ([#1330]).
- Opt-in analytics. Decisions need data: a feature that turns out to be a rare edge case is a candidate for
  removal, to reduce complexity ([#3212]).

**Turned down:**

- A beta channel: Voice doesn't have enough users for beta releases to give enough data ([#959]).

## Open questions

1. Crash reporting was removed in 2024 ([#2439]) and added back in 2025. Why?
2. Is email enough as a way to get help without a GitHub account?
3. The [design principles] say prompts "ask once", but the rating prompt can ask up to 3 times. Which one is right?

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
[#3820]: https://github.com/PaulWoitaschek/Voice/pull/3820
[#3821]: https://github.com/PaulWoitaschek/Voice/pull/3821
