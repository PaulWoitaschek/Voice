# Outside the app

**Status:** Outline.

Listeners control their book from wherever they are (headphones, the lock screen, the car, the home screen) without
opening Voice, and without losing their place.

**When:** The phone is in a pocket or a car holder, the screen is off, and their hands are busy.

| Problem | Status |
|---|---|
| [The car doesn't remember my book](#the-car-doesnt-remember-my-book) | ✅ Solved |
| [The car's skip buttons look like track buttons](#the-cars-skip-buttons-look-like-track-buttons) | ✅ Solved |
| [I can't jump to a chapter by voice](#i-cant-jump-to-a-chapter-by-voice) | ❌ Won't solve |
| [I can't change the speed in the car](#i-cant-change-the-speed-in-the-car) | ⭕ Open |
| [Headphone and car buttons don't do what I expect](#headphone-and-car-buttons-dont-do-what-i-expect) | ✅ Solved |
| [The lock screen slider loses my place](#the-lock-screen-slider-loses-my-place) | ✅ Solved |
| [The slider still moves my position within the chapter](#the-slider-still-moves-my-position-within-the-chapter) | ✅ Solved |
| [The notification goes too soon or stays too long](#the-notification-goes-too-soon-or-stays-too-long) | ✅ Solved |
| [Starting my book from the home screen or an automation means opening Voice](#starting-my-book-from-the-home-screen-or-an-automation-means-opening-voice) | ✅ Solved |
| [My book isn't on my home screen](#my-book-isnt-on-my-home-screen) | ✅ Solved |
| [I can't play my book on a speaker or TV](#i-cant-play-my-book-on-a-speaker-or-tv) | ⭕ Open |
| [I can't listen on my watch without my phone](#i-cant-listen-on-my-watch-without-my-phone) | ⭕ Open |
| [The Assistant works poorly with Voice](#the-assistant-works-poorly-with-voice) | ❌ Won't solve |

## The car doesn't remember my book

✅ **Solved** · [#1765]

In the car, listeners want to go on with the book they were listening to. The F-Droid build has no Android Auto. See
[Feedback and support](feedback-and-support.md).

**Solution:** Android Auto shows recent books, sorted by when you last played them, and has the current book ready when
you connect.

## The car's skip buttons look like track buttons

✅ **Solved** · [#2797]

In Android Auto, standard track buttons for rewind and fast forward make listeners think they go to the previous or
next track, but they jump 20 seconds.

**Solution:** Clear skip icons for rewind and fast forward.

## I can't jump to a chapter by voice

❌ **Won't solve** · [#231]

In the car, listeners want to start a chapter by asking for it.

**Why not:** When voice detection is flaky, a misheard request could lose the listener's latest position in the book.

## I can't change the speed in the car

⭕ **Open** · [#1878]

In traffic, some listeners can only follow at a lower speed, around 1.2×, and the car has no way to change it.

## Headphone and car buttons don't do what I expect

✅ **Solved** · [#3846]

Some listeners expect next and previous on their headphones or in the car to go to the next or previous track, and Voice
skips a few seconds instead ([#1883]).

**Solution:** Next and previous skip by the skip amount from everywhere outside the app: headsets, cars, watches and
the Assistant.

**Turned down:**

- Next and previous change the chapter: seeking matters far more than skipping chapters ([#231]). That was in 2017.
- Choosing what each press does: too specific to build in ([#456]).

## The lock screen slider loses my place

✅ **Solved** · [#3229]

The slider on the lock screen moves in a pocket or after an accidental touch, and the listener's position is gone.

**Solution:**

- The notification's seek bar only covers the current chapter.
- The listening history can take you back (see [Bookmarks and history](bookmarks.md)).

## The slider still moves my position within the chapter

✅ **Solved** · [#3229]

A stray touch on the slider still moves the position, just not past the current chapter. A chapter can be an hour long,
and a file without chapter marks is a single chapter. Some listeners never want to seek from the lock screen.

**Solution:** A "Lock screen slider" switch in the Listening settings, on by default. Off, the slider still shows the
progress but has no handle and ignores touches. It's a setting because listeners want opposite things: some scrub the
chapter from the lock screen ([#3289]), others never do. Bluetooth and Android Auto read the same session as the lock
screen, so their sliders follow the switch too.

## The notification goes too soon or stays too long

✅ **Solved** · [#868]

Listeners want the notification to stay while they pause, and to go when they're done. The two sides pull apart: some
listeners want playback to start when Bluetooth connects ([#1369]), and others don't ([#915]).

**Solution:** A paused notification stays, and the listener can swipe it away.

## Starting my book from the home screen or an automation means opening Voice

✅ **Solved** · [#1913]

Listeners want to control their book from the home screen, or from automations like Tasker. Some use Tasker commands
several times a day ([#3410]).

**Solution:**

- A "Play current" shortcut.
- Commands that Tasker can send. The first answer was no, and then they turned out to be easy to bring back.

## My book isn't on my home screen

✅ **Solved**

Listeners want to see their book on the home screen, and play it from there.

**Solution:**

- Now playing and Shelf widgets in any size, down to 1×1.
- A small widget keeps what matters most. As space runs out, it keeps play, then the cover, then the title, then
  rewind, then fast forward.
  - The cover comes before the title because sighted listeners recognize their book faster by its cover. A cover also
    needs less room than a readable title, so a small widget stays usable.
  - Rewind comes before fast forward because listeners jump back, after missing a sentence or dozing off, far more often
    than they skip ahead.

## I can't play my book on a speaker or TV

⭕ **Open** · [#76]

Listeners have asked for casting since 2014, to play their books on Android TV and smart speakers ([#1778]). No reason
for leaving it out is on record. One concern: Google's cast library would effectively make Voice proprietary software.

## I can't listen on my watch without my phone

⭕ **Open** · [#1749]

A watch can't play a book on its own. Today it works well as a remote for playback on the phone.

## The Assistant works poorly with Voice

❌ **Won't solve** · [#1071]

Listeners want to control their book by voice through the Assistant, for example to start and pause it while driving.

**Why not:** It isn't possible. It's a limit of the Assistant: only a few players that work directly with Google are on
its allowlist.

## Open questions

1. Casting: is it still wanted, and what's the real blocker, the proprietary library or the effort?
2. Speed in the car: does it fit, given the "seek first" choice for Android Auto?

[#76]: https://github.com/PaulWoitaschek/Voice/issues/76
[#231]: https://github.com/PaulWoitaschek/Voice/issues/231
[#456]: https://github.com/PaulWoitaschek/Voice/issues/456
[#868]: https://github.com/PaulWoitaschek/Voice/issues/868
[#915]: https://github.com/PaulWoitaschek/Voice/issues/915
[#1071]: https://github.com/PaulWoitaschek/Voice/issues/1071
[#1369]: https://github.com/PaulWoitaschek/Voice/discussions/1369
[#1749]: https://github.com/PaulWoitaschek/Voice/discussions/1749
[#1765]: https://github.com/PaulWoitaschek/Voice/issues/1765
[#1778]: https://github.com/PaulWoitaschek/Voice/discussions/1778
[#1878]: https://github.com/PaulWoitaschek/Voice/discussions/1878
[#1883]: https://github.com/PaulWoitaschek/Voice/discussions/1883
[#1913]: https://github.com/PaulWoitaschek/Voice/issues/1913
[#2797]: https://github.com/PaulWoitaschek/Voice/issues/2797
[#3229]: https://github.com/PaulWoitaschek/Voice/discussions/3229
[#3289]: https://github.com/PaulWoitaschek/Voice/discussions/3289
[#3410]: https://github.com/PaulWoitaschek/Voice/discussions/3410
[#3846]: https://github.com/PaulWoitaschek/Voice/issues/3846
