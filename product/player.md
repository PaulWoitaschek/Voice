# Player

**Status:** Outline.

Listeners press play and follow the story at their own pace, and Voice never loses their place.

**When:** At home, on the way to work, or doing chores, often with their eyes somewhere else. Books are long, sometimes
20 hours or more with few chapters.

| Problem | Status |
|---|---|
| [Playback stops on its own](#playback-stops-on-its-own) | ✅ Solved |
| [My position jumps when I pause](#my-position-jumps-when-i-pause) | ✅ Solved |
| [I lose the thread when I come back](#i-lose-the-thread-when-i-come-back) | ✅ Solved |
| [After a call or a navigation prompt, I miss what was said](#after-a-call-or-a-navigation-prompt-i-miss-what-was-said) | ⭕ Open |
| [I have to set the speed again for every book](#i-have-to-set-the-speed-again-for-every-book) | ✅ Solved |
| [I can't tell how much is left at my speed](#i-cant-tell-how-much-is-left-at-my-speed) | ✅ Solved |
| [Times in different places disagree](#times-in-different-places-disagree) | ⭕ Open |
| [The seek bar is too coarse for a long book](#the-seek-bar-is-too-coarse-for-a-long-book) | ✅ Solved |
| [One slip loses my place](#one-slip-loses-my-place) | ✅ Solved |
| [A stray touch still skips or seeks](#a-stray-touch-still-skips-or-seeks) | ⭕ Open |
| [Getting to an exact time takes many skips](#getting-to-an-exact-time-takes-many-skips) | ⭕ Open |
| [One skip amount doesn't suit both directions](#one-skip-amount-doesnt-suit-both-directions) | ⭕ Open |
| [A book is too quiet or full of pauses](#a-book-is-too-quiet-or-full-of-pauses) | ✅ Solved |
| [Skip silence feels rushed and clips words](#skip-silence-feels-rushed-and-clips-words) | ⭕ Open |
| [Some books still sound wrong](#some-books-still-sound-wrong) | ⭕ Open |
| [Calls and navigation prompts talk over my book](#calls-and-navigation-prompts-talk-over-my-book) | ✅ Solved |
| [I can't play music under my book](#i-cant-play-music-under-my-book) | ⭕ Open |

## Playback stops on its own

✅ **Solved** · [#1061]

Playback stops at random, and listeners stop trusting Voice. Some causes are outside Voice. Phone makers like Huawei
are known for aggressive battery optimizations.

**Solution:** Voice asks to lift battery limits, at most 3 times, and keeps the phone awake while playing.

## My position jumps when I pause

✅ **Solved** · [#917]

After a pause, the book goes on from before or after the spot where the listener paused. Some causes are outside Voice:
MP3s with an invalid bitrate of 20 kb/s throw off the duration and the position ([#3422]).

**Solution:** Auto rewind only runs when playback really pauses.

## I lose the thread when I come back

✅ **Solved** · [#87]

When listeners resume after a break, they've lost the thread of the story.

**Solution:** Auto rewind. When you resume, Voice goes back a little, 2 seconds by default and adjustable. It started
in 2015 as a setting, to keep it simple, with a default of 0 seconds and a maximum of 20.

## After a call or a navigation prompt, I miss what was said

⭕ **Open** · [#1368]

After a call or a navigation prompt, the book carries on without rewinding. It may have paused in the middle of a
word, so it's easy to miss what was said. When auto rewind was reworked in 2026, this was left alone ([#3853]).

## I have to set the speed again for every book

✅ **Solved** · [#3867]

Listeners who listen faster have to set every book to their speed, like 1.25×, by hand.

**Solution:**

- Speed is saved per book, from 0.5× to 3.5×.
- A new book starts at the speed you last listened at ([#1790]). This needs no setting, and it follows the listener
  when their speed changes. A speed picked for a book before its first play stays.

**Turned down:**

- A default speed setting: most listeners play at 1.0×, so it wasn't worth another setting. The point was good
  defaults and a simple UI, not the cost of maintaining it ([#860]). That was in 2020.

## I can't tell how much is left at my speed

✅ **Solved** · [#1292]

Times that ignore the speed don't show how much of the chapter or the book is really left.

**Solution:** The player shows how much of the book you've heard, and the time left at your speed.

## Times in different places disagree

⭕ **Open** · [#3226]

Not every time in Voice follows the speed, so the times in different places don't match.

## The seek bar is too coarse for a long book

✅ **Solved** · [#2452]

On a 22-hour book with no chapters, the seek bar is hard to use with any precision.

**Solution:**

- Skip buttons with an adjustable amount: 20 seconds by default, from 3 to 60.
- Chapters: a list, plus next and previous.

## One slip loses my place

✅ **Solved** · [#2195]

Listeners hit the track drop-down, next or previous, or a random spot on the progress bar by accident, and their place
is gone.

**Solution:** The jump-back pill after a big seek or a chapter change. This is the design principle "Prefer good
behavior to a setting" in practice.

## A stray touch still skips or seeks

⭕ **Open** · [#2195]

The pill takes the listener back afterwards, but a stray touch on the player can still skip or seek.

## Getting to an exact time takes many skips

⭕ **Open** · [#1901]

In a long book, getting to an exact time takes many skips or a fiddly drag. Before version 6, Voice could jump to a
typed time.

## One skip amount doesn't suit both directions

⭕ **Open** · [#1670]

Rewind and fast forward use the same skip amount, and one amount doesn't suit both.

## A book is too quiet or full of pauses

✅ **Solved**

Some books are recorded too quietly, or are full of pauses.

**Solution:** Skip silence and volume boost, up to 9 dB. Both are saved per book.

## Skip silence feels rushed and clips words

⭕ **Open** · [#3709]

Skipping silence can feel like rushing through the book ([#1802]). It sometimes cuts off the first or last letters of
a phrase along with the silence.

**Turned down:**

- Tuning skip silence: it would change skip silence for everyone who uses it, so Voice keeps the media3 defaults for
  now ([#3741]).

## Some books still sound wrong

⭕ **Open** · [#2292]

Some books are shrill, too quiet even with the boost ([#1712]), uneven in loudness, or meant for two ears when the
listener has one earbud in.

## Calls and navigation prompts talk over my book

✅ **Solved** · [#107]

A call or a navigation prompt from another app plays over the book.

**Solution:** Voice pauses for calls and for short sounds like navigation prompts, then carries on. In 2015 it got
quieter instead. Pausing came after a listener asked for it.

## I can't play music under my book

⭕ **Open** · [#2259]

Some listeners want music from another app, like instrumentals on Spotify, playing under their book. They can't have
both.

## Open questions

1. Should Voice rewind after a call or a navigation prompt ([#1368])?
2. When a file is broken ([#3422]), can Voice tell the listener instead of drifting quietly?

[#87]: https://github.com/PaulWoitaschek/Voice/issues/87
[#107]: https://github.com/PaulWoitaschek/Voice/issues/107
[#860]: https://github.com/PaulWoitaschek/Voice/issues/860
[#917]: https://github.com/PaulWoitaschek/Voice/issues/917
[#1061]: https://github.com/PaulWoitaschek/Voice/issues/1061
[#1292]: https://github.com/PaulWoitaschek/Voice/discussions/1292
[#1368]: https://github.com/PaulWoitaschek/Voice/discussions/1368
[#1670]: https://github.com/PaulWoitaschek/Voice/discussions/1670
[#1712]: https://github.com/PaulWoitaschek/Voice/discussions/1712
[#1790]: https://github.com/PaulWoitaschek/Voice/discussions/1790
[#1802]: https://github.com/PaulWoitaschek/Voice/discussions/1802
[#1901]: https://github.com/PaulWoitaschek/Voice/discussions/1901
[#2195]: https://github.com/PaulWoitaschek/Voice/discussions/2195
[#2259]: https://github.com/PaulWoitaschek/Voice/discussions/2259
[#2292]: https://github.com/PaulWoitaschek/Voice/discussions/2292
[#2452]: https://github.com/PaulWoitaschek/Voice/discussions/2452
[#3226]: https://github.com/PaulWoitaschek/Voice/discussions/3226
[#3422]: https://github.com/PaulWoitaschek/Voice/issues/3422
[#3709]: https://github.com/PaulWoitaschek/Voice/discussions/3709
[#3741]: https://github.com/PaulWoitaschek/Voice/pull/3741
[#3853]: https://github.com/PaulWoitaschek/Voice/pull/3853
[#3867]: https://github.com/PaulWoitaschek/Voice/discussions/3867
