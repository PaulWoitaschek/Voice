package de.ph1b.audiobook.features.bookmarks

import de.ph1b.audiobook.Bookmark
import de.ph1b.audiobook.Chapter

/**
 * View of the bookmarks
 *
 * @author Paul Woitaschek
 */
interface BookmarkView {

  fun init(chapters: List<Chapter>)
  fun render(bookmarks: List<Bookmark>)
  fun showBookmarkAdded(bookmark: Bookmark)
  fun finish()
}
