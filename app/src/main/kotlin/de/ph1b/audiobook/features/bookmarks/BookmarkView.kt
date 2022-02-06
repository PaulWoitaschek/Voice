package de.ph1b.audiobook.features.bookmarks

import de.ph1b.audiobook.data.Bookmark2
import de.ph1b.audiobook.data.Chapter2

/**
 * View of the bookmarks
 */
interface BookmarkView {

  fun render(bookmarks: List<Bookmark2>, chapters: List<Chapter2>)
  fun showBookmarkAdded(bookmark: Bookmark2)
  fun finish()
}
