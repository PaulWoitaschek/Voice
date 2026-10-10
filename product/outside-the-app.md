# Outside the app

**Status:** Outline.

## Outcome

Listeners control their book from wherever they are (headphones, the lock screen, the car, the home screen) without
opening Voice, and without losing their place.

## Who and when

The phone is in a pocket or a car holder, the screen is off, and their hands are busy.

## Opportunities

### "Let me listen in the car"

[#231] has 64 comments. "it won't remember the current book" ([#1765])

- ✅ **Android Auto**: recent books, sorted by when you last played them, and the current book ready when you connect
  ([#1940], [#1941], 2023).
- ✅ **Clear skip icons** for rewind and fast forward ([#3272], 2025), after "This is confusing as they actually
  rewind/forward 20 seconds." ([#2797])
- ❌ **Voice search for chapters**: "Else when voice detection is flaky you might lose the latest position in your
  audiobook." ([#231])
- 💡 **Speed control in the car** ([#1878], 10 upvotes): "I can only focus on 1.2x speed or so when there is traffic"

The F-Droid build has no Android Auto. See [Feedback and support](feedback-and-support.md).

### "My headphone and car buttons should do what I expect"

"Voice doesn't go to the next / previous track, but moves fast forward/backward several seconds." ([#1883])

- ✅ **Next and previous skip by the skip amount**: "Decided to keep skipping for next and previous from outside
  (headsets, cars, watches, the Assistant)." ([#3846]) The reason, from 2017: "I find the importance of the seek
  functionality way greater than the importance of the chapter skip functionality." ([#231])
- ❌ **Choosing what each press does**: "I find this to be too specific to put it an action." ([#456])

### "The lock screen slider loses my place"

[#3229], with 12 upvotes: "it gets randomly activated either in my pocket or when I touch the screen accidentally, and
I lose the current position with a lot of frustration"

- ✅ **The notification's seek bar only covers the current chapter** ([#3634]). *(next release)*
- ✅ **The listening history** can take you back (see [Bookmarks and history](bookmarks.md)). *(next release)*
- 💡 **Turn off seeking from the notification** ([#3229]).

### "The notification should stay while I pause, and go when I'm done"

"keep the notification until dismissed" ([#868])

- ✅ **A paused notification can be swiped away**: "This is because I want the user to be able to swipe-dismiss the
  notification in paused state." ([#868])
- The two sides pull apart: some want playback to start when Bluetooth connects ([#1369]), others don't ([#915]).

### "Let me control it from my home screen or an automation"

"now i use that daily [multiple times a day actually]" ([#3410], about Tasker)

- ✅ **A "Play current" shortcut** (2018).
- ✅ **Commands that Tasker can send** ([#2075], 2023). The first answer was no, then: "I need to revise my post and I
  think this is actually something we can easily bring back!" ([#1913])
- ✅ **Now playing and Shelf widgets**, down to 1×1 ([#3829], [#3832]). *(next release)*

### "Play it on my speaker or TV"

Casting has been requested since 2014 ([#76]). "Add the cast option so I can play on Android TV and Smart speakers"
([#1778])

- 💡 **Casting** ([#76], [#1778]). No reason for leaving it out is on record. One concern: the Google cast library
  "effectively makes this project proprietary software." ([#76])

### "Let me use it from my watch, or by voice"

- 💡 **A Wear OS app** ([#1749]). Today a watch works as a remote: "it works great as a controller of audiobook
  playback on my phone" ([#1749])
- ❌ **Better Assistant support** isn't possible: "This is a limitation of the Android Assistant. There are only a few
  players who are in direct contact with Google that are whitelisted within the Assistant." ([#1071])

## Open questions

1. Casting: is it still wanted, and what's the real blocker, the proprietary library or the effort?
2. The lock screen slider: is limiting it to the current chapter enough, or should it be possible to turn it off?
3. Speed in the car ([#1878]): does it fit, given the "seek first" choice for Android Auto?

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
[#1940]: https://github.com/PaulWoitaschek/Voice/pull/1940
[#1941]: https://github.com/PaulWoitaschek/Voice/pull/1941
[#2075]: https://github.com/PaulWoitaschek/Voice/pull/2075
[#2797]: https://github.com/PaulWoitaschek/Voice/issues/2797
[#3229]: https://github.com/PaulWoitaschek/Voice/discussions/3229
[#3272]: https://github.com/PaulWoitaschek/Voice/pull/3272
[#3410]: https://github.com/PaulWoitaschek/Voice/discussions/3410
[#3634]: https://github.com/PaulWoitaschek/Voice/pull/3634
[#3829]: https://github.com/PaulWoitaschek/Voice/pull/3829
[#3832]: https://github.com/PaulWoitaschek/Voice/pull/3832
[#3846]: https://github.com/PaulWoitaschek/Voice/issues/3846
