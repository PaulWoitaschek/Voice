# Bookmarks and history

**Status:** Outline.

## Outcome

Listeners keep the moments that matter, and they can always get back to where they were.

## Who and when

While listening, often away from the screen: walking, driving, cooking. Later, when studying a book or looking for a
scene. And right after an accidental tap or seek.

## Opportunities

### "Let me save a moment without stopping"

"I have to take my phone out, fumble through the settings and set a bookmark" ([#1289]). "Because you always create
the bookmark after you hear something!" ([#3368])

- ✅ **Long-press the bookmark button** to save right away.
- ✅ **Save first, add details after**: a kind (favorite, quote, note, revisit), a note, and moving the mark 15 seconds
  earlier or later.
- 🔜 **A bookmark is a moment and an optional note.** Saving no longer asks for a kind. Each kind needs its own shape,
  color, icon and filter, and picking one is a decision in the middle of listening. Replaces the kinds.
- ⭕ **Saving a moment still means taking out the phone and opening Voice** ([#1289]).

Assumptions:

- **Listeners say why a moment matters in the note, not with a kind**: Guess. Ask in a discussion.

### "Help me find the right one"

"In that case the bookmark gets on top of the others, but it's about an earlier time." ([#3168])

- ✅ **Story order** with chapter headers and "You are here", or **recently saved** first. Kinds work as filters, pins
  show on the progress bar, and swiping deletes with undo.
- ⭕ **The listener's own bookmarks get lost among automatic ones.** The automatic sleep timer adds one every night,
  and on the player's progress bar they pile up into a cluster nobody can read.
- 🔜 **Bookmarks are only what the listener saved.** What Voice records by itself goes into the history. The sleep timer
  stops adding bookmarks (see [Sleep timer](sleep-timer.md)).
- 🔜 **Pins only on the bookmarks screen.** On the player, the book bar shows chapters and progress. Bookmarks are a
  detail one step away, not something you need while listening.
- ❌ **A separate notes feature**: "Isn't that what bookmarks already are?" ([#2123])

### "Take me back to where I was"

After an accidental tap, a seek, or opening a bookmark. "can't go back to the position where you were listening before"
([#454]) "so i could at least find where i stopped last time" ([#1669])

- ✅ **The jump-back pill** right after a big seek or a chapter change (see [Player](player.md)).
- ✅ **Listening history**: a timeline of plays, pauses, jumps and sleep, showing what caused each one (the app,
  headphones, the car). You can jump back from any entry. It's always on, because a switch and a clear button "were more
  than it needs" ([#3824]).
- ⭕ **Finding a lost place in the history means reading a log.** Every play, pause, skip, setting change and bookmark
  is a row of its own, with filters by type and by source, "Lost your place?" suggestions, and undo for settings and
  deleted bookmarks.
- 🔜 **The history lists stretches of listening**: when, and from where to where in the book. A big jump starts a new
  one, so the place before an accidental jump is where the stretch before it ends. A stretch the sleep timer ended
  says so. What started or stopped it (headphones, the car) is a small line, not a filter. Tapping a row goes there.
  Replaces the timeline of events, with its filters, suggestions and undo. Settings and bookmarks aren't in the history
  anymore, and deleting a bookmark already has undo in the list.
- ❌ **Filters by type and by source**: a list of stretches has nothing left to filter.
- ❌ **"Lost your place?" suggestions**: the pill offers the way back right away, and later the list shows it without
  guessing.
- ❌ **The 2016 answer** was "Then create a bookmark before you click on another bookmark" ([#454]). The history replaces
  it, because the old design meant "accidental taps or seeks couldn't be undone" ([#3805]).

Assumptions:

- **Where each stretch ends is enough to find a lost place**, without seeing every skip and pause: Guess.

### "My bookmarks are notes, and I need them outside the app"

"Being able to export Notes/Bookmarks as a text file would improve the app usability as a study tool." ([#1585])

- ❌ **Turned down in 2016**: "An import / export functionality would require too much work with little benefit for
  most of the users." ([#367])
- ⭕ **Bookmarks and their notes can't leave the app** ([#2549]).

### "Don't lose my bookmarks"

This happens on a new phone or after moving files. See [Servers, sync and backup](servers-and-sync.md) and
[Getting started](getting-started.md).

- ✅ **Bookmarks sync with Audiobookshelf**, except the ones the sleep timer sets.

## Open questions

1. Export: ten years after "too much work", does the principle that "the data stays with the listener" change the
   answer? A text export is much smaller than a full backup.
2. A book's history follows it to a new id after a rescan. Does that fix losing bookmarks after a move ([#2636])?
3. What happens to what listeners already saved: the "Dozed off" bookmarks, and the kind of each bookmark?

[#367]: https://github.com/PaulWoitaschek/Voice/issues/367
[#454]: https://github.com/PaulWoitaschek/Voice/issues/454
[#1289]: https://github.com/PaulWoitaschek/Voice/discussions/1289
[#1585]: https://github.com/PaulWoitaschek/Voice/discussions/1585
[#1669]: https://github.com/PaulWoitaschek/Voice/discussions/1669
[#2123]: https://github.com/PaulWoitaschek/Voice/discussions/2123
[#2549]: https://github.com/PaulWoitaschek/Voice/discussions/2549
[#2636]: https://github.com/PaulWoitaschek/Voice/issues/2636
[#3168]: https://github.com/PaulWoitaschek/Voice/discussions/3168
[#3368]: https://github.com/PaulWoitaschek/Voice/discussions/3368
[#3805]: https://github.com/PaulWoitaschek/Voice/pull/3805
[#3824]: https://github.com/PaulWoitaschek/Voice/pull/3824
