# Library spec (draft)

Listeners with hundreds of books find the next one in seconds, follow their series in order, and keep what they don't
want to see right now out of the way.

This is a proposal. Once something ships, it moves into the [product spec](product.md) and leaves this page.

## Problems

- **The books of a series are scattered and out of order.** [#1748], [#3313], [#3725]
- **I can't put my books in the order I want.** [#1543], [#3235], [#2909]
- **Fixing the author or the series means editing the files.** [#1407], [#2145]
- **I can't take a book out of my library without deleting its files.** [#3397], [#3166]
- **My library is one long list.** Audiobookshelf libraries with thousands of books, the kids' books next to mine
  ([#2343], [#1214]).
- **The episodes of a radio play all look the same, and search can't find one by its number.** [#3132], [#1643]
- **My author folders don't make authors, and a series folder becomes one long book.** [#1091], [#3344], [#3749]

[#1091]: https://github.com/PaulWoitaschek/Voice/issues/1091
[#1214]: https://github.com/PaulWoitaschek/Voice/discussions/1214
[#1407]: https://github.com/PaulWoitaschek/Voice/discussions/1407
[#1543]: https://github.com/PaulWoitaschek/Voice/discussions/1543
[#1643]: https://github.com/PaulWoitaschek/Voice/issues/1643
[#1748]: https://github.com/PaulWoitaschek/Voice/discussions/1748
[#2145]: https://github.com/PaulWoitaschek/Voice/discussions/2145
[#2343]: https://github.com/PaulWoitaschek/Voice/discussions/2343
[#2909]: https://github.com/PaulWoitaschek/Voice/discussions/2909
[#3132]: https://github.com/PaulWoitaschek/Voice/issues/3132
[#3166]: https://github.com/PaulWoitaschek/Voice/discussions/3166
[#3235]: https://github.com/PaulWoitaschek/Voice/discussions/3235
[#3313]: https://github.com/PaulWoitaschek/Voice/discussions/3313
[#3344]: https://github.com/PaulWoitaschek/Voice/issues/3344
[#3397]: https://github.com/PaulWoitaschek/Voice/discussions/3397
[#3725]: https://github.com/PaulWoitaschek/Voice/discussions/3725
[#3749]: https://github.com/PaulWoitaschek/Voice/discussions/3749

## What Voice knows today

- Every book has a title, author, narrator, genre, series and part. The library only shows the title and the author.
- Search looks through all of them, but only lists books. Browse fills in the search field, so "Stephen King" also
  finds "Stephen Kingsley".
- Series and part come from Audiobookshelf and from the `MVNM` and `MVIN` tags of MP3 files. M4B files lose them,
  because media3 hands these tags over under a different id.
- A book of one file is named after its album first. 160 episodes with the album "The Three Investigators" all get
  that name, and their own titles are neither shown nor searched.
- The author is the artist tag. It often holds the narrator, several names, `<Unknown>`, or one person spelled two
  ways.
- Folder names never become an author or a series, not even with "Folders are authors". In that mode, a series folder
  in an author's folder becomes one long book.
- Only the title and the cover can be changed, and only for books on the device. The title syncs through `.voice`.
  Voice never writes tags.
- Current, Not started yet and Completed follow from the position, each as a list or a grid. Current and Completed are
  in the order they were last listened, Not started yet by title. There's no sorting, filtering or hiding.

**So: group by series, not by author.** A series is short, ordered by its parts, and comes from Audiobookshelf or the
tags. Authors are messy, most of them have a single book, and a group per author makes the list longer, not shorter.
Authors get their own screen instead, and are for search, sorting and editing.

**But most series aren't in the tags.** They're in the album or in the folders, so Voice reads them from there, and
fixing many books at once has to take a few taps.

## Ground rules

- The library stays one calm screen. Nothing new to learn for a listener with ten books.
- It stays quick with 5,000 books and a series of 200 episodes.
- No book gets lost. Search always finds grouped and hidden books.
- Edits never touch the files or the server.
- Grouping series needs no setting.

## 1. Series in the library

![Sketch: series as stacks, in the list and in the grid](sketches/library-series-stacks.png)

- Two or more books of a series show as one stack, in the list and in the grid. A single book of a series shows as a
  book.
- The stack has the series' name, its author and the cover of the book that's next. In the list, the line under the
  author says how many books there are, or how many are finished. In the grid, a badge on the cover counts the books.
- The bar of a started series shows how much of the whole series is listened.
- A book in progress always shows on its own, with its series and part. Two books of one series in progress are both
  in Current, and their series has no stack.
- A series with no book in progress is a stack: in Not started yet before its first book, in Current between two
  books, and in Completed after its last.
- A tap on a stack opens the series. A long press renames or hides it, like the series' menu.

## 2. The series

![Sketch: the series screen](sketches/series.png)

- The cover stack, the name of the series, its author and how many books are finished.
- One button continues the book in progress, or starts the first book that isn't finished.
- Books in the order of their parts, each with its own cover. Books without a part come after them, in the order of
  their folder or file names, with 2 before 10. That's where most libraries keep the number, like
  "Book 4 - The Ruin.m4b" or "Folge 063".
- More options: rename the series for all its books, hide the series. Renaming suggests the series in the library, and
  picking one joins the two.
- Opens from a stack, from a book's series in the player and the book actions, and from search.

## 3. The author

![Sketch: the author screen](sketches/author.png)

- The author's name, how many series and books they have, and how many are finished.
- Their series first, as stacks, then their other books, each by title.
- Names that only differ in case, spaces or punctuation are one author, like "jk Rowling" and "J. K. Rowling". The
  screen uses the spelling most of their books have. Nothing is written, so this works for Audiobookshelf books too.
  Series names match the same way.
- More options: rename the author for all their books. Renaming suggests the authors in the library, and picking one
  joins the two, like ten books of "Rowling, J. K." and ten of "J. K. Rowling".
- Opens from a book's author in the player, the book actions and the series, and from search.

## 4. Where series and authors come from

- An edit in Voice comes first. Then the folders, with "Folders are authors". Then Audiobookshelf and the tags, and
  last the album.
- **Tags:** `MVNM` and `MVIN` in MP3 and M4B files, and the `SERIES` and `SERIES-PART` tags that Audiobookshelf reads
  too.
- **Folders:** with "Folders are authors", the folder is the author. A folder of book folders in it is a series, so
  `Author/Series/Book` works too. Folders named like "CD 1" or "Disc 2" stay part of their book. The long book such a
  series folder makes today becomes its books, and its position and bookmarks move along, like when the layout
  changes.
- **Album:** books of one file in the same folder that share an album are a series named after the album. Each is
  named after its own title, or its file name without one. 160 episodes of "The Three Investigators" become one stack
  of 160 episodes with their own names.
- **Authors:** the artist tag, or the album artist when there's none. "Unknown" and `<Unknown>` count as no author. A
  book without an author has no author screen and comes last when sorted by author.

## 5. Sorting and filtering

![Sketch: the sort and show sheet](sketches/library-sort-sheet.png)

- **Sort by:** Last listened, Title, Author, Recently added. Last listened is the default and is what the library does
  today, because books that weren't started fall back to their title. The order applies to Not started yet and
  Completed. Current stays in the order it was listened in. Author sorts by author, then by the title or the series'
  name.
- A stack sorts by its last listened book, the series' name, its author or its newest book. Inside a stack, the order
  of the series always holds.
- **Show:** finished books, hidden books.
- **From:** one chip per audiobook folder and Audiobookshelf library, only with more than one.
- A dot on the button says that not everything is shown.
- Filtering by author, narrator or genre stays in search, which already does it.

## 6. Hiding books

![Sketch: hide from library in the book actions](sketches/book-actions-hide.png)

- Hide from library in the book actions, for a series on its stack or the series screen, and for selected books. A
  snackbar offers to undo it.
- Hidden books leave the library, Continue listening, the Shelf widget and Android Auto. Search still finds them,
  marked as hidden.
- Hidden books in the sort sheet shows them again, and the book actions unhide them.
- Progress, bookmarks and files stay, and a scan doesn't bring the book back. This also solves removing a book without
  deleting its files ([#3397]).
- Hiding syncs through `.voice`, so a book hidden on the phone is hidden on the tablet too.

## 7. Editing title, author and series

![Sketch: the edit sheet](sketches/edit-book.png)

- Edit replaces Rename, with title, author, series and part.
- The author and series fields suggest the names in the library, so one name isn't there twice with two spellings.
- "Use the names from the files" undoes all edits.
- Edits override the files, stay in Voice and sync through `.voice` like the title does today.
- A scan keeps the edits. What wasn't edited still follows the files, so a fixed tag shows up in Voice.
- Books from Audiobookshelf are edited on the server, like their title and cover today. The server has better tools
  for it and keeps every device in step.

### Several books at once

![Sketch: selecting books to change them together](sketches/library-select.png)

- Select in the book actions starts selecting, because a long press already opens them. A tap adds more books, and
  Select all takes everything shown.
- Selecting works in the library, on a series, on an author and in search results. To fix a radio play, search for
  it, select all and set the series.
- Series, Author and Hide change all selected books. Books put in a series follow their folder or file names. Series
  and Author leave out books from Audiobookshelf.
- This is for books with nothing in common in their tags, like 40 episodes of a radio play without a series tag. One
  wrong name on many books is a single rename on the author or series screen.

## 8. The next book in a series

![Sketch: the next book of the series, in the player and the library](sketches/next-book.png)

- When a book of a series ends, the player offers the next one: the first unfinished book after it in the series.
  Another edition of the same part is skipped.
- Finished books are skipped too, so a book read on paper only needs to be marked Completed.
- Continue listening in the library offers it too, until another book is started.

## 9. Search

![Sketch: search results with an author and series](sketches/search.png)

- The best match stays on top and plays in one tap. Authors and series that match come right below it, and open their
  screen.
- Books are still found one by one, also those in a series and hidden ones. Hidden books are marked.
- Browse opens the author's or the series' screen. Narrators and genres match the whole name, so "Stephen King" no
  longer finds "Stephen Kingsley".
- A series and a part work in one search, so "signal saga 3" puts Interference first. Numbers match without leading
  zeros, and episodes are named after their own title, so "63" finds "Folge 063".
- Asking Android Auto or the Assistant to play something uses the same search. "Play The Signal Saga" plays the book
  that's next.

## Won't solve

- **Author groups in the library:** tags are too messy for it, and most authors have a single book. The author screen
  shows an author's books in one place.
- **Own collections or shelves** ([#2343], [#1214]): every list is something the listener has to keep up. Folders
  already work as shelves: each one added to Voice is a chip in From, like one for the kids.
- **Writing tags into the files:** it could break files, and synced folders would change on every device.
- **Putting books in any order by hand** ([#1543]): sorting and the order of a series cover what listeners describe.
- **Sorting the whole library by file name** ([#3235]): books in a series already follow their folder or file names,
  and editing fixes the rest.

## Left out for now

- Playing the next book on its own. A book that ends while the listener falls asleep would run into the next one.
- Android Auto still lists all books in one list, and neither it nor the widgets offer the next book of a series.
- Books from Audiobookshelf have no `.voice` folder, so they stay hidden only on the device they were hidden on.
- Two series with the same name from different authors become one.
- A book in several series only shows in the first one, like today.
- An author tag with several names gets one author screen for all of them, not one per name.
- Names in another order, like "Rowling, J. K.", aren't matched on their own. One rename joins them.
- A narrator in the artist tag stays the author. Selecting their books and setting the author fixes it.
- Folders only become series with "Folders are authors". A series folder with the book files right in it stays one
  book, and disc folders not named like "CD 1" or "Disc 2" become books of their own.
- Search doesn't look through chapter names or file names.

## Order of work

Each step helps on its own.

1. Read series and part from M4B files and the `SERIES` tags.
2. Series from albums, and authors and series from folders with "Folders are authors".
3. The edit sheet with author, series and part.
4. Series stacks in the library and the series screen.
5. Offering the next book of a series.
6. The author screen.
7. Authors and series in search, Browse opening their screen, and numbers in search.
8. Hiding books.
9. The sort sheet.
10. Selecting several books.
11. Android Auto and the Assistant using the library search.

## Open questions

- Should books from Audiobookshelf be editable in Voice after all?
- Are folders enough as shelves ([#1214]), or do listeners need shelves they make in Voice?
