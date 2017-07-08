package de.ph1b.audiobook.playback

import android.net.Uri
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import dagger.Lazy
import de.ph1b.audiobook.features.bookSearch.BookSearchHandler
import de.ph1b.audiobook.features.bookSearch.BookSearchParser
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.utils.BookUriConverter
import e
import i
import javax.inject.Inject

/**
 * Media session callback that handles playback controls.
 *
 * @author Paul Woitaschek
 */
class MediaSessionCallback @Inject constructor(
    private val bookUriConverter: BookUriConverter,
    private val prefs: PrefsManager,
    private val bookSearchHandler: BookSearchHandler,
    private val autoConnection: Lazy<AndroidAutoConnection>,
    private val player: MediaPlayer,
    private val bookSearchParser: BookSearchParser
) : MediaSessionCompat.Callback() {

  override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
    i { "onPlayFromMediaId $mediaId" }
    val uri = Uri.parse(mediaId)
    val type = bookUriConverter.match(uri)
    if (type == BookUriConverter.BOOK_ID) {
      val id = bookUriConverter.extractBook(uri)
      prefs.currentBookId.value = id
      onPlay()
    } else {
      e { "Invalid mediaId $mediaId" }
    }
  }

  override fun onPlayFromSearch(query: String?, extras: Bundle?) {
    i { "onPlayFromSearch $query" }
    val bookSearch = bookSearchParser.parse(query, extras)
    bookSearchHandler.handle(bookSearch)
  }

  override fun onSkipToNext() {
    i { "onSkipToNext" }
    if (autoConnection.get().connected) {
      player.next()
    } else {
      onFastForward()
    }
  }

  override fun onRewind() {
    i { "onRewind" }
    player.skip(MediaPlayer.Direction.BACKWARD)
  }

  override fun onSkipToPrevious() {
    i { "onSkipToPrevious" }
    if (autoConnection.get().connected) {
      player.previous(toNullOfNewTrack = true)
    } else {
      onRewind()
    }
  }

  override fun onFastForward() {
    i { "onFastForward" }
    player.skip(MediaPlayer.Direction.FORWARD)
  }

  override fun onStop() {
    i { "onStop" }
    player.stop()
  }

  override fun onPause() {
    i { "onPause" }
    player.pause(true)
  }

  override fun onPlay() {
    i { "onPlay" }
    player.play()
  }

  override fun onCustomAction(action: String?, extras: Bundle?) {
    i { "onCustomAction $action" }
    when (action) {
      ANDROID_AUTO_ACTION_NEXT -> onSkipToNext()
      ANDROID_AUTO_ACTION_PREVIOUS -> onSkipToPrevious()
      ANDROID_AUTO_ACTION_FAST_FORWARD -> onFastForward()
      ANDROID_AUTO_ACTION_REWIND -> onRewind()
    }
  }
}
