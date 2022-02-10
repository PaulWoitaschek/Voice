package de.ph1b.audiobook.playback.player

import android.support.v4.media.session.PlaybackStateCompat
import androidx.datastore.core.DataStore
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.util.Assertions.checkMainThread
import de.paulwoitaschek.flowpref.Pref
import de.ph1b.audiobook.common.pref.CurrentBook
import de.ph1b.audiobook.common.pref.PrefKeys
import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.data.BookContent2
import de.ph1b.audiobook.data.Chapter2
import de.ph1b.audiobook.data.markForPosition
import de.ph1b.audiobook.data.repo.BookRepo2
import de.ph1b.audiobook.playback.di.PlaybackScope
import de.ph1b.audiobook.playback.playstate.PlayStateManager
import de.ph1b.audiobook.playback.playstate.PlayStateManager.PlayState
import de.ph1b.audiobook.playback.playstate.PlayerState
import de.ph1b.audiobook.playback.session.ChangeNotifier
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@PlaybackScope
class MediaPlayer
@Inject
constructor(
  private val playStateManager: PlayStateManager,
  @Named(PrefKeys.AUTO_REWIND_AMOUNT)
  private val autoRewindAmountPref: Pref<Int>,
  @Named(PrefKeys.SEEK_TIME)
  private val seekTimePref: Pref<Int>,
  private val dataSourceConverter: DataSourceConverter,
  private val player: ExoPlayer,
  private val changeNotifier: ChangeNotifier,
  private val repo: BookRepo2,
  @CurrentBook
  private val currentBook: DataStore<Book2.Id?>,
) {

  private val scope = MainScope()

  private val _book = MutableStateFlow<Book2?>(null)
  var book: Book2?
    get() = _book.value
    private set(value) {
      _book.value = value
    }

  private val _state = MutableStateFlow(PlayerState.IDLE)
  private var state: PlayerState
    get() = _state.value
    set(value) {
      _state.value = value
    }

  private val seekTime: Duration get() = seekTimePref.value.seconds
  private var autoRewindAmount by autoRewindAmountPref

  init {
    player.onSessionPlaybackStateNeedsUpdate {
      updateMediaSessionPlaybackState()
    }
    player.onStateChanged {
      playStateManager.playState = when (it) {
        PlayerState.IDLE -> PlayState.Stopped
        PlayerState.ENDED -> PlayState.Stopped
        PlayerState.PAUSED -> PlayState.Paused
        PlayerState.PLAYING -> PlayState.Playing
      }
      state = it
    }

    player.onError {
      Timber.e("onError")
      player.playWhenReady = false
    }

    // upon position change update the book
    player.onPositionDiscontinuity {
      val position = player.currentPosition
        .coerceAtLeast(0)
      Timber.i("onPositionDiscontinuity with currentPos=$position")

      updateContent {
        copy(
          positionInChapter = position,
          currentChapter = chapters[player.currentMediaItemIndex]
        )
      }
    }

    scope.launch {
      _state.collect {
        Timber.i("state changed to $it")
        // upon end stop the player
        if (it == PlayerState.ENDED) {
          Timber.v("onEnded. Stopping player")
          checkMainThread()
          player.playWhenReady = false
        }
      }
    }

    scope.launch {
      _state.map { it == PlayerState.PLAYING }.distinctUntilChanged()
        .transformLatest { playing ->
          if (playing) {
            while (true) {
              delay(200)
              emit(player.currentPosition.coerceAtLeast(0))
            }
          }
        }
        .distinctUntilChangedBy {
          // only if the second changed, emit
          it / 1000
        }
        .collect { time ->
          updateContent {
            copy(
              positionInChapter = time,
              currentChapter = chapters[player.currentMediaItemIndex],
            )
          }
        }
    }

    scope.launch {
      val notIdleFlow = _state.filter { it != PlayerState.IDLE }
      val chaptersChanged = currentBook.data.filterNotNull()
        .flatMapLatest { repo.flow(it) }
        .filterNotNull()
        .map { it.chapters }
        .distinctUntilChanged()
      combine(notIdleFlow, chaptersChanged) { _, _ -> }
        .collect { prepare() }
    }
  }

  fun updateMediaSessionPlaybackState() {
    val playbackStateCompat = when (player.playbackState) {
      Player.STATE_READY, Player.STATE_BUFFERING -> {
        if (player.playWhenReady) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
      }
      Player.STATE_ENDED -> PlaybackStateCompat.STATE_STOPPED
      Player.STATE_IDLE -> PlaybackStateCompat.STATE_NONE
      else -> PlaybackStateCompat.STATE_NONE
    }
    changeNotifier.updatePlaybackState(playbackStateCompat, book)
  }

  private fun alreadyInitializedChapters(book: Book2): Boolean {
    val currentBook = this.book
      ?: return false
    return currentBook.chapters == book.chapters
  }

  fun playPause() {
    if (state == PlayerState.PLAYING) {
      pause(rewind = true)
    } else {
      play()
    }
  }

  fun play() {
    Timber.v("play called in state $state, currentFile=${book?.currentChapter}")
    prepare()
    updateContent {
      copy(lastPlayedAt = Instant.now())
    }
    val book = book ?: return
    if (state == PlayerState.ENDED) {
      Timber.i("play in state ended. Back to the beginning")
      changePosition(0, book.chapters.first().id)
    }

    if (state == PlayerState.ENDED || state == PlayerState.PAUSED) {
      checkMainThread()
      player.playWhenReady = true
    } else Timber.d("ignore play in state $state")
  }

  private fun skip(skipAmount: Duration) {
    checkMainThread()
    prepare()
    if (state == PlayerState.IDLE)
      return

    book?.let {
      val currentPos = player.currentPosition.milliseconds
        .coerceAtLeast(Duration.ZERO)
      val duration = player.duration.milliseconds

      val seekTo = currentPos + skipAmount
      Timber.v("currentPos=$currentPos, seekTo=$seekTo, duration=$duration")
      when {
        seekTo < Duration.ZERO -> previous(false)
        seekTo > duration -> next()
        else -> changePosition(seekTo.inWholeMilliseconds)
      }
    }
  }

  fun skip(forward: Boolean) {
    Timber.v("skip forward=$forward")
    skip(skipAmount = if (forward) seekTime else -seekTime)
  }

  /** If current time is > 2000ms, seek to 0. Else play previous chapter if there is one. */
  fun previous(toNullOfNewTrack: Boolean) {
    Timber.i("previous with toNullOfNewTrack=$toNullOfNewTrack called in state $state")
    prepare()
    if (state == PlayerState.IDLE)
      return

    book?.let {
      val handled = previousByMarks(it)
      if (!handled) previousByFile(it, toNullOfNewTrack)
    }
  }

  private fun previousByFile(content: Book2, toNullOfNewTrack: Boolean) {
    checkMainThread()
    val previousChapter = content.previousChapter
    if (player.currentPosition > 2000 || previousChapter == null) {
      Timber.i("seekTo beginning")
      changePosition(0)
    } else {
      if (toNullOfNewTrack) {
        changePosition(0, previousChapter.id)
      } else {
        val time = (previousChapter.duration.milliseconds - seekTime)
          .coerceAtLeast(Duration.ZERO)
        changePosition(time.inWholeMilliseconds, previousChapter.id)
      }
    }
  }

  private fun previousByMarks(content: Book2): Boolean {
    val currentChapter = content.currentChapter
    val currentMark = currentChapter.markForPosition(content.content.positionInChapter)
    val timePlayedInMark = content.content.positionInChapter - currentMark.startMs
    if (timePlayedInMark > 2000) {
      changePosition(currentMark.startMs)
      return true
    } else {
      // jump to the start of the previous mark
      val indexOfCurrentMark = currentChapter.chapterMarks.indexOf(currentMark)
      if (indexOfCurrentMark > 0) {
        changePosition(currentChapter.chapterMarks[indexOfCurrentMark - 1].startMs)
        return true
      }
    }
    return false
  }

  private fun prepare() {
    val book = runBlocking {
      val id = currentBook.data.first() ?: return@runBlocking null
      repo.flow(id).first()
    } ?: return
    val shouldInitialize = player.playbackState == Player.STATE_IDLE || !alreadyInitializedChapters(book)
    if (!shouldInitialize) {
      return
    }
    Timber.i("prepare $book")
    this.book = book
    checkMainThread()
    player.playWhenReady = false
    player.setMediaSource(dataSourceConverter.toMediaSource(book))
    player.prepare()
    player.seekTo(book.content.currentChapterIndex, book.content.positionInChapter)
    player.setPlaybackSpeed(book.content.playbackSpeed)
    player.skipSilenceEnabled = book.content.skipSilence
    state = PlayerState.PAUSED
  }

  fun stop() {
    checkMainThread()
    player.stop()
  }

  fun pause(rewind: Boolean) {
    Timber.v("pause")
    checkMainThread()
    when (state) {
      PlayerState.PLAYING -> {
        book?.let {
          player.playWhenReady = false

          if (rewind) {
            val autoRewind = autoRewindAmount * 1000
            if (autoRewind != 0) {
              // get the raw position with rewinding applied
              val currentPosition = player.currentPosition
                .coerceAtLeast(0)
              var maybeSeekTo = (currentPosition - autoRewind)
                .coerceAtLeast(0) // make sure not to get into negative time

              // now try to find the current chapter mark and make sure we don't auto-rewind
              // to a previous mark
              val currentChapter = it.currentChapter
              val currentMark = currentChapter.markForPosition(currentPosition)
              val markForSeeking = currentChapter.markForPosition(maybeSeekTo)
              if (markForSeeking != currentMark) {
                maybeSeekTo = maybeSeekTo.coerceAtLeast(currentMark.startMs)
              }

              // finally change position
              changePosition(maybeSeekTo)
            }
          }
        }
      }
      else -> Timber.d("pause ignored because of $state")
    }
  }

  fun next() {
    checkMainThread()
    prepare()
    val book = book
      ?: return
    val nextMark = book.nextMark
    if (nextMark != null) {
      changePosition(nextMark.startMs)
    } else {
      book.nextChapter?.let { changePosition(0, it.id) }
    }
  }

  /** Changes the current position in book. */
  fun changePosition(time: Long, changedChapter: Chapter2.Id? = null) {
    checkMainThread()
    Timber.v("changePosition with time $time and file $changedChapter")
    prepare()
    if (state == PlayerState.IDLE)
      return
    updateContent {
      val newChapter = changedChapter ?: currentChapter
      player.seekTo(chapters.indexOf(newChapter), time)
      copy(positionInChapter = time, currentChapter = newChapter)
    }
  }

  /** The current playback speed. 1.0 for normal playback, 2.0 for twice the speed, etc. */
  fun setPlaybackSpeed(speed: Float) {
    checkMainThread()
    prepare()
    updateContent { copy(playbackSpeed = speed) }
    player.setPlaybackSpeed(speed)
  }

  fun setSkipSilences(skip: Boolean) {
    checkMainThread()
    Timber.v("setSkipSilences to $skip")
    prepare()
    updateContent { copy(skipSilence = skip) }
    player.skipSilenceEnabled = skip
  }

  fun release() {
    player.release()
    scope.cancel()
  }

  private fun updateContent(update: BookContent2.() -> BookContent2) {
    val book = book ?: return
    val updated = book.copy(content = update(book.content))
    this.book = updated
    runBlocking {
      repo.updateBook(book.id, update)
    }
  }
}
