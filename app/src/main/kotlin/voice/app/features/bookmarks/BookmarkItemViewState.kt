package voice.app.features.bookmarks

import voice.data.Bookmark

data class BookmarkItemViewState(
  val title: String,
  val subtitle: String,
  val id: Bookmark.Id,
)

data class BookmarkViewState(
  val bookmarks: List<BookmarkItemViewState>,
  val shouldScrollTo: Bookmark.Id?,
)
