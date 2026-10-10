# Library

**Status:** Outline.

## Outcome

Listeners find the book they want in seconds, and can see at a glance how far along they are in each one.

## Who and when

Most visits are probably to continue the current book (**Guess**). Others are to pick the next one, or to tidy up.
Libraries range from one book to hundreds, and search "currently works well for those who have a small library"
([#571]).

## Opportunities

### "Let me get back to my book"

- ✅ **"Continue listening"** at the top, with a greeting ([#3792]). Nobody asked for these by name. *(next release)*
- ✅ **Shelves for Current, Not started and Completed**, and marking a book as one of them ([#1059], 2022). The older
  shelves confused people: "the startup screen with "new" and "more" is confusig to them (and me)." ([#906])

### "How far along am I?"

[#1422] (37 upvotes): "Since this has been a request for 5 years, I'm not holding out much hope"

- ✅ **Progress on each book** in the library ([#3170], 2025). For the player, see [Player](player.md).
- ❌ **Turned down in 2020**: "I try to follow a minimal design principle and therefore don't want another element in
  the UI." ([#834]) This was later reversed.
- Bug: finished books show 99% ([#3788]).

### "Let me sort my shelf"

The biggest library topic: 15 threads with about 59 upvotes around [#1543]. "I have many books from the same author and
it is difficult to hear them in the correct order." ([#1543]) "This is the main reason why I mostly still use Smart
Audiobook Player." ([#3235])

- ❌ **Sort options** were added in 2018 ([#765]) and removed the next day. No reason was written down.
- The maintainer suggested search instead: "What about searching for the author instead? Does that already cover your
  use case?" ([#1543])
- 💡 **Sort by name, date added, author, series, length, or by hand** ([#1543]).

### "Group my series and authors"

[#2343] and [#1748], about 47 upvotes together. "Could be a life-changing feature for those who like to collect
audiobook series" ([#1748])

- ✅ **Folders are authors**, when adding books (see [Getting started](getting-started.md)).
- ✅ **Search can browse by series, author, narrator and genre** ([#3817]). *(next release)*
- 💡 **Series and collections** ([#1748], [#2343]): "in the long term we should rather allow for a way to really
  support this kind of series." ([#1643])

### "Help me find a book fast"

- ✅ **Search** ([#1537], 2022) through titles, authors, narrators, series and genres, with recent searches
  ([#3817]). *(next release)*
- ✅ **Grid or list.**

### "Let me fix how a book looks"

"Currently I need to change the author in the audiofile itself" ([#1161]). "Voice forces them to fit into a square,
often cropping out stuff, like author or title." ([#2856])

- ✅ **Rename a book.**
- ✅ **Change the cover** with one from the files or the internet, and crop it ([#3828]). *(next release)*
- 💡 **Edit the author and other details** ([#1407]).
- 💡 **Covers that aren't square** ([#2856]).
- 💡 **Save the cover into the book's folder** ([#2303]).

### "Let me remove a book safely"

"Can we have an option to remove a book file/folder from the app interface without having to delete it from the
device?" ([#3397])

- ✅ **Deleting shows which files will go**, and asks you to tick "Delete my files" first ([#3828]). This came after
  [#1500], "Deleted all my audiobooks". *(next release)*
- 💡 **Hide a book without deleting its files** ([#3397]).

### "Show me how much I've listened"

[#2826], about 23 upvotes across 7 threads. "It would be cool to see how many hours has been spent listening" ([#2826])

- ✅ **Listening is now recorded**, "as groundwork for a yearly listening recap". The rating prompt shows hours, finished
  books and authors ([#3820]). *(next release, behind a flag)*
- ❌ **Turned down in 2015**: "I want to keep it simple so I am against an additional menu." ([#296])
- 💡 **Listening stats or a yearly recap** ([#2826]).

## Open questions

1. Why was sorting removed in 2018, and is searching instead still the answer to the biggest library request?
2. Series: from tags, from folders (the authors layout), or both?
3. Is "Continue listening" what most visits need? Analytics can tell.

[#296]: https://github.com/PaulWoitaschek/Voice/issues/296
[#571]: https://github.com/PaulWoitaschek/Voice/issues/571
[#765]: https://github.com/PaulWoitaschek/Voice/pull/765
[#834]: https://github.com/PaulWoitaschek/Voice/issues/834
[#906]: https://github.com/PaulWoitaschek/Voice/issues/906
[#1059]: https://github.com/PaulWoitaschek/Voice/pull/1059
[#1161]: https://github.com/PaulWoitaschek/Voice/discussions/1161
[#1407]: https://github.com/PaulWoitaschek/Voice/discussions/1407
[#1422]: https://github.com/PaulWoitaschek/Voice/discussions/1422
[#1500]: https://github.com/PaulWoitaschek/Voice/issues/1500
[#1537]: https://github.com/PaulWoitaschek/Voice/pull/1537
[#1543]: https://github.com/PaulWoitaschek/Voice/discussions/1543
[#1643]: https://github.com/PaulWoitaschek/Voice/issues/1643
[#1748]: https://github.com/PaulWoitaschek/Voice/discussions/1748
[#2303]: https://github.com/PaulWoitaschek/Voice/discussions/2303
[#2343]: https://github.com/PaulWoitaschek/Voice/discussions/2343
[#2826]: https://github.com/PaulWoitaschek/Voice/discussions/2826
[#2856]: https://github.com/PaulWoitaschek/Voice/discussions/2856
[#3170]: https://github.com/PaulWoitaschek/Voice/pull/3170
[#3235]: https://github.com/PaulWoitaschek/Voice/discussions/3235
[#3397]: https://github.com/PaulWoitaschek/Voice/discussions/3397
[#3788]: https://github.com/PaulWoitaschek/Voice/issues/3788
[#3792]: https://github.com/PaulWoitaschek/Voice/pull/3792
[#3817]: https://github.com/PaulWoitaschek/Voice/pull/3817
[#3820]: https://github.com/PaulWoitaschek/Voice/pull/3820
[#3828]: https://github.com/PaulWoitaschek/Voice/pull/3828
