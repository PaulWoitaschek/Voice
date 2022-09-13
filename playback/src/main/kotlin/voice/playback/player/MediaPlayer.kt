package voice.playback.player

import android.support.v4.media.session.PlaybackStateCompat
import androidx.datastore.core.DataStore
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import de.paulwoitaschek.flowpref.Pref
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
import voice.common.BookId
import voice.common.pref.CurrentBook
import voice.common.pref.PrefKeys
import voice.data.Book
import voice.data.BookContent
import voice.data.Chapter
import voice.data.markForPosition
import voice.data.repo.BookRepository
import voice.logging.core.Logger
import voice.playback.di.PlaybackScope
import voice.playback.misc.Decibel
import voice.playback.misc.VolumeGain
import voice.playback.playstate.PlayStateManager
import voice.playback.playstate.PlayStateManager.PlayState
import voice.playback.playstate.PlayerState
import voice.playback.session.ChangeNotifier
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
  private val player: Player,
  private val changeNotifier: ChangeNotifier,
  private val repo: BookRepository,
  @CurrentBook
  private val currentBook: DataStore<BookId?>,
  private val volumeGain: VolumeGain,
) {

  private val scope = MainScope()

  private val _book = MutableStateFlow<Book?>(null)
  var book: Book?
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
    player.onAudioSessionIdChanged {
      volumeGain.audioSessionId = it
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
      Logger.w("onError")
      player.playWhenReady = false
    }

    // upon position change update the book
    player.onPositionDiscontinuity {
      val position = player.currentPosition
        .coerceAtLeast(0)
      Logger.v("onPositionDiscontinuity with currentPos=$position")

      scope.launch {
        updateContent {
          copy(
            positionInChapter = position,
            currentChapter = chapters[player.currentMediaItemIndex],
          )
        }
      }
    }

    scope.launch {
      _state.collect {
        Logger.v("state changed to $it")
        // upon end stop the player
        if (it == PlayerState.ENDED) {
          Logger.v("onEnded. Stopping player")
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

  private fun alreadyInitializedChapters(book: Book): Boolean {
    val currentBook = this.book
      ?: return false
    return currentBook.chapters == book.chapters
  }

  suspend fun playPause() {
    if (state == PlayerState.PLAYING) {
      pause(rewind = true)
    } else {
      play()
    }
  }

  suspend fun play() {
    Logger.v("play called in state $state, currentFile=${book?.currentChapter}")
    prepare()
    updateContent {
      copy(lastPlayedAt = Instant.now())
    }
    val book = book ?: return
    if (state == PlayerState.ENDED) {
      Logger.d("play in state ended. Back to the beginning")
      changePosition(0, book.chapters.first().id)
    }

    if (state == PlayerState.ENDED || state == PlayerState.PAUSED) {
      player.playWhenReady = true
    } else {
      Logger.d("ignore play in state $state")
    }
  }

  private suspend fun skip(skipAmount: Duration) {
    prepare()
    if (state == PlayerState.IDLE) {
      return
    }

    book?.let {
      val currentPos = player.currentPosition.milliseconds
        .coerceAtLeast(Duration.ZERO)
      val duration = player.duration.milliseconds

      val seekTo = currentPos + skipAmount
      Logger.v("currentPos=$currentPos, seekTo=$seekTo, duration=$duration")
      when {
        seekTo < Duration.ZERO -> previous(false)
        seekTo > duration -> next()
        else -> changePosition(seekTo.inWholeMilliseconds)
      }
    }
  }

  suspend fun skip(forward: Boolean) {
    Logger.v("skip forward=$forward")
    skip(skipAmount = if (forward) seekTime else -seekTime)
  }

  /** If current time is > 2000ms, seek to 0. Else play previous chapter if there is one. */
  suspend fun previous(toNullOfNewTrack: Boolean) {
    Logger.i("previous with toNullOfNewTrack=$toNullOfNewTrack called in state $state")
    prepare()
    if (state == PlayerState.IDLE) {
      return
    }

    book?.let {
      previousByMarks(it)
      val handled = previousByMarks(it)
      if (!handled) {
        previousByFile(it, toNullOfNewTrack)
      }
    }
  }

  private suspend fun previousByFile(content: Book, toNullOfNewTrack: Boolean) {
    val previousChapter = content.previousChapter
    if (player.currentPosition > 2000 || previousChapter == null) {
      Logger.i("seekTo beginning")
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

  private suspend fun previousByMarks(content: Book): Boolean {
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

  private suspend fun prepare() {
    val id = currentBook.data.first() ?: return
    val book = repo.get(id) ?: return
    val shouldInitialize = player.playbackState == Player.STATE_IDLE || !alreadyInitializedChapters(book)
    if (!shouldInitialize) {
      return
    }
    Logger.v("prepare $book")
    this.book = book
    player.playWhenReady = false
    player.setMediaItems(
      book.toMediaItems(),
      book.content.currentChapterIndex,
      book.content.positionInChapter,
    )
    player.prepare()
    player.setPlaybackSpeed(book.content.playbackSpeed)
    volumeGain.gain = Decibel(book.content.gain)
    if (player is ExoPlayer) {
      player.skipSilenceEnabled = book.content.skipSilence
    }
    state = PlayerState.PAUSED
  }

  fun stop() {
    player.stop()
  }

  suspend fun pause(rewind: Boolean) {
    pause(
      rewindAmount = if (rewind) {
        autoRewindAmount.seconds
      } else {
        Duration.ZERO
      },
    )
  }

  suspend fun pause(rewindAmount: Duration) {
    Logger.v("pause(rewindAmount=$rewindAmount)")
    when (state) {
      PlayerState.PLAYING -> {
        book?.let {
          player.playWhenReady = false

          if (rewindAmount > Duration.ZERO) {
            // get the raw position with rewinding applied
            val currentPosition = player.currentPosition
              .coerceAtLeast(0)
            var maybeSeekTo = (currentPosition - rewindAmount.inWholeMilliseconds)
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
      else -> Logger.d("pause ignored because of $state")
    }
  }

  suspend fun next() {
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

  suspend fun changePosition(time: Long, changedChapter: Chapter.Id? = null) {
    Logger.v("changePosition with time $time and file $changedChapter")
    prepare()
    if (state == PlayerState.IDLE) {
      return
    }
    updateContent {
      val newChapter = changedChapter ?: currentChapter
      val index = chapters.indexOf(newChapter)
      if (index != -1) {
        player.seekTo(index, time)
        copy(positionInChapter = time, currentChapter = newChapter)
      } else {
        Logger.e("Illegal seek command. Content=$this, changedChapter=$changedChapter, currentChapter=$currentChapter")
        this
      }
    }
  }

  suspend fun changePosition(chapter: Chapter.Id) {
    Logger.v("chapterPosition($chapter)")
    prepare()
    if (state == PlayerState.IDLE) {
      return
    }
    updateContent {
      if (chapter !in chapters || currentChapter == chapter) {
        return@updateContent this
      }
      player.seekTo(chapters.indexOf(chapter), 0)
      copy(positionInChapter = 0, currentChapter = chapter)
    }
  }

  /** The current playback speed. 1.0 for normal playback, 2.0 for twice the speed, etc. */
  suspend fun setPlaybackSpeed(speed: Float) {
    prepare()
    updateContent { copy(playbackSpeed = speed) }
    player.setPlaybackSpeed(speed)
  }

  fun setVolume(volume: Float) {
    require(volume in 0F..1F)
    player.volume = volume
  }

  suspend fun setGain(gain: Decibel) {
    volumeGain.gain = gain
    updateContent { copy(gain = gain.value) }
  }

  suspend fun setSkipSilences(skip: Boolean) {
    Logger.v("setSkipSilences to $skip")
    prepare()
    updateContent { copy(skipSilence = skip) }
    if (player is ExoPlayer) {
      player.skipSilenceEnabled = skip
    }
  }

  fun release() {
    player.release()
    scope.cancel()
  }

  private suspend fun updateContent(update: BookContent.() -> BookContent) {
    val book = book ?: return
    val updated = book.copy(content = update(book.content))
    this.book = updated
    repo.updateBook(book.id, update)
  }
}
