package voice.core.playback.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.test.utils.FakeMediaSource
import androidx.media3.test.utils.FakeTimeline
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.ChapterMark
import voice.core.data.MarkData
import voice.core.data.repo.BookRepository
import voice.core.logging.api.LogWriter
import voice.core.logging.api.Logger
import voice.core.playback.DefaultPlaybackSpeedApplier
import voice.core.playback.MemoryDataStore
import voice.core.playback.session.MediaItemProvider
import voice.core.playback.session.realChapterId
import voice.core.playback.session.search.book
import voice.core.playback.session.toMediaIdOrNull
import voice.core.sleeptimer.SleepTimer
import voice.core.sleeptimer.SleepTimerMode
import voice.core.sleeptimer.SleepTimerState
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.uuid.Uuid

@RunWith(AndroidJUnit4::class)
class VoicePlayerTest {

  init {
    Logger.install(
      object : LogWriter {
        override fun log(
          severity: Logger.Severity,
          message: String,
          throwable: Throwable?,
        ) {
          println("$severity: $message")
          throwable?.printStackTrace()
        }
      },
    )
  }

  private val seekTimeStore = MemoryDataStore(2)
  private val autoRewindAmountStore = MemoryDataStore(2)
  private val defaultPlaybackSpeedStore = MemoryDataStore(1F)

  private val internalPlayer = TestExoPlayerBuilder(ApplicationProvider.getApplicationContext())
    .setMediaSourceFactory(
      mockk {
        every { createMediaSource(any()) } answers {
          val mediaItem = arg<MediaItem>(0)
          val mediaId = mediaItem.mediaId
          val chapter = currentBook.chapters.single {
            it.id == mediaId.toMediaIdOrNull()!!.realChapterId
          }
          FakeMediaSource(
            FakeTimeline(
              FakeTimeline.TimelineWindowDefinition.Builder()
                .setPeriodCount(1)
                .setSeekable(true)
                .setDurationUs(TimeUnit.MILLISECONDS.toMicros(chapter.duration))
                .setMediaItem(mediaItem)
                .build(),
            ),
          )
        }
      },
    )
    .build()

  private val scope = TestScope()
  private val mediaItemProvider = MediaItemProvider(mockk(), mockk(), mockk(), mockk(), mockk(), mockk())
  private val bookId = BookId(Uuid.random().toString())
  private lateinit var currentBook: Book
  private val sleepTimer = FakeSleepTimer()
  private val repo = mockk<BookRepository> {
    coEvery { get(bookId) } answers { currentBook }
    coEvery { updateBook(any(), any()) } just Runs
  }
  private val player = VoicePlayer(
    player = internalPlayer,
    repo = repo,
    currentBookStoreId = mockk {
      every { data } returns flowOf(bookId)
    },
    seekTimeStore = seekTimeStore,
    autoRewindAmountStore = autoRewindAmountStore,
    defaultPlaybackSpeedApplier = DefaultPlaybackSpeedApplier(defaultPlaybackSpeedStore, repo),
    scope = scope,
    mediaItemProvider = mediaItemProvider,
    volumeGain = mockk(relaxed = true),
    sleepTimer = sleepTimer,
    analytics = mockk(relaxed = true),
  )

  @Test
  fun `seekToNext does not clip`() = scope.runTest {
    setMediaItems(
      listOf(
        chapter(
          ChapterMark(startMs = 0, endMs = 19_999, name = null),
          ChapterMark(startMs = 20_000, endMs = 30_000, name = null),
        ),
        chapter(
          ChapterMark(startMs = 0, endMs = 19_999, name = null),
          ChapterMark(startMs = 20_000, endMs = 30_000, name = null),
        ),
      ),
    )

    seekTimeStore.updateData { 7 }

    player.prepare()
    awaitReady()
    player.shouldHavePosition(0, 0)

    player.seekToNext()
    player.shouldHavePosition(0, 7_000)

    player.seekToNext()
    player.shouldHavePosition(0, 14_000)

    player.seekToNext()
    player.shouldHavePosition(0, 21_000)

    player.seekToNext()
    player.shouldHavePosition(0, 28_000)

    player.seekToNext()
    player.shouldHavePosition(1, 5_000)

    player.seekToNext()
    player.shouldHavePosition(1, 12_000)
  }

  @Test
  fun `seekToPrevious does not clip`() = scope.runTest {
    setMediaItems(
      listOf(
        chapter(
          ChapterMark(startMs = 0, endMs = 4_999, name = null),
          ChapterMark(startMs = 5_000, endMs = 12_000, name = null),
        ),
        chapter(
          ChapterMark(startMs = 0, endMs = 4_999, name = null),
          ChapterMark(startMs = 5_000, endMs = 12_001, name = null),
        ),
      ),
    )

    seekTimeStore.updateData { 5 }

    player.seekTo(1, 12_000)
    player.prepare()
    awaitReady()

    player.shouldHavePosition(1, 11_999)

    player.seekToPrevious()
    player.shouldHavePosition(1, 6_999)

    player.seekToPrevious()
    player.shouldHavePosition(1, 1_999)

    player.seekToPrevious()
    player.shouldHavePosition(0, 1_998)

    player.seekToPrevious()
    player.shouldHavePosition(0, 0)

    player.seekToPrevious()
    player.shouldHavePosition(0, 0)
  }

  @Test
  fun `forceSeekToNext jumps to chapters`() = scope.runTest {
    setMediaItems(
      listOf(
        chapter(
          ChapterMark(startMs = 0, endMs = 11_999, name = null),
          ChapterMark(startMs = 12_000, endMs = 20_000, name = null),
        ),
        chapter(
          ChapterMark(startMs = 0, endMs = 11_999, name = null),
          ChapterMark(startMs = 12_000, endMs = 20_000, name = null),
        ),
      ),
    )

    player.prepare()
    awaitReady()
    player.shouldHavePosition(0, 0)

    player.forceSeekToNext()
    player.shouldHavePosition(1, 0)

    player.forceSeekToNext()
    player.shouldHavePosition(2, 0)

    player.forceSeekToNext()
    player.shouldHavePosition(3, 0)

    player.forceSeekToNext()
    player.shouldHavePosition(3, 0)
  }

  @Test
  fun `forceSeekToPrevious jumps to chapters`() = scope.runTest {
    setMediaItems(
      listOf(
        chapter(
          ChapterMark(startMs = 0, endMs = 11_999, name = null),
          ChapterMark(startMs = 12_000, endMs = 20_000, name = null),
        ),
        chapter(
          ChapterMark(startMs = 0, endMs = 11_999, name = null),
          ChapterMark(startMs = 12_000, endMs = 20_000, name = null),
        ),
      ),
    )

    player.seekTo(3, 6_000)
    player.prepare()
    awaitReady()
    player.shouldHavePosition(3, 6_000)

    player.forceSeekToPrevious()
    player.shouldHavePosition(3, 0)

    player.forceSeekToPrevious()
    player.shouldHavePosition(2, 0)

    player.forceSeekToPrevious()
    player.shouldHavePosition(1, 0)

    player.forceSeekToPrevious()
    player.shouldHavePosition(0, 0)
  }

  @Test
  fun `forceSeekToPrevious jumps to previous chapter when in the 2s window`() = scope.runTest {
    setMediaItems(
      listOf(
        chapter(
          ChapterMark(startMs = 0, endMs = 11_999, name = null),
          ChapterMark(startMs = 12_000, endMs = 20_000, name = null),
        ),
        chapter(
          ChapterMark(startMs = 0, endMs = 11_999, name = null),
          ChapterMark(startMs = 12_000, endMs = 20_000, name = null),
        ),
      ),
    )

    player.seekTo(2, 1_000)
    player.prepare()
    awaitReady()
    player.shouldHavePosition(2, 1_000)

    player.forceSeekToPrevious()
    player.shouldHavePosition(1, 0)

    player.seekTo(1, 1_000)
    player.forceSeekToPrevious()
    player.shouldHavePosition(0, 0)
  }

  @Test
  fun `setBook resumes inside matching chapter mark`() = scope.runTest {
    val chapter = chapter(
      ChapterMark(startMs = 0, endMs = 11_999, name = null),
      ChapterMark(startMs = 12_000, endMs = 20_000, name = null),
    )
    setMediaItems(
      chapters = listOf(chapter),
      currentChapter = chapter,
      positionInChapter = 15_000,
    )

    player.prepare()
    awaitReady()

    player.shouldHavePosition(1, 3_000)
  }

  @Test
  fun `end of chapter sleep timer pauses at start of next chapter mark`() = scope.runTest {
    setMediaItems(
      listOf(
        chapter(
          ChapterMark(startMs = 0, endMs = 1_000, name = null),
          ChapterMark(startMs = 1_000, endMs = 2_000, name = null),
        ),
      ),
    )

    player.prepare()
    awaitReady()
    sleepTimer.enable(SleepTimerMode.EndOfChapter)

    TestPlayerRunHelper.play(internalPlayer).untilPlayWhenReadyIs(false)

    player.shouldHavePosition(1, 0)
    assertFalse(player.playWhenReady)
    assertEquals(expected = SleepTimerState.Disabled, actual = sleepTimer.state.value)
  }

  @Test
  fun `end of chapter sleep timer pauses at start of normal file chapter`() = scope.runTest {
    setMediaItems(
      listOf(
        chapter(ChapterMark(startMs = 0, endMs = 1_000, name = null)),
        chapter(ChapterMark(startMs = 0, endMs = 1_000, name = null)),
      ),
    )

    player.prepare()
    awaitReady()
    sleepTimer.enable(SleepTimerMode.EndOfChapter)

    TestPlayerRunHelper.play(internalPlayer).untilPlayWhenReadyIs(false)

    player.shouldHavePosition(1, 0)
    assertFalse(player.playWhenReady)
    assertEquals(expected = SleepTimerState.Disabled, actual = sleepTimer.state.value)
  }

  @Test
  fun `auto rewind clamps to current chapter start`() = scope.runTest {
    setMediaItems(
      listOf(
        chapter(
          ChapterMark(startMs = 0, endMs = 11_999, name = null),
          ChapterMark(startMs = 12_000, endMs = 20_000, name = null),
        ),
      ),
    )

    autoRewindAmountStore.updateData { 5 }

    player.seekTo(1, 3_000)
    player.prepare()
    awaitReady()
    player.shouldHavePosition(1, 3_000)

    player.pause()

    player.shouldHavePosition(1, 0)
  }

  @Test
  fun `never played book starts at the default playback speed`() = scope.runTest {
    defaultPlaybackSpeedStore.updateData { 1.25F }

    setMediaItems(listOf(chapter(ChapterMark(startMs = 0, endMs = 10_000, name = null))))

    assertEquals(expected = 1.25F, actual = internalPlayer.playbackParameters.speed)
    coVerify { repo.updateBook(bookId, any()) }
  }

  @Test
  fun `book that was played keeps its own playback speed`() = scope.runTest {
    defaultPlaybackSpeedStore.updateData { 1.25F }

    setMediaItems(
      listOf(chapter(ChapterMark(startMs = 0, endMs = 10_000, name = null))),
      lastPlayedAt = Instant.ofEpochMilli(1_000),
    )

    assertEquals(expected = 1F, actual = internalPlayer.playbackParameters.speed)
  }

  @Test
  fun `never played book with a custom speed keeps it`() = scope.runTest {
    defaultPlaybackSpeedStore.updateData { 1.25F }

    setMediaItems(
      listOf(chapter(ChapterMark(startMs = 0, endMs = 10_000, name = null))),
      playbackSpeed = 1.5F,
    )

    assertEquals(expected = 1.5F, actual = internalPlayer.playbackParameters.speed)
  }

  private fun TestScope.setMediaItems(
    chapters: List<Chapter>,
    currentChapter: Chapter = chapters.first(),
    positionInChapter: Long = 0,
    playbackSpeed: Float = 1F,
    lastPlayedAt: Instant = Instant.EPOCH,
  ) {
    currentBook = book(chapters, bookId).update {
      it.copy(
        currentChapter = currentChapter.id,
        positionInChapter = positionInChapter,
        playbackSpeed = playbackSpeed,
        lastPlayedAt = lastPlayedAt,
      )
    }
    player.setMediaItem(mediaItemProvider.mediaItem(currentBook))
    runCurrent()
  }

  @Test
  fun `forceSeekToPrevious jumps to chapter start when outside the 2s window`() = scope.runTest {
    setMediaItems(
      listOf(
        chapter(
          ChapterMark(startMs = 0, endMs = 11_999, name = null),
          ChapterMark(startMs = 12_000, endMs = 20_000, name = null),
        ),
        chapter(
          ChapterMark(startMs = 0, endMs = 11_999, name = null),
          ChapterMark(startMs = 12_000, endMs = 20_000, name = null),
        ),
      ),
    )

    player.seekTo(2, 3_000)
    player.prepare()
    awaitReady()
    player.shouldHavePosition(2, 3_000)

    player.forceSeekToPrevious()
    player.shouldHavePosition(2, 0)

    player.seekTo(2, 1_000)
    player.forceSeekToPrevious()
    player.shouldHavePosition(1, 0)
  }

  private fun chapter(vararg marks: ChapterMark): Chapter {
    return Chapter(
      id = ChapterId(Uuid.random().toString()),
      name = "chapter",
      duration = marks.maxOf { it.endMs },
      fileLastModified = Instant.EPOCH,
      markData = marks.map {
        MarkData(it.startMs, it.name ?: "mark ")
      },
      fileSize = 0,
    )
  }

  private fun awaitReady() {
    TestPlayerRunHelper.runUntilPlaybackState(internalPlayer, Player.STATE_READY)
  }

  @IgnorableReturnValue
  private fun Player.shouldHavePosition(
    currentMediaItemIndex: Int,
    currentPosition: Long,
  ): Player {
    scope.advanceUntilIdle()
    assertEquals(expected = currentMediaItemIndex, actual = this.currentMediaItemIndex)
    assertEquals(expected = currentPosition, actual = this.currentPosition)
    return this
  }

  private class FakeSleepTimer : SleepTimer {
    override val state: StateFlow<SleepTimerState>
      get() = stateFlow

    private val stateFlow = MutableStateFlow<SleepTimerState>(SleepTimerState.Disabled)

    override fun enable(mode: SleepTimerMode) {
      stateFlow.value = when (mode) {
        is SleepTimerMode.TimedWithDuration -> SleepTimerState.Enabled.WithDuration(mode.duration)
        SleepTimerMode.TimedWithDefault -> error("TimedWithDefault is not used in these tests")
        SleepTimerMode.EndOfChapter -> SleepTimerState.Enabled.WithEndOfChapter
      }
    }

    override fun disable() {
      stateFlow.value = SleepTimerState.Disabled
    }
  }
}
