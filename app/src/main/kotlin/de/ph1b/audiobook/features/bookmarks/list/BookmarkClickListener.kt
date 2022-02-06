package de.ph1b.audiobook.features.bookmarks.list

import de.ph1b.audiobook.data.Bookmark2

interface BookmarkClickListener {

  fun onOptionsMenuClicked(bookmark: Bookmark2, v: android.view.View)
  fun onBookmarkClicked(bookmark: Bookmark2)
}
