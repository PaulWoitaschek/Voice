package de.ph1b.audiobook.playback.utils

import android.content.Context
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaDescriptionCompat
import d
import dagger.Reusable
import de.ph1b.audiobook.R
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.persistence.PrefsManager
import javax.inject.Inject

/**
 * Helper class for MediaBrowserServiceCompat handling
 *
 * @author Paul Woitaschek
 */
@Reusable class MediaBrowserHelper
@Inject constructor(private val bookUriConverter: BookUriConverter, private val repo: BookRepository, private val prefs: PrefsManager, private val context: Context) {

  fun onGetRoot(): MediaBrowserServiceCompat.BrowserRoot = MediaBrowserServiceCompat.BrowserRoot(bookUriConverter.allBooks().toString(), null)

  fun onLoadChildren(parentId: String, result: MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>>) {
    d { "onLoadChildren $parentId, $result" }
    val uri = Uri.parse(parentId)
    val items = mediaItems(uri)
    d { "sending result $items" }
    result.sendResult(items)
  }

  private fun mediaItems(uri: Uri): List<MediaBrowserCompat.MediaItem>? {
    val match = bookUriConverter.match(uri)

    if (match == BookUriConverter.ROOT) {
      val current = repo.bookById(prefs.currentBookId.value)?.let {
        MediaDescriptionCompat.Builder()
          .setTitle("${context.getString(R.string.current_book)}: ${it.name}")
          .setMediaId(bookUriConverter.book(it.id).toString())
          .build().let {
          MediaBrowserCompat.MediaItem(it, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
        }
      }

      val all = repo.activeBooks.map {
        val description = MediaDescriptionCompat.Builder()
          .setTitle(it.name)
          .setMediaId(bookUriConverter.book(it.id).toString())
          .build()
        return@map MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
      }

      if (current == null) {
        return all
      } else {
        return listOf(current) + all
      }
    } else {
      return null
    }
  }
}