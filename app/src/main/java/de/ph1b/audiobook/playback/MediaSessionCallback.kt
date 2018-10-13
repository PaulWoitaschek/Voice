package de.ph1b.audiobook.playback

import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import de.ph1b.audiobook.features.bookSearch.BookSearchHandler
import de.ph1b.audiobook.features.bookSearch.BookSearchParser
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.utils.BookUriConverter
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

/**
 * Media session callback that handles playback controls.
 */
class MediaSessionCallback @Inject constructor(
  private val bookUriConverter: BookUriConverter,
  @Named(PrefKeys.CURRENT_BOOK)
  private val currentBookIdPref: Pref<UUID>,
  private val bookSearchHandler: BookSearchHandler,
  private val autoConnection: AndroidAutoConnectedReceiver,
  private val player: MediaPlayer,
  private val bookSearchParser: BookSearchParser
) : MediaSessionCompat.Callback() {

  override fun onSkipToQueueItem(id: Long) {
    Timber.i("onSkipToQueueItem $id")
    val chapter = player.bookContent
      ?.chapters?.getOrNull(id.toInt()) ?: return
    player.changePosition(time = 0, changedFile = chapter.file)
    player.play()
  }

  override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
    Timber.i("onPlayFromMediaId $mediaId")
    mediaId ?: return
    val parsed = bookUriConverter.parse(mediaId)
    if (parsed is BookUriConverter.Parsed.Book) {
      currentBookIdPref.value = parsed.id
      onPlay()
    } else {
      Timber.e("Didn't handle $parsed")
    }
  }

  override fun onPlayFromSearch(query: String?, extras: Bundle?) {
    Timber.i("onPlayFromSearch $query")
    val bookSearch = bookSearchParser.parse(query, extras)
    bookSearchHandler.handle(bookSearch)
  }

  override fun onSkipToNext() {
    Timber.i("onSkipToNext")
    if (autoConnection.connected) {
      player.next()
    } else {
      onFastForward()
    }
  }

  override fun onRewind() {
    Timber.i("onRewind")
    player.skip(forward = false)
  }

  override fun onSkipToPrevious() {
    Timber.i("onSkipToPrevious")
    if (autoConnection.connected) {
      player.previous(toNullOfNewTrack = true)
    } else {
      onRewind()
    }
  }

  override fun onFastForward() {
    Timber.i("onFastForward")
    player.skip(forward = true)
  }

  override fun onStop() {
    Timber.i("onStop")
    player.stop()
  }

  override fun onPause() {
    Timber.i("onPause")
    player.pause(true)
  }

  override fun onPlay() {
    Timber.i("onPlay")
    player.play()
  }

  override fun onCustomAction(action: String?, extras: Bundle?) {
    Timber.i("onCustomAction $action")
    when (action) {
      ANDROID_AUTO_ACTION_NEXT -> onSkipToNext()
      ANDROID_AUTO_ACTION_PREVIOUS -> onSkipToPrevious()
      ANDROID_AUTO_ACTION_FAST_FORWARD -> onFastForward()
      ANDROID_AUTO_ACTION_REWIND -> onRewind()
    }
  }
}
