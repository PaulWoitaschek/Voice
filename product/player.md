# Player

Listeners press play, follow the story at their own pace, and never lose their place.

## Solved

- **Playback stops on its own.** Voice asks to lift battery limits and keeps the phone awake while playing.
- **I lose the thread after a pause.** Auto rewind goes back a little when playback pauses, 2 seconds by default.
- **I have to set the speed again for every book.** Speed is saved per book, and a new book starts at the speed
  listened at last.
- **I can't tell how much of the book is left.** The player shows the progress and the time left at the current speed.
- **One slip loses my place.** After a big seek, a chapter change or going to a bookmark, the jump-back pill takes the
  listener back.
- **A book is too quiet or full of pauses.** Volume boost and skip silence, saved per book.
- **Calls and navigation prompts talk over my book.** Voice pauses and carries on afterwards.

## Open

- **After a call or a navigation prompt, I miss what was said.** [#1368]
- **A stray touch still skips or seeks.** [#2195]
- **Getting to an exact time takes many skips.** [#1901]
- **One skip amount doesn't suit both directions.** [#1670]
- **Times in different places disagree**, because not all of them follow the speed. [#3226]
- **Skip silence clips words.** [#2252]
- **Some books still sound wrong**: shrill, uneven, or meant for two ears. [#2292]
- **I can't play music under my book.** [#2259]

## Won't solve

- **A default speed setting:** starting a new book at the last speed needs no setting ([#1790]).

[#1368]: https://github.com/PaulWoitaschek/Voice/discussions/1368
[#1670]: https://github.com/PaulWoitaschek/Voice/discussions/1670
[#1790]: https://github.com/PaulWoitaschek/Voice/discussions/1790
[#1901]: https://github.com/PaulWoitaschek/Voice/discussions/1901
[#2195]: https://github.com/PaulWoitaschek/Voice/discussions/2195
[#2252]: https://github.com/PaulWoitaschek/Voice/discussions/2252
[#2259]: https://github.com/PaulWoitaschek/Voice/discussions/2259
[#2292]: https://github.com/PaulWoitaschek/Voice/discussions/2292
[#3226]: https://github.com/PaulWoitaschek/Voice/discussions/3226
