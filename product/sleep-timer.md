# Sleep timer

**Status:** Outline.

## Outcome

Listeners fall asleep to a book, and the next night they pick up where they left off without searching for their place.

## Who and when

In bed with the lights off, drowsy, often with headphones and next to a partner. They don't want to unlock their phone,
or touch it at all. Above everything else, the timer has to stop: "woke up several minutes to hours later and my
audiobook was still playing" ([#2175])

## Opportunities

### "It keeps playing after I fall asleep"

"wake up at 3am with an audiobook still blaring, with no hope of figuring out where I was up to" ([#2788])

- ✅ **A timer from the moon button**: 5, 15, 30 or 60 minutes, or an exact time in 1-minute steps. It counts listening
  time, so pausing doesn't use it up.
- ✅ **End of chapter** ([#2838], 2025). It was turned down in 2016 ("The sleep menu is full enough", [#464]), then
  reverted once because of bugs ([#2264]). It shipped only with tests: "I don't want to merge this PR without a
  test-suite covering it." ([#2668])
- 💡 **More than one chapter**, for books with very short chapters ([#1421]).

### "I forget to set it"

"I'm constantly forgetting to start the timer and waking up to a finished book." ([#988])

- ✅ **Automatic sleep timer** ([#2673], 2025): starts a timer when playback begins between two times, 22:00 to 06:00
  by default.
- ❌ **Turned down before that**: "I think it is not too complicated to just press play and then sleep. That way we can
  get rid of one setting." ([#414]) This was reversed in 2025.

### "The stop wakes me up"

"I always wake up when the sound is cut" ([#393])

- ✅ **The last 10 seconds fade out, then Voice rewinds by the fade**: "Always fading is good. And best, rewind the time
  when the playback stops by the fade amount." ([#393])

### "I'm still awake when it stops"

"I don't want to turn around in bed, wake up my better half, unlock my phone, open the app, press play, click timer…"
([#742])

- ✅ **Shake within 30 seconds of the end** to restart with the same timer. It's always on, with no setting ([#981]).
- ✅ **The automatic timer starts again** when you press play inside its time window ([#3216]).
- ❌ **Restarting the timer on every play, at any time of day**: "I think this request is too specific that everyone
  should have it." ([#535])
- 💡 **More time from a headphone button** ([#2233]).

Assumptions:

- **Listeners can shake in bed.** Evidence against: "Lying in bed, hands under the covers, I can't do it…" ([#3278])

### "I don't know where I fell asleep"

"the next morning I don't know how far back to seek" ([#1026])

- ✅ **A bookmark where the timer starts.** In the next release it's called "Dozed off" and has its own look.
- ✅ **"Dozed off · timer ended" in the listening history**, with a way to jump back there. *(next release)*
- 💡 **Making the bookmark optional** ([#3288]).

### "I want to set it without opening the app"

From a widget ([#1473]), Tasker ([#3410]) or the lock screen ([#2983]).

- ❌ **A button in the notification**: removed in 2025 ([#2738]). No reason was written down.
- ❌ **A sleep timer widget**: dropped before it shipped ([#3839]). No reason was written down.

## Open questions

1. End of chapter has no fade-out, no shake to restart and no bookmark. Is that on purpose?
2. The exact time in the sheet also sets how long the automatic timer runs, and Settings doesn't show that. Is that okay?
3. One tap on the moon cancels a running timer. Should it open the sheet instead, with "Turn off" as one option?
4. Why were the notification button and the widget dropped?

[#393]: https://github.com/PaulWoitaschek/Voice/issues/393
[#414]: https://github.com/PaulWoitaschek/Voice/issues/414
[#464]: https://github.com/PaulWoitaschek/Voice/issues/464
[#535]: https://github.com/PaulWoitaschek/Voice/issues/535
[#742]: https://github.com/PaulWoitaschek/Voice/issues/742
[#981]: https://github.com/PaulWoitaschek/Voice/issues/981
[#988]: https://github.com/PaulWoitaschek/Voice/issues/988
[#1026]: https://github.com/PaulWoitaschek/Voice/issues/1026
[#1421]: https://github.com/PaulWoitaschek/Voice/discussions/1421
[#1473]: https://github.com/PaulWoitaschek/Voice/discussions/1473
[#2175]: https://github.com/PaulWoitaschek/Voice/issues/2175
[#2233]: https://github.com/PaulWoitaschek/Voice/discussions/2233
[#2264]: https://github.com/PaulWoitaschek/Voice/pull/2264
[#2668]: https://github.com/PaulWoitaschek/Voice/pull/2668
[#2673]: https://github.com/PaulWoitaschek/Voice/pull/2673
[#2738]: https://github.com/PaulWoitaschek/Voice/pull/2738
[#2788]: https://github.com/PaulWoitaschek/Voice/issues/2788
[#2838]: https://github.com/PaulWoitaschek/Voice/pull/2838
[#2983]: https://github.com/PaulWoitaschek/Voice/discussions/2983
[#3216]: https://github.com/PaulWoitaschek/Voice/discussions/3216
[#3278]: https://github.com/PaulWoitaschek/Voice/discussions/3278
[#3288]: https://github.com/PaulWoitaschek/Voice/discussions/3288
[#3410]: https://github.com/PaulWoitaschek/Voice/discussions/3410
[#3839]: https://github.com/PaulWoitaschek/Voice/pull/3839
