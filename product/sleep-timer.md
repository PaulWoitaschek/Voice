# Sleep timer

**Status:** Draft. Written in October 2026 from the code and from GitHub issues, discussions and pull requests. It
needs a maintainer to check the **Guess** lines and the open questions.

## Outcome

Listeners fall asleep to a book, and the next night they pick up where they left off without searching for their place.

## Who and when

People who listen in bed with the lights off, often with headphones, often next to a partner. They're drowsy, they
can't see well, and they don't want to unlock their phone, or touch it at all.

Above everything else, the timer has to stop. The angriest sleep reports are about one that didn't ([#991], [#2175],
[#3110]): "woke up several minutes to hours later and my audiobook was still playing" ([#2175]). The listener is asleep,
so they can't notice a problem or fix it.

## Opportunities

### "It keeps playing after I fall asleep"

"wake up at 3am with an audiobook still blaring, with no hope of figuring out where I was up to" ([#2788])

- ✅ **A timer from the moon button in the player**: quick choices of 5, 15, 30 and 60 minutes, or an exact time with
  − and + in 1-minute steps (hold to go faster). Voice remembers the last exact time.
- ✅ **It counts listening time, not clock time**: it waits while the book is paused. **Guess:** so that pausing
  doesn't use up your sleep time.
- ✅ **You can see it's running**: the moon shows the time left. In the next release, the player also gets a starry
  night look, and the Now playing widget shows "Stops at 23:42".
- ❌ **One fixed duration from settings**: this was how it worked until 2016. "I believe most people are fine with a
  fixed amount of time they need to find to sleep" ([#372]). Months later the time could be chosen each time ([#474]).
  Listeners kept asking for exact times: "I usually have specific timers like 7 or 12 or 17 minutes" ([#2281]). Rather
  than adding a new input, "In order to keep the UI simple I kept it with the regular +- buttons but allowed a
  long-press" ([#2934]).

#### "I forget to set it"

"I'm constantly forgetting to start the timer and waking up to a finished book." ([#988])

- ✅ **Automatic sleep timer** (2025, [#2673]): when playback starts between two times (22:00 to 06:00 by default) and
  no timer is running, a timer starts with the remembered exact time. It lives in Settings because "it's clearer and
  more consistent that this is a global setting, affecting all books." It works however playback starts ([#2842]).
- ❌ **Turned down twice before that** as too much configuration: "Thats to much of a configuration hastle" ([#101]),
  and "I think it is not too complicated to just press play and then sleep. That way we can get rid of one setting."
  ([#414]) A listener answered: "It is not possible to press sleep if you are controlling the playback via Bluetooth."
  ([#414]) The 2025 version reversed this.

#### "It stops in the middle of a scene"

"if the timer cuts off just by a half a minute too soon, then I have to turn over and fiddle with my phone" ([#426]).
Working out how long a chapter has left "is not always easy if I'm sleepy" ([#2177]). This is the most requested sleep
timer feature: [#1323] and [#2177] have the most upvotes of any sleep thread.

- ✅ **End of chapter** (2025, [#2838]): stops when the current chapter ends, including chapters inside a single-file
  book. It was turned down in 2016 ("The sleep menu is full enough", [#464]), built in 2023, and reverted in 2024 because
  of "various bugs around different file structures" ([#2264]). The next attempt had to come with tests: "I don't want
  to merge this PR without a test-suite covering it." ([#2668])
- 💡 **More than one chapter**, for books with very short chapters ([#1421], [#3485], [#3675]).

Assumptions:

- **A sleep feature is only worth shipping if it can't fail silently.** The end of chapter history supports this.

### "The stop wakes me up"

"I always wake up when the sound is cut" ([#393])

- ✅ **The last 10 seconds fade out, then Voice rewinds by the fade** plus the usual auto rewind: "Always fading is
  good. And best, rewind the time when the playback stops by the fade amount." ([#393]) The rewind answers the other
  complaint: "The fadeout causes me to miss some parts" ([#1375]).

### "I'm still awake when it stops"

Getting more story shouldn't take a whole routine: "I don't want to turn around in bed, wake up my better half, unlock
my phone, open the app, press play, click timer…" ([#742])

- ✅ **Shake within 30 seconds of the end** to restart playback with the same timer. It's always on: "The feature is
  now always active without a toggle." ([#981]) The sheet shows a hint about it. *(next release)*
- ✅ **The automatic timer starts again** when you press play inside its time window, including from headphones
  ([#3216]).
- ❌ **A long shake window.** It was 5 minutes in 2016. It got shorter after reports that shake detection "continues to
  run permanently" and books restarted by themselves ([#936], [#954], [#974]). **Guess:** 30 seconds is long enough if
  you're still awake, and short enough that turning over doesn't restart the book.
- ❌ **Restarting the timer on every play, at any time of day**: "I think this request is too specific that everyone
  should have it." ([#535])
- ❌ **Voice commands**: "a little bit of an overkill" ([#188])
- 💡 **More time from a headphone or Bluetooth button** ([#2233], [#3382]). Outside the automatic timer's window,
  pressing play on headphones "causes the app to resume playback indefinitely" ([#3382]).
- 💡 **Shaking during the countdown to add time** ([#2943]).

Assumptions:

- **Listeners know about shaking.** Evidence against: several threads ask whether it was removed ([#981], [#1533],
  [#2774]). The hint in the sheet, coming in the next release, is meant to fix
  this.
- **Listeners can shake in bed.** Evidence against: "Lying in bed, hands under the covers, I can't do it… By the time I
  figure Voice stopped, it's too late to shake" ([#3278]).

### "I don't know where I fell asleep"

"the next morning I don't know how far back to seek" ([#1026]). "I fall asleep, but I don't know when." ([#1853])

- ✅ **A bookmark** where a timed timer or the automatic timer starts. In the next release it's called "Dozed off"
  and has its own moon look, so it stays apart from your own bookmarks. **Guess:** it marks the start because that's the last moment you surely heard.
- ✅ **"Dozed off · timer ended" in the listening history**, at the spot where it stopped. You can jump back from there.
  *(next release)*
- 💡 **A bookmark on every restart** ([#1339]).
- 💡 **Making the bookmark optional** ([#3288]). This goes against keeping it always on with no switches, which the
  bookmark has done since 2020.

### "I want to set it without opening the app"

From a widget ([#1473], open since 2020), Tasker ([#3410]) or the lock screen ([#2983]).

- ❌ **A button in the notification** (2023 to 2025). It was removed in [#2738]. No reason was written down, and
  listeners missed it: "It was a very convenient way of setting the sleep timer without having to open the app".
- ❌ **A sleep timer widget**: added and removed the same day, before it shipped ([#3829], [#3839]). No reason was
  written down.

### "Other things should switch off too"

Bluetooth, or the app itself. There's little evidence for this.

- ❌ "only be useful for a very little amount of users and thus I prefer to keep the structure simple." ([#391])

## Principles at work

- **Prefer good behavior to a setting** ([design principles]). Shake and the bookmark used to be optional, and became
  always on in 2020 ([f11c34407]).
- **Many listeners, not one power user.** This is the reason given for most of the ❌ above.

## Open questions

1. **End of chapter behaves differently from the timed timer.** It has no fade-out, no shake to restart and no
   "Dozed off" bookmark. Is that on purpose? **Guess:** no. It was built years later, on its own.
2. **The exact time also sets the automatic timer's length.** Pressing − or + in the sheet changes how long tonight's
   automatic timer runs, and Settings doesn't show that length. Is this coupling okay?
3. **One tap on the moon cancels a running timer.** You can't change or extend a running timer without cancelling it,
   and an accidental tap cancels it with no feedback. Should a tap open the sheet, with "Turn off" as one of its
   options?
4. **Why were the notification button and the sleep timer widget dropped?** Writing it down makes the next request
   easier to answer.
5. **Headphone listeners outside the night window.** Is widening the automatic timer's window the answer to [#2233], or is
   there a better default?
6. **Is 30 seconds right for shake?** It's too short when you're half asleep ([#3278]). The old, longer window restarted
   books by accident.
7. **Where should the "Dozed off" bookmark go?** It marks where the timer started. Now that the history shows where the
   timer ended, is the start still the right place?

The Play build records each time a timer starts, with its mode and duration (`sleep_timer_enabled`). That shows which
quick choices people use and how often they pick end of chapter.

## How it got here

- **2014:** one fixed duration set in settings, and the moon turns the timer on or off.
- **2015:** a bookmark when the timer starts, the countdown on screen, and the timer waits while paused.
- **2016:** choose the time each time you start it, and shake to restart.
- **2018:** fade-out with a rewind.
- **2020:** shake and the bookmark become always on, and the shake window becomes 30 seconds.
- **2023:** quick choices of 5, 15, 30 and 60 minutes, the timer in the notification, and a first end of chapter
  (reverted in 2024).
- **2025:** the timer leaves the notification. The automatic sleep timer arrives, end of chapter ships, and exact times
  get 1-minute steps.
- **2026:** a new sleep sheet with a night look, Voice's own shake detection, a listening history with "Dozed off",
  and the widget shows when playback will stop. All of this is in the next release.

[design principles]: ../.agents/skills/design/SKILL.md
[f11c34407]: https://github.com/PaulWoitaschek/Voice/commit/f11c34407
[#101]: https://github.com/PaulWoitaschek/Voice/issues/101
[#188]: https://github.com/PaulWoitaschek/Voice/issues/188
[#372]: https://github.com/PaulWoitaschek/Voice/issues/372
[#391]: https://github.com/PaulWoitaschek/Voice/issues/391
[#393]: https://github.com/PaulWoitaschek/Voice/issues/393
[#414]: https://github.com/PaulWoitaschek/Voice/issues/414
[#426]: https://github.com/PaulWoitaschek/Voice/issues/426
[#464]: https://github.com/PaulWoitaschek/Voice/issues/464
[#474]: https://github.com/PaulWoitaschek/Voice/issues/474
[#535]: https://github.com/PaulWoitaschek/Voice/issues/535
[#742]: https://github.com/PaulWoitaschek/Voice/issues/742
[#936]: https://github.com/PaulWoitaschek/Voice/issues/936
[#954]: https://github.com/PaulWoitaschek/Voice/issues/954
[#974]: https://github.com/PaulWoitaschek/Voice/issues/974
[#981]: https://github.com/PaulWoitaschek/Voice/issues/981
[#988]: https://github.com/PaulWoitaschek/Voice/issues/988
[#991]: https://github.com/PaulWoitaschek/Voice/issues/991
[#1026]: https://github.com/PaulWoitaschek/Voice/issues/1026
[#1533]: https://github.com/PaulWoitaschek/Voice/issues/1533
[#2175]: https://github.com/PaulWoitaschek/Voice/issues/2175
[#2264]: https://github.com/PaulWoitaschek/Voice/pull/2264
[#2668]: https://github.com/PaulWoitaschek/Voice/pull/2668
[#2673]: https://github.com/PaulWoitaschek/Voice/pull/2673
[#2738]: https://github.com/PaulWoitaschek/Voice/pull/2738
[#2788]: https://github.com/PaulWoitaschek/Voice/issues/2788
[#2838]: https://github.com/PaulWoitaschek/Voice/pull/2838
[#2842]: https://github.com/PaulWoitaschek/Voice/pull/2842
[#2934]: https://github.com/PaulWoitaschek/Voice/pull/2934
[#3110]: https://github.com/PaulWoitaschek/Voice/issues/3110
[#3829]: https://github.com/PaulWoitaschek/Voice/pull/3829
[#3839]: https://github.com/PaulWoitaschek/Voice/pull/3839
[#1323]: https://github.com/PaulWoitaschek/Voice/discussions/1323
[#1339]: https://github.com/PaulWoitaschek/Voice/discussions/1339
[#1375]: https://github.com/PaulWoitaschek/Voice/discussions/1375
[#1421]: https://github.com/PaulWoitaschek/Voice/discussions/1421
[#1473]: https://github.com/PaulWoitaschek/Voice/discussions/1473
[#1853]: https://github.com/PaulWoitaschek/Voice/discussions/1853
[#2177]: https://github.com/PaulWoitaschek/Voice/discussions/2177
[#2233]: https://github.com/PaulWoitaschek/Voice/discussions/2233
[#2281]: https://github.com/PaulWoitaschek/Voice/discussions/2281
[#2774]: https://github.com/PaulWoitaschek/Voice/discussions/2774
[#2943]: https://github.com/PaulWoitaschek/Voice/discussions/2943
[#2983]: https://github.com/PaulWoitaschek/Voice/discussions/2983
[#3216]: https://github.com/PaulWoitaschek/Voice/discussions/3216
[#3278]: https://github.com/PaulWoitaschek/Voice/discussions/3278
[#3288]: https://github.com/PaulWoitaschek/Voice/discussions/3288
[#3382]: https://github.com/PaulWoitaschek/Voice/discussions/3382
[#3410]: https://github.com/PaulWoitaschek/Voice/discussions/3410
[#3485]: https://github.com/PaulWoitaschek/Voice/discussions/3485
[#3675]: https://github.com/PaulWoitaschek/Voice/discussions/3675
