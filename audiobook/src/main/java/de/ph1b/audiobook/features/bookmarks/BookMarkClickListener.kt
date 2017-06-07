package de.ph1b.audiobook.features.bookmarks

import android.view.View
import de.ph1b.audiobook.Bookmark

interface BookMarkClickListener {

  fun onOptionsMenuClicked(bookmark: Bookmark, v: View)
  fun onBookmarkClicked(bookmark: Bookmark)
}
