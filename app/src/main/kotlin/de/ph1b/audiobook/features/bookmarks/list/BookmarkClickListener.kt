package de.ph1b.audiobook.features.bookmarks.list

import de.ph1b.audiobook.data.Bookmark

interface BookmarkClickListener {

  fun onOptionsMenuClicked(bookmark: Bookmark, v: android.view.View)
  fun onBookmarkClicked(bookmark: Bookmark)
}
