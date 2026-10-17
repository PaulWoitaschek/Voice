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
  ([#2343]).

[#1407]: https://github.com/PaulWoitaschek/Voice/discussions/1407
[#1543]: https://github.com/PaulWoitaschek/Voice/discussions/1543
[#1748]: https://github.com/PaulWoitaschek/Voice/discussions/1748
[#2145]: https://github.com/PaulWoitaschek/Voice/discussions/2145
[#2343]: https://github.com/PaulWoitaschek/Voice/discussions/2343
[#2909]: https://github.com/PaulWoitaschek/Voice/discussions/2909
[#3166]: https://github.com/PaulWoitaschek/Voice/discussions/3166
[#3235]: https://github.com/PaulWoitaschek/Voice/discussions/3235
[#3313]: https://github.com/PaulWoitaschek/Voice/discussions/3313
[#3397]: https://github.com/PaulWoitaschek/Voice/discussions/3397
[#3725]: https://github.com/PaulWoitaschek/Voice/discussions/3725

## What Voice knows today

- Every book has a title, author, narrator, genre, series and part. Search uses all of them and can browse by each. The
  library only shows the title and the author.
- Series and part come from Audiobookshelf and from the `MVNM` and `MVIN` tags of MP3 files. M4B files most likely lose
  them, because media3 hands these tags over under a different id. Folder names never become an author or a series,
  not even with "Folders are authors".
- The author is the artist tag. It often holds the narrator, several names, or one person spelled two ways.
- Only the title and the cover can be changed, and only for books on the device. The title syncs through `.voice`.
  Voice never writes tags.
- Current, Not started yet and Completed follow from the position, each as a list or a grid. Current and Completed are
  in the order they were last listened, Not started yet by title. There's no sorting, filtering or hiding.

**So: group by series, not by author.** A series is short, ordered by its parts, and comes from Audiobookshelf or the
tags. Authors are messy, most of them have a single book, and a group per author makes the list longer, not shorter.
Authors get their own screen instead, and are for search, sorting and editing.

## Ground rules

- The library stays one calm screen. Nothing new to learn for a listener with ten books.
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
- The book in Continue listening shows on its own, with its series and part. Its series shows nowhere else.
- Any other series is in Current once one of its books is started, also between two books. It's in Not started yet
  before that, and in Completed once all are finished.
- A tap on a stack opens the series.

## 2. The series

![Sketch: the series screen](sketches/series.png)

- The cover stack, the name of the series, its author and how many books are finished.
- One button continues the book in progress, or starts the first book that isn't finished.
- Books in the order of their parts. Books without a part come after them, by title, with 2 before 10.
- More options: rename the series for all its books, hide the series.
- Opens from a stack, from a book's series in the player and the book actions, and from Browse in search, which today
  only fills in the search field.

## 3. The author

![Sketch: the author screen](sketches/author.png)

- The author's name, how many series and books they have, and how many are finished.
- Their series first, as stacks, then their other books, each by title.
- More options: rename the author for all their books, which joins two spellings of one name.
- Opens from a book's author in the player, the book actions and the series, and from Browse in search.

## 4. Sorting and filtering

![Sketch: the sort and show sheet](sketches/library-sort-sheet.png)

- **Sort by:** Last listened, Title, Author, Recently added. Last listened is the default and is what the library does
  today, because books that weren't started fall back to their title. The order applies to Not started yet and
  Completed. Current stays in the order it was listened in. Author sorts by author, then by the title or the series'
  name.
- **Show:** finished books, hidden books.
- **From:** one chip per audiobook folder and Audiobookshelf library, only with more than one.
- A dot on the button says that not everything is shown.
- Filtering by author, narrator or genre stays in search, which already does it.

## 5. Hiding books

![Sketch: hide from library in the book actions](sketches/book-actions-hide.png)

- Hide from library in the book actions, and for a series on the series screen. A snackbar offers to undo it.
- Hidden books leave the library, Continue listening, the Shelf widget and Android Auto. Search still finds them,
  marked as hidden.
- Hidden books in the sort sheet shows them again, and the book actions unhide them.
- Progress, bookmarks and files stay, and a scan doesn't bring the book back. This also solves removing a book without
  deleting its files ([#3397]).
- Hiding syncs through `.voice`, so a book hidden on the phone is hidden on the tablet too.

## 6. Editing title, author and series

![Sketch: the edit sheet](sketches/edit-book.png)

- Edit replaces Rename, with title, author, series and part.
- The author and series fields suggest the names in the library, so one name isn't there twice with two spellings.
- "Use the names from the files" undoes all edits.
- Edits override the files, stay in Voice and sync through `.voice` like the title does today.
- Books from Audiobookshelf are edited on the server, like their title and cover today. The server has better tools
  for it and keeps every device in step.
- Voice reads series and part from M4B files too, so fewer books need editing.

## 7. The next book in a series

![Sketch: the next book of the series, in the player and the library](sketches/next-book.png)

- When a book of a series ends, the player offers the next one: the first unfinished book after it, by part.
- Continue listening in the library offers it too, until another book is started.

## Won't solve

- **Author groups in the library:** tags are too messy for it, and most authors have a single book. The author screen
  shows an author's books in one place.
- **Own collections or shelves** ([#2343]): every list is something the listener has to keep up. Series, hiding and
  search cover most of what's asked for.
- **Writing tags into the files:** it could break files, and synced folders would change on every device.
- **Putting books in any order by hand** ([#1543]): sorting and the order of a series cover what listeners describe.
- **Sorting by file name** ([#3235]): editing the title or the series fixes the order for good.

## Left out for now

- Playing the next book on its own. A book that ends while the listener falls asleep would run into the next one.
- Android Auto and the widgets don't offer the next book of a series.
- Books from Audiobookshelf have no `.voice` folder, so they stay hidden only on the device they were hidden on.
- Two series with the same name from different authors become one.
- A book in several series only shows in the first one, like today.
- An author tag with several names gets one author screen for all of them, not one per name.

## Order of work

Each step helps on its own.

1. Read series and part from M4B files.
2. The edit sheet with author, series and part.
3. Series stacks in the library and the series screen.
4. Offering the next book of a series.
5. The author screen.
6. Hiding books.
7. The sort sheet.

## Open questions

- Should books from Audiobookshelf be editable in Voice after all?
