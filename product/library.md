# Library

**Status:** Outline.

Listeners find the book they want in seconds, and can see at a glance how far along they are in each one.

**When:** Most visits are probably to continue the current book (**Guess**). Others are to pick the next one, or to
tidy up. Libraries range from one book to hundreds, and search works well when the library is small ([#571]).

| Problem | Status |
|---|---|
| [I have to look for the book I'm listening to](#i-have-to-look-for-the-book-im-listening-to) | ✅ Solved |
| [I can't see how far along I am in each book](#i-cant-see-how-far-along-i-am-in-each-book) | ✅ Solved |
| [I can't put my books in the order I want](#i-cant-put-my-books-in-the-order-i-want) | ⭕ Open |
| [I can't browse my books by author or series](#i-cant-browse-my-books-by-author-or-series) | ✅ Solved |
| [The books of a series aren't grouped or in order](#the-books-of-a-series-arent-grouped-or-in-order) | ⭕ Open |
| [Finding a book takes too long](#finding-a-book-takes-too-long) | ✅ Solved |
| [A book's name or cover is wrong](#a-books-name-or-cover-is-wrong) | ✅ Solved |
| [Fixing the author means editing the files](#fixing-the-author-means-editing-the-files) | ⭕ Open |
| [Covers that aren't square get cropped](#covers-that-arent-square-get-cropped) | ⭕ Open |
| [The cover I pick stays in Voice](#the-cover-i-pick-stays-in-voice) | ⭕ Open |
| [Deleting a book can delete more than I meant](#deleting-a-book-can-delete-more-than-i-meant) | ✅ Solved |
| [Removing a book from the library deletes its files](#removing-a-book-from-the-library-deletes-its-files) | ⭕ Open |
| [I can't see how much I've listened](#i-cant-see-how-much-ive-listened) | ⭕ Open |

## I have to look for the book I'm listening to

✅ **Solved** · [#906]

The older start screen split the books into "new" and "more", and that confused listeners.

**Solution:**

- "Continue listening" at the top, with a greeting. Nobody asked for these by name.
- Shelves for Current, Not started and Completed, and marking a book as one of them.

## I can't see how far along I am in each book

✅ **Solved** · [#1422]

Listeners want to see how far along they are in each book, right in the library. It had been requested for 5 years.

**Solution:** Progress on each book in the library. For the player, see [Player](player.md).

**Turned down:**

- No progress in the library, to keep the design minimal without another element in the UI ([#834]). That was in
  2020, and it was reversed later.

## I can't put my books in the order I want

⭕ **Open** · [#1543]

Listeners with many books by the same author have a hard time listening to them in the right order, like the order
they were written in. This is the biggest library topic. For one listener, it's the main reason to mostly use Smart
Audiobook Player instead ([#3235]). The maintainer suggested searching for the author instead.

**Turned down:**

- Sort options: added in 2018 ([#765]) and removed the next day. No reason was written down.

## I can't browse my books by author or series

✅ **Solved**

Listeners want to see the books of one author or one series together.

**Solution:**

- Folders are authors, a layout for adding books (see [Getting started](getting-started.md)).
- Search can browse by series, author, narrator and genre.

## The books of a series aren't grouped or in order

⭕ **Open** · [#1643]

The library doesn't treat a series as one thing. One listener called this a life-changing feature for those who
collect series ([#1748]). The maintainer wants real support for series in the long term.

## Finding a book takes too long

✅ **Solved**

**Solution:**

- Search, which also finds narrators, series and genres, and remembers recent searches.
- Grid or list.

## A book's name or cover is wrong

✅ **Solved**

**Solution:**

- Rename a book.
- Change the cover with one from the files or the internet, and crop it.

## Fixing the author means editing the files

⭕ **Open** · [#1407]

A wrong author or other detail can only be fixed in the files' tags, outside Voice ([#1161]). Voice reads the tags
again when the files change, and a name or cover given in Voice stays.

## Covers that aren't square get cropped

⭕ **Open** · [#2856]

Voice fits every cover into a square, often cutting off the author or title.

## The cover I pick stays in Voice

⭕ **Open** · [#2303]

A cover picked in Voice isn't saved with the book's files.

## Deleting a book can delete more than I meant

✅ **Solved** · [#1500]

Deleting one book once deleted a listener's whole audiobook folder.

**Solution:** Deleting shows which files will go, and asks you to tick "Delete my files" first.

## Removing a book from the library deletes its files

⭕ **Open** · [#3397]

Listeners want to take a book out of Voice and keep its files on the device.

## I can't see how much I've listened

⭕ **Open** · [#2826]

Listeners would like to see how many hours they've spent listening. Voice records listening, as groundwork for a yearly
listening recap ([#3820]). The rating prompt shows hours, finished books and authors, but there's no place to look it
up.

**Turned down:**

- A statistics menu: in 2015, to keep Voice simple without another menu ([#296]).

## Open questions

1. Why was sorting removed in 2018, and is searching instead still the answer to the biggest library request?
2. Series: from tags, from folders (the authors layout), or both?
3. Is "Continue listening" what most visits need? Analytics can tell.

[#296]: https://github.com/PaulWoitaschek/Voice/issues/296
[#571]: https://github.com/PaulWoitaschek/Voice/issues/571
[#765]: https://github.com/PaulWoitaschek/Voice/pull/765
[#834]: https://github.com/PaulWoitaschek/Voice/issues/834
[#906]: https://github.com/PaulWoitaschek/Voice/issues/906
[#1161]: https://github.com/PaulWoitaschek/Voice/discussions/1161
[#1407]: https://github.com/PaulWoitaschek/Voice/discussions/1407
[#1422]: https://github.com/PaulWoitaschek/Voice/discussions/1422
[#1500]: https://github.com/PaulWoitaschek/Voice/issues/1500
[#1543]: https://github.com/PaulWoitaschek/Voice/discussions/1543
[#1643]: https://github.com/PaulWoitaschek/Voice/issues/1643
[#1748]: https://github.com/PaulWoitaschek/Voice/discussions/1748
[#2303]: https://github.com/PaulWoitaschek/Voice/discussions/2303
[#2826]: https://github.com/PaulWoitaschek/Voice/discussions/2826
[#2856]: https://github.com/PaulWoitaschek/Voice/discussions/2856
[#3235]: https://github.com/PaulWoitaschek/Voice/discussions/3235
[#3397]: https://github.com/PaulWoitaschek/Voice/discussions/3397
[#3820]: https://github.com/PaulWoitaschek/Voice/pull/3820
