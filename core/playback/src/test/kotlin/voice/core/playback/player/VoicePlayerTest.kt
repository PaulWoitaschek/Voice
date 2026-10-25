package voice.core.playback.player

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Looper
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
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verifyOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import voice.core.analytics.api.Analytics
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.ChapterMark
import voice.core.data.ListeningEvent
import voice.core.data.MarkData
import voice.core.data.repo.ListeningHistoryRepo
import voice.core.data.store.CurrentBookStore
import voice.core.playback.AutoRewindAmountStore
import voice.core.playback.LastPlaybackSpeedStore
import voice.core.playback.MemoryDataStore
import voice.core.playback.SeekTimeStore
import voice.core.playback.history.CommandSourceResolver
import voice.core.playback.history.ListeningHistoryRecorder
import voice.core.playback.history.PlaybackPosition
import voice.core.playback.misc.Decibel
import voice.core.playback.misc.VolumeGain
import voice.core.playback.misc.VolumeGainSetter
import voice.core.playback.session.MediaItemProvider
import voice.core.playback.session.realChapterId
import voice.core.playback.session.search.book
import voice.core.playback.session.toMediaIdOrNull
import voice.core.sleeptimer.SleepTimer
import voice.core.sleeptimer.SleepTimerMode
import voice.core.sleeptimer.SleepTimerState
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

@RunWith(AndroidJUnit4::class)
class VoicePlayerTest {

  private val seekTimeStore = MemoryDataStore(2)
  private val autoRewindAmountStore = MemoryDataStore(2)
  private val lastPlaybackSpeedStore = MemoryDataStore(1F)

  private val internalPlayer = TestExoPlayerBuilder(ApplicationProvider.getApplicationContext())
    .setMaxSeekToPreviousPositionMs(2_000)
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
  private val currentBookStore = MemoryDataStore<BookId?>(bookId)
  private lateinit var currentBook: Book
  private val sleepTimer = FakeSleepTimer()
  private val historyEvents = mutableListOf<ListeningEvent>()
  private val historyRepo = mockk<ListeningHistoryRepo> {
    coEvery { add(capture(historyEvents)) } just Runs
  }
  private val analytics = mockk<Analytics>(relaxUnitFun = true)
  private val chapterMarkPlayer = ChapterMarkPlayer(internalPlayer, mediaItemProvider)
  private val player = VoicePlayer(
    player = chapterMarkPlayer,
    repo = mockk {
      coEvery { get(bookId) } answers { currentBook }
      coEvery { updateBook(bookId, any()) } answers {
        currentBook = currentBook.update(secondArg<(BookContent) -> BookContent>())
      }
    },
    currentBookStoreId = CurrentBookStore(currentBookStore),
    seekTimeStore = SeekTimeStore(seekTimeStore),
    autoRewindAmountStore = AutoRewindAmountStore(autoRewindAmountStore),
    lastPlaybackSpeedStore = LastPlaybackSpeedStore(lastPlaybackSpeedStore),
    scope = scope,
    volumeGain = VolumeGain(VolumeGainSetter()),
    sleepTimer = sleepTimer,
    analytics = analytics,
    historyRecorder = ListeningHistoryRecorder(
      repo = historyRepo,
      clock = Clock.fixed(Instant.parse("2026-10-07T10:00:00Z"), ZoneOffset.UTC),
      scope = scope,
    ),
    commandSourceResolver = CommandSourceResolver(ApplicationProvider.getApplicationContext()),
  )

  @Test
  fun `seekToNext carries over into the next chapter mark`() = scope.runTest {
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

    // the first mark ends at 19_999, so the remainder carries over into the second one
    player.seekToNext()
    player.shouldHavePosition(1, 1_001)

    player.seekToNext()
    player.shouldHavePosition(1, 8_001)

    player.seekToNext()
    player.shouldHavePosition(2, 5_002)

    player.seekToNext()
    player.shouldHavePosition(2, 12_002)
  }

  @Test
  fun `seekToPrevious carries over into the previous chapter mark`() = scope.runTest {
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

    // the mark only spans 7s, so the seek is clamped to its end
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

    player.seekTo(3, 5_000)
    player.forceSeekToNext()
    player.shouldHavePosition(3, 5_000)
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
  fun `setBook starts a book that was never played at the last speed and keeps it`() = scope.runTest {
    lastPlaybackSpeedStore.updateData { 1.5F }

    setMediaItems(listOf(chapter(ChapterMark(startMs = 0, endMs = 20_000, name = null))))

    assertEquals(expected = 1.5F, actual = internalPlayer.playbackParameters.speed)
    assertEquals(expected = 1.5F, actual = currentBook.content.playbackSpeed)
  }

  @Test
  fun `setBook starts a played book at its own speed`() = scope.runTest {
    lastPlaybackSpeedStore.updateData { 1.5F }

    setMediaItems(
      listOf(chapter(ChapterMark(startMs = 0, endMs = 20_000, name = null))),
      playbackSpeed = 1.25F,
      lastPlayedAt = Instant.ofEpochSecond(1),
    )

    assertEquals(expected = 1.25F, actual = internalPlayer.playbackParameters.speed)
    assertEquals(expected = 1.25F, actual = currentBook.content.playbackSpeed)
  }

  @Test
  fun `playing marks the book that plays as played, not the current one`() = scope.runTest {
    setMediaItems(listOf(chapter(ChapterMark(startMs = 0, endMs = 20_000, name = null))))
    currentBookStore.updateData { BookId("a book that was only opened") }

    player.play()
    runCurrent()

    assertNotEquals(illegal = Instant.EPOCH, actual = currentBook.content.lastPlayedAt)
  }

  @Test
  fun `playing remembers the speed for new books`() = scope.runTest {
    setMediaItems(
      listOf(chapter(ChapterMark(startMs = 0, endMs = 20_000, name = null))),
      playbackSpeed = 1.75F,
      lastPlayedAt = Instant.ofEpochSecond(1),
    )

    player.play()
    runCurrent()

    assertEquals(expected = 1.75F, actual = lastPlaybackSpeedStore.data.first())
  }

  @Test
  fun `changing the speed remembers it for new books`() = scope.runTest {
    setMediaItems(listOf(chapter(ChapterMark(startMs = 0, endMs = 20_000, name = null))))

    player.setPlaybackSpeed(1.25F)
    runCurrent()

    assertEquals(expected = 1.25F, actual = lastPlaybackSpeedStore.data.first())
  }

  @Test
  fun `loading a book does not change the speed for new books`() = scope.runTest {
    setMediaItems(
      listOf(chapter(ChapterMark(startMs = 0, endMs = 20_000, name = null))),
      playbackSpeed = 1.75F,
      lastPlayedAt = Instant.ofEpochSecond(1),
    )

    assertEquals(expected = 1F, actual = lastPlaybackSpeedStore.data.first())
  }

  @Test
  fun `playing tells whether volume boost is on`() = scope.runTest {
    setMediaItems(listOf(chapter(ChapterMark(startMs = 0, endMs = 20_000, name = null))), gain = 3F)

    player.play()
    player.setGain(Decibel.Zero)
    player.play()

    verifyOrder {
      analytics.event("play", mapOf("volume_boost" to "on"))
      analytics.event("play", mapOf("volume_boost" to "off"))
    }
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
  fun `end of chapter sleep timer keeps a finished book at its end`() = scope.runTest {
    setMediaItems(listOf(chapter(ChapterMark(startMs = 0, endMs = 10_000, name = null))))
    autoRewindAmountStore.updateData { 5 }

    player.seekTo(0, 9_000)
    player.prepare()
    awaitReady()
    sleepTimer.enable(SleepTimerMode.EndOfChapter)

    TestPlayerRunHelper.play(internalPlayer).untilPlayWhenReadyIs(false)
    shadowOf(Looper.getMainLooper()).idle()

    player.shouldHavePosition(0, player.duration)
  }

  @Test
  fun `end of chapter sleep timer records once where it paused`() = scope.runTest {
    val chapter = chapter(
      ChapterMark(startMs = 0, endMs = 1_000, name = null),
      ChapterMark(startMs = 1_000, endMs = 2_000, name = null),
    )
    setMediaItems(listOf(chapter))

    player.prepare()
    awaitReady()
    sleepTimer.enable(SleepTimerMode.EndOfChapter)

    TestPlayerRunHelper.play(internalPlayer).untilPlayWhenReadyIs(false)
    shadowOf(Looper.getMainLooper()).idle()
    advanceUntilIdle()

    assertEquals(
      expected = listOf(ListeningEvent.Type.SleepTimerEnded to ListeningEvent.Source.SleepTimer),
      actual = historyEvents.map { it.type to it.source },
    )
    assertEquals(
      expected = PlaybackPosition(bookId, chapter.id, 1_000),
      actual = historyEvents.single().let { PlaybackPosition(it.bookId, it.chapterId, it.time) },
    )
  }

  @Test
  fun `end of chapter sleep timer survives skipping to the next chapter`() = scope.runTest {
    setMediaItems(
      listOf(
        chapter(
          ChapterMark(startMs = 0, endMs = 4_999, name = null),
          ChapterMark(startMs = 5_000, endMs = 9_999, name = null),
          ChapterMark(startMs = 10_000, endMs = 20_000, name = null),
        ),
      ),
    )
    player.prepare()
    awaitReady()
    sleepTimer.enable(SleepTimerMode.EndOfChapter)

    player.forceSeekToNext()
    player.shouldHavePosition(1, 0)
    awaitReady()
    shadowOf(Looper.getMainLooper()).idle()
    scope.advanceUntilIdle()

    assertEquals(expected = SleepTimerState.Enabled.WithEndOfChapter, actual = sleepTimer.state.value)
  }

  @Test
  fun `end of chapter sleep timer keeps the silence at the end of the chapter`() = scope.runTest {
    setMediaItems(
      listOf(
        chapter(
          ChapterMark(startMs = 0, endMs = 9_999, name = null),
          ChapterMark(startMs = 10_000, endMs = 20_000, name = null),
        ),
      ),
    )
    player.setSkipSilenceEnabled(true)
    player.prepare()
    awaitReady()
    sleepTimer.enable(SleepTimerMode.EndOfChapter)

    TestPlayerRunHelper.play(internalPlayer).untilPositionAtLeast(0, 3_000)
    assertTrue(internalPlayer.skipSilenceEnabled)

    TestPlayerRunHelper.advance(internalPlayer).untilPositionAtLeast(0, 6_000)
    assertFalse(internalPlayer.skipSilenceEnabled)

    TestPlayerRunHelper.advance(internalPlayer).untilPlayWhenReadyIs(false)
    player.shouldHavePosition(1, 0)
    assertTrue(internalPlayer.skipSilenceEnabled)
  }

  @Test
  fun `skip silence stays on at the end of the chapter without the end of chapter sleep timer`() = scope.runTest {
    setMediaItems(
      listOf(
        chapter(
          ChapterMark(startMs = 0, endMs = 9_999, name = null),
          ChapterMark(startMs = 10_000, endMs = 20_000, name = null),
        ),
      ),
    )
    player.setSkipSilenceEnabled(true)
    player.prepare()
    awaitReady()

    TestPlayerRunHelper.play(internalPlayer).untilPositionAtLeast(0, 6_000)

    assertTrue(internalPlayer.skipSilenceEnabled)
  }

  @Test
  fun `skip silence comes back in the next chapter when the end of chapter sleep timer was disabled`() = scope.runTest {
    setMediaItems(
      listOf(
        chapter(
          ChapterMark(startMs = 0, endMs = 9_999, name = null),
          ChapterMark(startMs = 10_000, endMs = 20_000, name = null),
        ),
      ),
    )
    player.setSkipSilenceEnabled(true)
    player.prepare()
    awaitReady()
    sleepTimer.enable(SleepTimerMode.EndOfChapter)

    TestPlayerRunHelper.play(internalPlayer).untilPositionAtLeast(0, 6_000)
    sleepTimer.disable()
    TestPlayerRunHelper.advance(internalPlayer).untilPositionAtLeast(0, 11_000)

    assertTrue(player.playWhenReady)
    assertTrue(internalPlayer.skipSilenceEnabled)
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

    player.play()
    player.pause()

    player.shouldHavePosition(1, 0)
  }

  @Test
  fun `pausing while already paused does not rewind again`() = scope.runTest {
    setMediaItems(listOf(chapter(ChapterMark(startMs = 0, endMs = 20_000, name = null))))
    autoRewindAmountStore.updateData { 5 }

    player.seekTo(0, 15_000)
    player.prepare()
    awaitReady()

    player.play()
    player.pause()
    player.shouldHavePosition(0, 10_000)

    player.pause()
    player.shouldHavePosition(0, 10_000)
  }

  @Test
  fun `pausing a finished book keeps it at its end`() = scope.runTest {
    setMediaItems(listOf(chapter(ChapterMark(startMs = 0, endMs = 10_000, name = null))))
    autoRewindAmountStore.updateData { 5 }

    player.seekTo(0, 9_000)
    player.prepare()
    awaitReady()
    TestPlayerRunHelper.play(internalPlayer).untilState(Player.STATE_ENDED)

    player.pause()
    shadowOf(Looper.getMainLooper()).idle()

    player.shouldHavePosition(0, player.duration)
  }

  @Test
  fun `unplugging headphones rewinds`() = scope.runTest {
    setMediaItems(listOf(chapter(ChapterMark(startMs = 0, endMs = 20_000, name = null))))
    autoRewindAmountStore.updateData { 5 }
    internalPlayer.setHandleAudioBecomingNoisy(true)

    player.seekTo(0, 8_000)
    player.prepare()
    awaitReady()
    player.play()
    TestPlayerRunHelper.advance(internalPlayer).untilPendingCommandsAreFullyHandled()

    ApplicationProvider.getApplicationContext<Context>().sendBroadcast(Intent(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
    TestPlayerRunHelper.advance(internalPlayer).untilPlayWhenReadyIs(false)
    shadowOf(Looper.getMainLooper()).idle()
    val pausedAt = player.currentPosition

    player.shouldHavePosition(0, pausedAt - 5_000)
  }

  private fun TestScope.setMediaItems(
    chapters: List<Chapter>,
    currentChapter: Chapter = chapters.first(),
    positionInChapter: Long = 0,
    playbackSpeed: Float = 1F,
    lastPlayedAt: Instant = Instant.EPOCH,
    gain: Float = 0F,
  ) {
    currentBook = book(chapters, bookId).update {
      it.copy(
        currentChapter = currentChapter.id,
        positionInChapter = positionInChapter,
        playbackSpeed = playbackSpeed,
        lastPlayedAt = lastPlayedAt,
        gain = gain,
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
}
