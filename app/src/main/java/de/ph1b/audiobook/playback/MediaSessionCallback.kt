package de.ph1b.audiobook.playback

import android.net.Uri
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import de.ph1b.audiobook.features.bookPlaying.chaptersAsBookPlayChapters
import de.ph1b.audiobook.features.bookSearch.BookSearchHandler
import de.ph1b.audiobook.features.bookSearch.BookSearchParser
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.utils.BookUriConverter
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

/**
 * Media session callback that handles playback controls.
 */
class MediaSessionCallback @Inject constructor(
  private val bookUriConverter: BookUriConverter,
  @Named(PrefKeys.CURRENT_BOOK)
  private val currentBookIdPref: Pref<Long>,
  private val bookSearchHandler: BookSearchHandler,
  private val autoConnection: AndroidAutoConnectedReceiver,
  private val player: MediaPlayer,
  private val bookSearchParser: BookSearchParser
) : MediaSessionCompat.Callback() {

  override fun onSkipToQueueItem(id: Long) {
    super.onSkipToQueueItem(id)
    val bookPlayChapters = player.book
      ?.chaptersAsBookPlayChapters()
        ?: return
    val chapter = bookPlayChapters[id.toInt()]
    player.changePosition(time = chapter.start, changedFile = chapter.file)
    player.play()
  }

  override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
    Timber.i("onPlayFromMediaId $mediaId")
    val uri = Uri.parse(mediaId)
    val type = bookUriConverter.type(uri)
    when (type) {
      BookUriConverter.BOOK_ID, BookUriConverter.CHAPTER_ID -> {
        val id = bookUriConverter.extractBook(uri)
        currentBookIdPref.value = id
        onPlay()
      }
      else -> Timber.e("Invalid mediaId $mediaId")
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
