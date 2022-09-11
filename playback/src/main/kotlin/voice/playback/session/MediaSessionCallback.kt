package voice.playback.session

import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat.TransportControls
import android.support.v4.media.session.MediaSessionCompat
import androidx.datastore.core.DataStore
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import voice.common.BookId
import voice.common.pref.CurrentBook
import voice.data.Chapter
import voice.logging.core.Logger
import voice.playback.androidauto.AndroidAutoConnectedReceiver
import voice.playback.di.PlaybackScope
import voice.playback.misc.Decibel
import voice.playback.player.MediaPlayer
import voice.playback.session.search.BookSearchHandler
import voice.playback.session.search.BookSearchParser
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Media session callback that handles playback controls.
 */
@PlaybackScope
class MediaSessionCallback
@Inject constructor(
  private val bookUriConverter: BookUriConverter,
  @CurrentBook
  private val currentBook: DataStore<BookId?>,
  private val bookSearchHandler: BookSearchHandler,
  private val autoConnection: AndroidAutoConnectedReceiver,
  private val bookSearchParser: BookSearchParser,
  private val player: MediaPlayer,
) : MediaSessionCompat.Callback() {

  private val scope = MainScope()

  override fun onSkipToQueueItem(id: Long) {
    scope.launch {
      Logger.i("onSkipToQueueItem $id")
      val chapter = player.book
        ?.chapters?.getOrNull(id.toInt()) ?: return@launch
      player.changePosition(0, chapter.id)
      player.play()
    }
  }

  override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
    scope.launch {
      Logger.i("onPlayFromMediaId $mediaId")
      mediaId ?: return@launch
      when (val parsed = bookUriConverter.parse(mediaId)) {
        is BookUriConverter.Parsed.Book -> {
          currentBook.updateData { parsed.bookId }
          onPlay()
        }
        is BookUriConverter.Parsed.Chapter -> {
          currentBook.updateData { parsed.bookId }
          player.changePosition(parsed.chapterId)
          onPlay()
        }
        BookUriConverter.Parsed.AllBooks -> {
          Logger.w("Didn't handle $parsed")
        }
        null -> {}
      }
    }
  }

  override fun onPlayFromSearch(query: String?, extras: Bundle?) {
    Logger.i("onPlayFromSearch $query")
    scope.launch {
      val bookSearch = bookSearchParser.parse(query, extras)
      bookSearchHandler.handle(bookSearch)
    }
  }

  override fun onSkipToNext() {
    Logger.i("onSkipToNext")
    scope.launch {
      if (autoConnection.connected) {
        player.next()
      } else {
        onFastForward()
      }
    }
  }

  override fun onRewind() {
    Logger.i("onRewind")
    scope.launch {
      player.skip(forward = false)
    }
  }

  override fun onSkipToPrevious() {
    Logger.i("onSkipToPrevious")
    scope.launch {
      if (autoConnection.connected) {
        player.previous(toNullOfNewTrack = true)
      } else {
        onRewind()
      }
    }
  }

  override fun onFastForward() {
    Logger.i("onFastForward")
    scope.launch {
      player.skip(forward = true)
    }
  }

  override fun onStop() {
    Logger.i("onStop")
    player.stop()
  }

  override fun onPause() {
    Logger.i("onPause")
    scope.launch {
      player.pause(rewind = true)
    }
  }

  override fun onPlay() {
    Logger.i("onPlay")
    scope.launch {
      player.play()
    }
  }

  override fun onSeekTo(pos: Long) {
    scope.launch {
      player.changePosition(pos)
    }
  }

  override fun onSetPlaybackSpeed(speed: Float) {
    scope.launch {
      player.setPlaybackSpeed(speed)
    }
  }

  override fun onCustomAction(action: String?, extras: Bundle?) {
    Logger.i("onCustomAction $action")
    scope.launch {
      when (action) {
        ANDROID_AUTO_ACTION_NEXT -> onSkipToNext()
        ANDROID_AUTO_ACTION_PREVIOUS -> onSkipToPrevious()
        ANDROID_AUTO_ACTION_FAST_FORWARD -> onFastForward()
        ANDROID_AUTO_ACTION_REWIND -> onRewind()
        PLAY_PAUSE_ACTION -> player.playPause()
        SKIP_SILENCE_ACTION -> {
          val skip = extras!!.getBoolean(SKIP_SILENCE_EXTRA)
          player.setSkipSilences(skip)
        }
        SET_POSITION_ACTION -> {
          val id = Chapter.Id(extras!!.getString(SET_POSITION_EXTRA_CHAPTER)!!)
          val time = extras.getLong(SET_POSITION_EXTRA_TIME)
          player.changePosition(time, id)
        }
        FORCED_PREVIOUS -> {
          player.previous(toNullOfNewTrack = true)
        }
        FORCED_NEXT -> {
          player.next()
        }
        SET_VOLUME -> {
          val volume = extras!!.getFloat(SET_VOLUME_EXTRA_VOLUME)
          player.setVolume(volume)
        }
        SET_GAIN -> {
          val gain = Decibel(extras!!.getFloat(SET_GAIN_EXTRA_GAIN))
          player.setGain(gain)
        }
        PAUSE_WITH_REWIND -> {
          val rewindAmount = extras!!.getLong(PAUSE_WITH_REWIND_EXTRA_DURATION).milliseconds
          player.pause(rewindAmount)
        }
        else -> {
          Logger.w("Didn't handle customAction=$action")
        }
      }
    }
  }
}

private inline fun TransportControls.sendCustomAction(action: String, fillBundle: Bundle.() -> Unit = {}) {
  sendCustomAction(action, Bundle().apply(fillBundle))
}

private const val PLAY_PAUSE_ACTION = "playPause"

fun TransportControls.playPause() = sendCustomAction(PLAY_PAUSE_ACTION)

private const val SKIP_SILENCE_ACTION = "skipSilence"
private const val SKIP_SILENCE_EXTRA = "$SKIP_SILENCE_ACTION#value"

fun TransportControls.skipSilence(skip: Boolean) = sendCustomAction(SKIP_SILENCE_ACTION) {
  putBoolean(SKIP_SILENCE_EXTRA, skip)
}

private const val SET_POSITION_ACTION = "setPosition"
private const val SET_POSITION_EXTRA_TIME = "$SET_POSITION_ACTION#time"
private const val SET_POSITION_EXTRA_CHAPTER = "$SET_POSITION_ACTION#uri"

fun TransportControls.setPosition(time: Long, id: Chapter.Id) = sendCustomAction(SET_POSITION_ACTION) {
  putString(SET_POSITION_EXTRA_CHAPTER, id.value)
  putLong(SET_POSITION_EXTRA_TIME, time)
}

private const val SET_VOLUME = "setVolume"
private const val SET_VOLUME_EXTRA_VOLUME = "$SET_VOLUME#volume"

fun TransportControls.setVolume(volume: Float) = sendCustomAction(SET_VOLUME) {
  putFloat(SET_VOLUME_EXTRA_VOLUME, volume)
}

private const val SET_GAIN = "setGain"
private const val SET_GAIN_EXTRA_GAIN = "$SET_GAIN#volume"

fun TransportControls.setGain(gain: Decibel) = sendCustomAction(SET_GAIN) {
  putFloat(SET_GAIN_EXTRA_GAIN, gain.value)
}

const val ANDROID_AUTO_ACTION_FAST_FORWARD = "fast_forward"
const val ANDROID_AUTO_ACTION_REWIND = "rewind"
const val ANDROID_AUTO_ACTION_NEXT = "next"
const val ANDROID_AUTO_ACTION_PREVIOUS = "previous"

private const val FORCED_PREVIOUS = "forcedPrevious"
fun TransportControls.forcedPrevious() = sendCustomAction(FORCED_PREVIOUS)

private const val FORCED_NEXT = "forcedNext"
fun TransportControls.forcedNext() = sendCustomAction(FORCED_NEXT)

private const val PAUSE_WITH_REWIND = "pauseWithRewind"
private const val PAUSE_WITH_REWIND_EXTRA_DURATION = "$PAUSE_WITH_REWIND#duration"

fun TransportControls.pauseWithRewind(rewind: Duration) = sendCustomAction(PAUSE_WITH_REWIND) {
  putLong(PAUSE_WITH_REWIND_EXTRA_DURATION, rewind.inWholeMilliseconds)
}
