package de.ph1b.audiobook.features.bookSearch

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import dagger.Reusable
import javax.inject.Inject

@Reusable
class BookSearchParser @Inject constructor() {

  fun parse(query: String?, extras: Bundle?): BookSearch {
    val mediaFocus = extras?.getString(MediaStore.EXTRA_MEDIA_FOCUS)
    val album = extras?.getString(MediaStore.EXTRA_MEDIA_ALBUM)
    val artist = extras?.getString(MediaStore.EXTRA_MEDIA_ARTIST)
    val playlist = extras?.getString("android.intent.extra.playlist")
    return BookSearch(query, mediaFocus, album, artist, playlist)
  }

  fun parse(intent: Intent?): BookSearch? {
    return if (intent?.action == MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) {
      val query: String? = intent.getStringExtra(SearchManager.QUERY)
      val extras: Bundle? = intent.extras
      parse(query, extras)
    } else null
  }
}
