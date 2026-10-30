# Plan: editing a book's series and part

Step 3 of the [library spec](../library.md#7-editing-title-author-and-series), sketch in
[edit-book.png](../sketches/edit-book.png). Listeners edit the series and part of a book in the edit sheet. The
series field suggests the series the library already has, edits sync through `.voice`, and for books from
Audiobookshelf the series is written to the server. Everything is behind the `edit_series` feature flag
(`EditSeriesFeatureFlag`, off by default).

This file is for the sessions that build it. Each pull request updates its row and notes what it did differently
from the plan. It goes once all four are merged, so sessions working on review feedback still find it.

To start the next step: "Read `docs/plans/series-editing.md` and build PR N."

## Status

| PR | What | Status |
|----|------|--------|
| 1 | Edit series and part of books on the device | In review: [#4081](https://github.com/PaulWoitaschek/Voice/pull/4081), branch `edit-book-series` |
| 2 | Suggest series the library already has | In review: [#4082](https://github.com/PaulWoitaschek/Voice/pull/4082), branch `series-suggestions` |
| 3 | Sync series and part through `.voice` | In review: [#4083](https://github.com/PaulWoitaschek/Voice/pull/4083), branch `sync-series` |
| 4 | Write the series of Audiobookshelf books to the server | In review: [#4084](https://github.com/PaulWoitaschek/Voice/pull/4084), branch `audiobookshelf-series` |

The pull requests are stacked: each branches off the one before and targets its branch, so each diff shows one step.
After a merge, the next one is retargeted to `main`.

## 1. Edit series and part of books on the device

- `CustomMetadata` has `series` and `part`. `followingFiles()` drops them when they equal the files' values, like
  the name and author. Database 69 adds the columns with an auto migration.
- `Book.series` and `Book.part` merge the edit with the files. Search reads these.
- `EditBookViewModel` and `EditBookSheet` show a Series field and a narrow Part field below the author when the flag
  is on, and save only what changed. An empty field goes back to the files, like the author.
- Books from Audiobookshelf still have no Edit item.
- Done differently: from 1.5× font size, Part goes below Series, and its width grows with the font, so its label
  stays on one line in every language.

## 2. Suggest series the library already has

All in `:features:bookOverview`.

- When the sheet opens with the flag on, `EditBookViewModel` loads the library's series once from `repo.all()`. It
  groups `book.series` by `lowercase()`, takes the most common spelling as the name and counts the books, like
  `BookSearchViewModel.browse()`.
- `EditBookState` gets `allSeries: List<SeriesSuggestion(name, bookCount)>` and derives `suggestions`: at most 3
  series whose name contains the typed text and isn't equal to it, those starting with it first. Nothing shows before
  something is typed.
- Done differently: the edited book itself isn't counted, so a typo in its own series isn't suggested back. The
  preview shows the list on its own, since it only appears while the field has focus.
- `EditBookSheet` shows them below the series field while it has focus, inline in the column in a rounded
  `surfaceContainerHigh` container, not as a popup, so it works with the keyboard inside the sheet. Each row is the
  name in `.bookText()` with "4 books" below. A tap takes the name and moves focus to Part.
- For "4 books", reuse the plural `library.search.browse.book_count` if it fits.
- Add a preview with suggestions. Tests in `EditBookViewModelTest` for grouping, counting and filtering.

## 3. Sync series and part through `.voice`

All in `:core:foldersync`, the same as the author in #4053.

- `SyncedBook` gets `series: SyncedText? = null` and `part: SyncedText? = null`. Older versions skip them through
  `ignoreUnknownKeys`.
- `SyncedBooks.kt`: `syncedText(played.custom.series, files.series, own?.series, …)` in `toSyncedBook()`, the same for
  `part`, and two lines each in `withoutFilesNames()` and `with()`.
- `FolderSync.takeOver()`: `updateCustomMetadata { it.copy(series = …) }` and the same for `part`.
- Tests: `SyncedBooksTest`, `FolderSyncTest` (the author tests are the template), `LibraryFiles.book(…)` gets
  `series` and `part`. Update `website/src/pages/faq.md` where it lists what syncs.
- Done differently: `takeOver()` uses one small helper for the four edited texts instead of a block each. The FAQ
  still lists titles and authors, since listeners can't edit the series before the flag goes away. Whoever removes
  the flag adds the series there.

## 4. Write the series of Audiobookshelf books to the server

How the server handles it (`LibraryItemController.updateMedia`, `Book.updateSeriesFromRequest`):

- `PATCH api/items/{id}/media` with `{"metadata":{"series":[{"name":…,"sequence":…}]}}`.
- It matches series by name, ignoring case, creates unknown ones, and removes the book from every series not in the
  request.
- A user without the update permission gets 403. The response is `{updated, libraryItem}`.

In `:core:audiobookshelf`:

- `AudiobookshelfApi.updateMedia(itemId, MediaUpdate)`, returning `Response<Unit>`, with
  `MediaUpdate(metadata = MetadataUpdate(series: List<AbsSeries>))`.
- A new `internal class SeriesUpdate` in `sync/`, with the http client and account as in `ProgressSync.send`:
  1. Fetch the item with `items(BatchGetRequest(listOf(itemId)))` to get all its series. Voice shows only the first,
     the others must survive.
  2. Replace the first series with the edited one and keep the rest. An empty name removes the first series. Another
     series with the edited name is dropped.
  3. On success, `bookRepository.updateBook(id)` sets `metadata.series` and `part`, so the change shows right away.
     The server bumps `updatedAt`, so the next library sync reads the same.
  4. Returns `ServerEdit.Saved`, `NotAllowed` (403) or `Failed` (`IOException`, `HttpException`, other codes). Log only
     the HTTP code.
- `ServerLibrary.updateSeries(bookId, series, part): ServerEdit`, implemented in `AudiobookshelfServerLibrary`.
- `SeriesUpdateTest` with `MockWebServer`, like `ProgressSyncTest`: path and body, other series kept, 403, local book
  updated.

In `:features:bookOverview`:

- `EditBookViewModel.items()` offers Edit for books on the server only with the flag on.
- `EditBookState.remote` shows only series and part, since the title and author come from the server. Focus starts in
  Series.
- Saving a remote book doesn't slide the sheet away first. It sets `saving`, which shows a `LoadingIndicator` in the
  button and makes the fields read-only. `Saved` sets `saved`, and a `LaunchedEffect` slides the sheet away. On an
  error, `error` shows as text in the error color above the buttons, and Save works again.
- Strings: "Couldn't save on your server. Try again." and "Your account on the server can't change books."
- Answer the open question in `docs/library.md` about editing books from Audiobookshelf, and update
  `website/src/pages/audiobookshelf.md`, which says names are edited on the server.
- Done differently:
  - The server puts a series it just got last. So the library sync keeps showing the series Voice showed while the
    server still has it, and `SeriesUpdate` replaces that one, not the first.
  - The sheet has one `serverResult: ServerEdit?` instead of `saved` and `error`.
  - The website page stays as it is, like the FAQ in PR 3, until the flag goes away.

## Left out

- A series from the tags can't be removed from a book on the device, only renamed. An empty field means "as in the
  files", like the author.
- Author suggestions, "Use the names from the files", and suggesting the next part.
- The series shows nowhere but search. Series stacks are step 4 of the spec.
- No retry queue when the server can't be reached. The sheet shows the error.
