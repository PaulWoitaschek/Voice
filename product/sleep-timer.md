# Sleep timer

**Status:** Outline.

Listeners fall asleep to a book, and the next night they pick up where they left off without searching for their place.

**When:** In bed with the lights off, drowsy, often with headphones and next to a partner. They don't want to unlock
their phone, or touch it at all. Above everything else, the timer has to stop ([#2175]).

| Problem | Status |
|---|---|
| [The book keeps playing after I fall asleep](#the-book-keeps-playing-after-i-fall-asleep) | ✅ Solved |
| [With short chapters, end of chapter stops too soon](#with-short-chapters-end-of-chapter-stops-too-soon) | ⭕ Open |
| [I forget to set the timer](#i-forget-to-set-the-timer) | ✅ Solved |
| [The stop wakes me up](#the-stop-wakes-me-up) | ✅ Solved |
| [The timer stops while I'm still awake](#the-timer-stops-while-im-still-awake) | ✅ Solved |
| [After the shake window, more time means setting the timer again](#after-the-shake-window-more-time-means-setting-the-timer-again) | ⭕ Open |
| [I don't know where I fell asleep](#i-dont-know-where-i-fell-asleep) | ✅ Solved |
| [The sleep timer fills up my bookmarks](#the-sleep-timer-fills-up-my-bookmarks) | ✅ Solved |
| [Setting the timer means opening the app](#setting-the-timer-means-opening-the-app) | ⭕ Open |

## The book keeps playing after I fall asleep

✅ **Solved** · [#2788]

Listeners wake up in the middle of the night with the book still playing, and no idea where they were.

**Solution:**

- A timer from the moon button: 5, 15, 30 or 60 minutes, or an exact time in 1-minute steps. It counts listening time,
  so pausing doesn't use it up.
- End of chapter. It was turned down once because the sleep menu was full enough ([#464]), and then shipped only with
  tests ([#2668]).

## With short chapters, end of chapter stops too soon

⭕ **Open** · [#1421]

When chapters are only a few minutes long, end of chapter stops before the listener is asleep.

## I forget to set the timer

✅ **Solved** · [#988]

Listeners forget to start the timer and wake up to a finished book.

**Solution:** The automatic sleep timer starts a timer when playback begins between two times, 22:00 to 06:00 by
default.

**Turned down:**

- No automatic timer, to save a setting, because pressing play and then the moon is easy enough ([#414]). This was
  reversed later.

## The stop wakes me up

✅ **Solved** · [#393]

When the sound cuts off, some listeners wake up.

**Solution:** The last 10 seconds fade out, then Voice rewinds by the length of the fade.

## The timer stops while I'm still awake

✅ **Solved** · [#742]

Getting more time shouldn't mean turning around in bed, waking a partner, unlocking the phone and opening the app.

**Solution:**

- Shake within 30 seconds of the end to restart with the same timer. It's always on, with no setting ([#981]).
- The automatic timer starts again when you press play inside its time window.

**Turned down:**

- Restarting the timer on every play, at any time of day: too specific to give everyone ([#535]).

**Assumption:** Listeners can shake in bed. Evidence against: some lie with their hands under the covers ([#3278]).

## After the shake window, more time means setting the timer again

⭕ **Open** · [#2233]

Once the 30 seconds have passed, the only way to more time is the moon button.

## I don't know where I fell asleep

✅ **Solved** · [#1026]

The next morning, listeners don't know how far back to go.

**Solution:** The night is a stretch in the listening history, marked "Dozed off · timer ended". Tapping it goes to the
last spot the listener played from or moved to, where they were surely still awake.

## The sleep timer fills up my bookmarks

✅ **Solved** · [#3288]

A timer that adds a "Dozed off" bookmark every night, at a spot where the listener was still awake, piles them up
between the bookmarks the listener saved and on the player's progress bar.

**Solution:** The sleep timer sets no bookmarks. The night is in the listening history instead (see
[Bookmarks and history](bookmarks.md)). The "Dozed off" bookmarks from before moved there too. One the listener wrote a
note on stays, as a plain bookmark.

**Turned down:**

- A setting to turn the bookmark off ([#3288]): it isn't the listener's bookmark in the first place, and a good default
  beats a setting.
- Keeping only the newest "Dozed off" bookmark: it still mixes what Voice did with what the listener saved.
- Doing nothing: one more bookmark every night.

## Setting the timer means opening the app

⭕ **Open**

Listeners want to set it from a widget, Tasker or the lock screen.

**Turned down:**

- A button in the notification: removed ([#2738]). No reason was written down.
- A sleep timer widget: dropped before it shipped ([#3839]). No reason was written down.

## Open questions

1. End of chapter has no fade-out and no shake to restart. Is that on purpose?
2. The exact time in the sheet also sets how long the automatic timer runs, and Settings doesn't show that. Is that
   okay?
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
[#2175]: https://github.com/PaulWoitaschek/Voice/issues/2175
[#2233]: https://github.com/PaulWoitaschek/Voice/discussions/2233
[#2668]: https://github.com/PaulWoitaschek/Voice/pull/2668
[#2738]: https://github.com/PaulWoitaschek/Voice/pull/2738
[#2788]: https://github.com/PaulWoitaschek/Voice/issues/2788
[#3278]: https://github.com/PaulWoitaschek/Voice/discussions/3278
[#3288]: https://github.com/PaulWoitaschek/Voice/discussions/3288
[#3839]: https://github.com/PaulWoitaschek/Voice/pull/3839
