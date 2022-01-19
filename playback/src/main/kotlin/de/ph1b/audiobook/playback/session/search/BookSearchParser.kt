package de.ph1b.audiobook.playback.session.search

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import dagger.Reusable
import javax.inject.Inject

@Reusable
class BookSearchParser @Inject constructor() {

  fun parse(query: String?, extras: Bundle?): BookSearch {
    return BookSearch(query = query,
      mediaFocus = extras?.getString(MediaStore.EXTRA_MEDIA_FOCUS),
      album = extras?.getString(MediaStore.EXTRA_MEDIA_ALBUM),
      artist = extras?.getString(MediaStore.EXTRA_MEDIA_ARTIST))
  }

  fun parse(intent: Intent?): BookSearch? {
    return if (intent?.action == MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) {
      val query: String? = intent.getStringExtra(SearchManager.QUERY)
      val extras: Bundle? = intent.extras
      parse(query, extras)
    } else null
  }
}
