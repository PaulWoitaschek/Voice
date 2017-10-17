package de.ph1b.audiobook.features.bookmarks

import de.ph1b.audiobook.data.Bookmark
import de.ph1b.audiobook.data.Chapter

/**
 * View of the bookmarks
 */
interface BookmarkView {

  fun render(bookmarks: List<Bookmark>, chapters: List<Chapter>)
  fun showBookmarkAdded(bookmark: Bookmark)
  fun finish()
}
