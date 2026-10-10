# Player

**Status:** Outline.

## Outcome

Listeners press play and follow the story at their own pace, and Voice never loses their place.

## Who and when

At home, on the way to work, or doing chores, often with their eyes somewhere else. Books are long, sometimes 20 hours
or more with few chapters.

## Opportunities

### "It forgets my position, jumps, or just stops"

This is about trust. There are 23 threads with low votes but long discussions. "When ever I pause it it goes to a
position before or after where I actually paused." ([#917]) "right now it seems to stop playing the audio randomly"
([#1061])

- ✅ **Asking to lift battery limits**, at most 3 times ([#1839], 2023), and keeping the phone awake while playing
  ([#1924]).
- ✅ **Auto rewind only when playback really pauses** ([#3853]). *(next release)*
- Some causes are outside Voice. Broken files: "the files are MP3s with an invalid bitrate of 20 kb/s, which throws off
  the duration and the position" ([#3422]). Phone makers: "Huawei is known for crazy battery optimizations" ([#1061]).

### "I lose the thread when I come back"

- ✅ **Auto rewind**: when you resume, Voice goes back a little (2 seconds by default, adjustable). "Wanted to keep it
  simple, so there is a setting with a default jump of 0s and a maximum jump of 20s" ([#87], 2015)
- 💡 **Rewind after a call or a navigation prompt** ([#1368]): "sometimes it pauses in the middle of a word, making it
  easy to miss what was said once audio continues". In 2026: "I left that alone." ([#3853])

### "I have to set the speed again for every book"

The most upvoted open player request: [#1790] (20 upvotes, about 54 across 8 threads). "so you don't have to set every
single book to 1.25x by hand" ([#3867])

- ✅ **Speed is saved per book**, from 0.5× to 3.5×.
- ❌ **A default speed setting**, in 2020: "I also think that the vast majority people just plays at 1.0 speed so I
  don't want another setting." And: "It's mostly not about the maintenance cost. I'ts about providing good defaults and
  having a simple ui." ([#860])
- 💡 **A default speed** ([#1790]).
- 💡 **New books start at the speed of your last book.** Nobody asked for exactly this. It's an option that needs no
  setting.

### "How much is left, really, at my speed?"

16 threads with 133 upvotes; the top one is [#1422]. "It currently doesn't properly reflect how much time is actually
left in the chapter / book." ([#1292])

- ✅ **How much of the book you've heard, and the time left at your speed**, in the player ([#3792]).
  *(next release)*
- 💡 **All times adjusted to the speed** ([#3226]), and **tap to switch between elapsed and remaining** ([#1311]).

### "The seek bar is useless on a 20-hour book, and one slip loses my place"

"Using the seek bar with fidelity is quite difficult on a 22 hour long book, with no chapters." ([#2452]) "I often hit
either the track selection drop-down (or the next/prev buttons) or a random location on the progress bar" ([#2195])

- ✅ **Skip buttons** with an adjustable amount: 20 seconds by default, from 3 to 60.
- ✅ **Chapters**: a list, plus next and previous.
- ✅ **The jump-back pill** after a big seek or a chapter change ([#3805]). This is the design principle "Prefer good
  behavior to a setting" in practice. *(next release)*
- 💡 **Jump to a typed time** ([#1901]). This existed before version 6.
- 💡 **Different amounts for back and forward** ([#1670]), and **locking the controls** ([#2195]).

### "This book is too quiet, too shrill, or full of pauses"

"Skipping silence is great but it feels like speedrunning something" ([#1802]). "sometimes cuts off the first or last
letters of phrases along with the silence" ([#3709]). "Now volume boost is written to be up to 9 db. It is noticeable,
but no much." ([#1712]) "sometimes audio books are a bit shrill" ([#2292])

- ✅ **Skip silence and volume boost** (up to 9 dB), both saved per book.
- ❌ **Tuning skip silence**: "I'd like to keep the media3 defaults for now." ([#3741])
- 💡 **An equalizer** ([#2292], [#1353]), **mono** ([#1539]), **even loudness** ([#1372]), and **a stronger boost**
  ([#1712]).

### "Other apps' sounds break my book"

"I would like to be able to play instrumental music on Spotify simultaneously while listening to an Audio Book"
([#2259])

- ✅ **Voice pauses for calls and for short sounds like navigation prompts**, then carries on. In 2015 it got quieter
  instead: "The player should lower volume when the audiofocus gets lost temporary" ([#107]). Pausing came after a
  listener asked for it.
- 💡 **Keep playing under other audio** ([#2259]).

## Open questions

1. Default speed: a setting, or a smart default that needs no setting? It's the most upvoted open player request.
2. Should Voice rewind after a call or a navigation prompt ([#1368])?
3. When a file is broken ([#3422]), can Voice tell the listener instead of drifting quietly?

[#87]: https://github.com/PaulWoitaschek/Voice/issues/87
[#107]: https://github.com/PaulWoitaschek/Voice/issues/107
[#860]: https://github.com/PaulWoitaschek/Voice/issues/860
[#917]: https://github.com/PaulWoitaschek/Voice/issues/917
[#1061]: https://github.com/PaulWoitaschek/Voice/issues/1061
[#1292]: https://github.com/PaulWoitaschek/Voice/discussions/1292
[#1311]: https://github.com/PaulWoitaschek/Voice/discussions/1311
[#1353]: https://github.com/PaulWoitaschek/Voice/discussions/1353
[#1368]: https://github.com/PaulWoitaschek/Voice/discussions/1368
[#1372]: https://github.com/PaulWoitaschek/Voice/discussions/1372
[#1422]: https://github.com/PaulWoitaschek/Voice/discussions/1422
[#1539]: https://github.com/PaulWoitaschek/Voice/discussions/1539
[#1670]: https://github.com/PaulWoitaschek/Voice/discussions/1670
[#1712]: https://github.com/PaulWoitaschek/Voice/discussions/1712
[#1790]: https://github.com/PaulWoitaschek/Voice/discussions/1790
[#1802]: https://github.com/PaulWoitaschek/Voice/discussions/1802
[#1839]: https://github.com/PaulWoitaschek/Voice/pull/1839
[#1901]: https://github.com/PaulWoitaschek/Voice/discussions/1901
[#1924]: https://github.com/PaulWoitaschek/Voice/pull/1924
[#2195]: https://github.com/PaulWoitaschek/Voice/discussions/2195
[#2259]: https://github.com/PaulWoitaschek/Voice/discussions/2259
[#2292]: https://github.com/PaulWoitaschek/Voice/discussions/2292
[#2452]: https://github.com/PaulWoitaschek/Voice/discussions/2452
[#3226]: https://github.com/PaulWoitaschek/Voice/discussions/3226
[#3422]: https://github.com/PaulWoitaschek/Voice/issues/3422
[#3709]: https://github.com/PaulWoitaschek/Voice/discussions/3709
[#3741]: https://github.com/PaulWoitaschek/Voice/pull/3741
[#3792]: https://github.com/PaulWoitaschek/Voice/pull/3792
[#3805]: https://github.com/PaulWoitaschek/Voice/pull/3805
[#3853]: https://github.com/PaulWoitaschek/Voice/pull/3853
[#3867]: https://github.com/PaulWoitaschek/Voice/discussions/3867
