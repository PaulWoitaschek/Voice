package de.ph1b.audiobook.features.bookmarks.list

interface BookmarkClickListener {

  fun onOptionsMenuClicked(bookmark: de.ph1b.audiobook.Bookmark, v: android.view.View)
  fun onBookmarkClicked(bookmark: de.ph1b.audiobook.Bookmark)
}
