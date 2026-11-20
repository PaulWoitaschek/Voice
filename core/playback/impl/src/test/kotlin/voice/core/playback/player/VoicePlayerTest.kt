package voice.core.playback.player

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.Util
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
import io.mockk.verify
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.Uuid
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
import voice.core.data.ListeningTally
import voice.core.data.MarkData
import voice.core.data.repo.ListeningHistoryRepo
import voice.core.data.store.CurrentBookStore
import voice.core.playback.AutoRewindAmountStore
import voice.core.playback.LastPlaybackSpeedStore
import voice.core.playback.MemoryDataStore
import voice.core.playback.SeekTimeStore
import voice.core.playback.history.AutomaticPauseRecorder
import voice.core.playback.history.CommandSource
import voice.core.playback.history.CommandSourceResolver
import voice.core.playback.history.ListeningHistoryRecorder
import voice.core.playback.history.PlaybackPosition
import voice.core.playback.misc.Decibel
import voice.core.playback.misc.VolumeGain
import voice.core.playback.misc.VolumeGainSetter
import voice.core.playback.nextChapterTarget
import voice.core.playback.previousChapterIndex
import voice.core.playback.session.MediaItemProvider
import voice.core.playback.session.realChapterId
import voice.core.playback.session.search.book
import voice.core.playback.session.toMediaIdOrNull
import voice.core.playback.stats.ListeningSessionRecorder
import voice.core.playback.stats.ListeningTallyRecorder
import voice.core.playback.stats.MemoryListeningStatsRepo
import voice.core.sleeptimer.SleepTimer
import voice.core.sleeptimer.SleepTimerMode
import voice.core.sleeptimer.SleepTimerState

@RunWith(AndroidJUnit4::class)
class VoicePlayerTest {

  private val seekTimeStore = MemoryDataStore(2.seconds)
  private val autoRewindAmountStore = MemoryDataStore(2.seconds)
  private val lastPlaybackSpeedStore = MemoryDataStore(1F)

  private val internalPlayer =
    TestExoPlayerBuilder(ApplicationProvider.getApplicationContext())
      .setMaxSeekToPreviousPositionMs(2_000)
      .setMediaSourceFactory(
        mockk {
          every { createMediaSource(any()) } answers
            {
              val mediaItem = arg<MediaItem>(0)
              val mediaId = mediaItem.mediaId
              val chapter =
                currentBook.chapters.single {
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
  private val mediaItemProvider =
    MediaItemProvider(mockk(), mockk(), mockk(relaxed = true), mockk())
  private val bookId = BookId(Uuid.random().toString())
  private val currentBookStore = MemoryDataStore<BookId?>(bookId)
  private lateinit var currentBook: Book
  private val sleepTimer = FakeSleepTimer()
  private val historyEvents = mutableListOf<ListeningEvent>()
  private val historyRepo =
    mockk<ListeningHistoryRepo> {
      coEvery { add(capture(historyEvents)) } just Runs
    }
  private val plays = mutableListOf<Map<String, String>>()
  private val analytics =
    mockk<Analytics>(relaxUnitFun = true) {
      every { event("play", capture(plays)) } just Runs
    }
  private val chapterMarkPlayer = ChapterMarkPlayer(internalPlayer)
  private val commandSourceResolver =
    mockk<CommandSourceResolver> { every { current() } returns null }
  private val clock =
    object : Clock {
      override fun now(): Instant = Instant.parse("2026-10-07T10:00:00Z")
    }
  private val statsRepo = MemoryListeningStatsRepo()
  private val historyRecorder =
    ListeningHistoryRecorder(
      repo = historyRepo,
      clock = clock,
      scope = scope,
      tally =
        ListeningTallyRecorder(
          repo = statsRepo,
          clock = clock,
          zone = { ZoneId.of("Europe/Berlin") },
          scope = scope,
        ),
    )
  private val playPlaces = mutableListOf<ListeningEvent.Source?>()
  private val listeningSessionRecorder =
    mockk<ListeningSessionRecorder> {
      every { playPressed(captureNullable(playPlaces)) } just Runs
    }
  private val player =
    VoicePlayer(
      player = chapterMarkPlayer,
      repo =
        mockk {
          coEvery { updateBook(any(), any()) } answers
            {
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
      historyRecorder = historyRecorder,
      commandSourceResolver = commandSourceResolver,
      listeningSessionRecorder = listeningSessionRecorder,
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

    seekTimeStore.updateData { 7.seconds }

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

    seekTimeStore.updateData { 5.seconds }

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
  fun `next goes to the start of the next chapter`() = scope.runTest {
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

    player.next()
    player.shouldHavePosition(1, 0)

    player.next()
    player.shouldHavePosition(2, 0)

    player.next()
    player.shouldHavePosition(3, 0)
  }

  @Test
  fun `last chapter, playing, next goes to the end, completed, playback stops`() = scope.runTest {
    setMediaItems(listOf(twelveChapters()))
    player.seekTo(11, 5_000)
    player.prepare()
    awaitReady()
    player.play()

    player.next()
    TestPlayerRunHelper.advance(internalPlayer).untilPendingCommandsAreFullyHandled()
    player.shouldHavePosition(11, 19_999)

    TestPlayerRunHelper.advance(internalPlayer).untilState(Player.STATE_ENDED)
    shadowOf(Looper.getMainLooper()).idle()
    player.shouldHavePosition(11, 19_999)
    assertFalse(player.isPlaying)
  }

  @Test
  fun `last chapter, already at its end, next does nothing`() = scope.runTest {
    setMediaItems(listOf(twelveChapters()))
    player.seekTo(11, 19_999)
    player.prepare()
    awaitReady()

    assertNull(player.nextChapterTarget())
  }

  @Test
  fun `last chapter, next on headphones or in the car skips by the skip amount`() = scope.runTest {
    setMediaItems(listOf(twelveChapters()))
    seekTimeStore.updateData { 7.seconds }
    player.seekTo(11, 5_000)
    player.prepare()
    awaitReady()

    player.seekToNext()
    player.shouldHavePosition(11, 12_000)

    player.seekToNextMediaItem()
    player.shouldHavePosition(11, 19_000)
  }

  @Test
  fun `last chapter, paused, next goes to the end, completed, stays paused`() = scope.runTest {
    setMediaItems(listOf(twelveChapters()))
    player.seekTo(11, 5_000)
    player.prepare()
    awaitReady()

    player.next()
    TestPlayerRunHelper.runUntilPendingCommandsAreFullyHandled(internalPlayer)
    shadowOf(Looper.getMainLooper()).idle()

    player.shouldHavePosition(11, 19_999)
    assertFalse(player.playWhenReady)
  }

  @Test
  fun `last chapter, end of chapter sleep timer on, next goes to the end, playback stops, the timer turns off`() =
    scope.runTest {
      setMediaItems(listOf(twelveChapters()))
      player.seekTo(11, 5_000)
      player.prepare()
      awaitReady()
      sleepTimer.enable(SleepTimerMode.EndOfChapter)
      player.play()

      player.next()
      TestPlayerRunHelper.advance(internalPlayer).untilPendingCommandsAreFullyHandled()
      player.shouldHavePosition(11, 19_999)

      TestPlayerRunHelper.advance(internalPlayer).untilPlayWhenReadyIs(false)
      shadowOf(Looper.getMainLooper()).idle()

      player.shouldHavePosition(11, 19_999)
      assertFalse(player.isPlaying)
      assertEquals(expected = SleepTimerState.Disabled, actual = sleepTimer.state.value)
    }

  @Test
  fun `last chapter, 30 seconds before the end, skip forward goes to 10 seconds before the end`() =
    scope.runTest {
      setMediaItems(twoChaptersOfAMinute())
      seekTimeStore.updateData { 20.seconds }
      player.seekTo(1, 29_999)
      player.prepare()
      awaitReady()

      player.seekForward()

      player.shouldHavePosition(1, 49_999)
    }

  @Test
  fun `last chapter, 10 seconds before the end, playing, skip forward goes to the end, completed, playback stops`() =
    scope.runTest {
      AutomaticPauseRecorder(historyRecorder).attachTo(player)
      setMediaItems(twoChaptersOfAMinute())
      seekTimeStore.updateData { 20.seconds }
      player.seekTo(1, 49_999)
      player.prepare()
      awaitReady()
      player.play()

      player.seekForward()
      player.shouldHavePosition(1, 59_999)

      TestPlayerRunHelper.advance(internalPlayer).untilState(Player.STATE_ENDED)
      shadowOf(Looper.getMainLooper()).idle()
      advanceUntilIdle()
      player.shouldHavePosition(1, 59_999)
      assertFalse(player.isPlaying)
      assertEquals(
        expected = listOf(ListeningEvent.Type.Pause to 59_999L),
        actual = historyEvents.map { it.type to it.time },
      )
    }

  @Test
  fun `last chapter, 10 seconds before the end, paused, skip forward goes to the end, completed, stays paused`() =
    scope.runTest {
      setMediaItems(twoChaptersOfAMinute())
      seekTimeStore.updateData { 20.seconds }
      player.seekTo(1, 49_999)
      player.prepare()
      awaitReady()

      player.seekForward()
      player.shouldHavePosition(1, 59_999)

      TestPlayerRunHelper.runUntilPendingCommandsAreFullyHandled(internalPlayer)
      shadowOf(Looper.getMainLooper()).idle()
      player.shouldHavePosition(1, 59_999)
      assertFalse(player.playWhenReady)
    }

  @Test
  fun `last chapter, 10 seconds before the end, end of chapter sleep timer on, skip forward goes to the end, completed, playback stops, the timer turns off`() =
    scope.runTest {
      setMediaItems(twoChaptersOfAMinute())
      seekTimeStore.updateData { 20.seconds }
      player.seekTo(1, 49_999)
      player.prepare()
      awaitReady()
      sleepTimer.enable(SleepTimerMode.EndOfChapter)
      player.play()

      player.seekForward()
      player.shouldHavePosition(1, 59_999)

      TestPlayerRunHelper.advance(internalPlayer).untilPlayWhenReadyIs(false)
      shadowOf(Looper.getMainLooper()).idle()
      player.shouldHavePosition(1, 59_999)
      assertFalse(player.isPlaying)
      assertEquals(expected = SleepTimerState.Disabled, actual = sleepTimer.state.value)
    }

  @Test
  fun `last chapter, at the end of the book, skip forward does nothing`() = scope.runTest {
    AutomaticPauseRecorder(historyRecorder).attachTo(player)
    setMediaItems(twoChaptersOfAMinute())
    seekTimeStore.updateData { 20.seconds }
    player.seekTo(1, 49_999)
    player.prepare()
    awaitReady()
    player.play()
    player.seekForward()
    TestPlayerRunHelper.advance(internalPlayer).untilState(Player.STATE_ENDED)
    shadowOf(Looper.getMainLooper()).idle()
    advanceUntilIdle()
    val eventsAtTheEnd = historyEvents.toList()

    player.seekToNext()
    TestPlayerRunHelper.runUntilPendingCommandsAreFullyHandled(internalPlayer)
    shadowOf(Looper.getMainLooper()).idle()
    advanceUntilIdle()

    player.shouldHavePosition(1, 59_999)
    assertEquals(expected = Player.STATE_ENDED, actual = player.playbackState)
    assertEquals(expected = eventsAtTheEnd, actual = historyEvents)
  }

  @Test
  fun `last chapter, 10 seconds before the end, next on headphones goes to the end, completed, playback stops, no jump back`() =
    scope.runTest {
      val chapters = twoChaptersOfAMinute()
      setMediaItems(chapters, currentChapter = chapters[1], positionInChapter = 49_999)
      seekTimeStore.updateData { 20.seconds }
      player.prepare()
      awaitReady()
      every { commandSourceResolver.current() } returns
        CommandSource(ListeningEvent.Source.Bluetooth)
      player.play()

      player.seekToNext()
      player.shouldHavePosition(1, 59_999)

      TestPlayerRunHelper.advance(internalPlayer).untilState(Player.STATE_ENDED)
      shadowOf(Looper.getMainLooper()).idle()
      player.shouldHavePosition(1, 59_999)
      assertFalse(player.isPlaying)
      assertEquals(
        expected = listOf(ListeningEvent.Type.Play, ListeningEvent.Type.SkipForward),
        actual = historyEvents.map { it.type },
      )
      assertNull(historyRecorder.lastJump.value)
    }

  @Test
  fun `previous restarts the chapter and then goes back chapter by chapter`() = scope.runTest {
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

    player.seekTo(player.previousChapterIndex(), 0)
    player.shouldHavePosition(3, 0)

    player.seekTo(player.previousChapterIndex(), 0)
    player.shouldHavePosition(2, 0)

    player.seekTo(player.previousChapterIndex(), 0)
    player.shouldHavePosition(1, 0)

    player.seekTo(player.previousChapterIndex(), 0)
    player.shouldHavePosition(0, 0)
  }

  @Test
  fun `previous goes to the previous chapter when in the 2s window`() = scope.runTest {
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

    player.seekTo(player.previousChapterIndex(), 0)
    player.shouldHavePosition(1, 0)

    player.seekTo(1, 1_000)
    player.seekTo(player.previousChapterIndex(), 0)
    player.shouldHavePosition(0, 0)
  }

  @Test
  fun `the chapter marks start in the mark where the book stopped`() = scope.runTest {
    val chapter =
      chapter(
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
  fun `a chapter mark picked from the list starts at that mark`() = scope.runTest {
    val chapter =
      chapter(
        ChapterMark(startMs = 0, endMs = 11_999, name = null),
        ChapterMark(startMs = 12_000, endMs = 20_000, name = null),
      )
    currentBook = book(listOf(chapter), bookId).update { it.copy(positionInChapter = 15_000) }
    player.setMediaItems(mediaItemProvider.playbackItems(currentBook), 0, 5_000)
    runCurrent()

    player.prepare()
    awaitReady()

    player.shouldHavePosition(0, 5_000)
  }

  @Test
  fun `the settings of the book stay when its chapter marks are set`() = scope.runTest {
    setMediaItems(
      listOf(chapter(ChapterMark(startMs = 0, endMs = 20_000, name = null))),
      playbackSpeed = 1.5F,
      skipSilence = true,
    )

    player.prepare()
    awaitReady()

    assertEquals(expected = 1.5F, actual = internalPlayer.playbackParameters.speed)
    assertTrue(internalPlayer.skipSilenceEnabled)
  }

  @Test
  fun `playing marks the book that plays as played, not the current one`() = scope.runTest {
    setMediaItems(listOf(chapter(ChapterMark(startMs = 0, endMs = 20_000, name = null))))
    currentBookStore.updateData { BookId("a book that was only opened") }

    player.play()
    runCurrent()

    assertNotEquals(
      illegal = Instant.fromEpochMilliseconds(0),
      actual = currentBook.content.lastPlayedAt,
    )
  }

  @Test
  fun `playing remembers the speed for new books`() = scope.runTest {
    setMediaItems(
      listOf(chapter(ChapterMark(startMs = 0, endMs = 20_000, name = null))),
      playbackSpeed = 1.75F,
      lastPlayedAt = Instant.fromEpochSeconds(1),
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
      lastPlayedAt = Instant.fromEpochSeconds(1),
    )

    assertEquals(expected = 1F, actual = lastPlaybackSpeedStore.data.first())
  }

  @Test
  fun `playing tells whether volume boost is on`() = scope.runTest {
    setMediaItems(listOf(chapter(ChapterMark(startMs = 0, endMs = 20_000, name = null))), gain = 3F)

    player.play()
    player.pause()
    player.setGain(Decibel.Zero)
    player.play()

    assertEquals(expected = listOf("on", "off"), actual = plays.map { it["volume_boost"] })
  }

  @Test
  fun `player, skip silence on, 1_3x, press play, play with volume boost, skip silence on, faster up to 1_5x, from the app, from a folder`() =
    scope.runTest {
      setMediaItems(oneChapter(), playbackSpeed = 1.3F, skipSilence = true)

      playFromTheApp(ListeningEvent.Source.App)

      assertEquals(
        expected =
          listOf(
            mapOf(
              "volume_boost" to "off",
              "skip_silence" to "on",
              "speed" to "up_to_1_5x",
              "source" to "app",
              "book_from" to "folder",
            ),
          ),
        actual = plays,
      )
    }

  @Test
  fun `player, 1x, skip silence off, press play, play with skip silence off, 1x`() = scope.runTest {
    setMediaItems(oneChapter(), playbackSpeed = 1F, skipSilence = false)

    playFromTheApp(ListeningEvent.Source.App)

    assertEquals(expected = "off", actual = plays.single()["skip_silence"])
    assertEquals(expected = "1x", actual = plays.single()["speed"])
  }

  @Test
  fun `the speed ranges are slower than 1x, 1x, faster up to 1_5x and faster than 1_5x`() =
    scope.runTest {
      setMediaItems(oneChapter())

      listOf(0.95F, 1F, 1.05F, 1.5F, 1.55F).forEach { speed ->
        player.setPlaybackSpeed(speed)
        player.play()
        player.pause()
      }

      assertEquals(
        expected = listOf("slower", "1x", "up_to_1_5x", "up_to_1_5x", "faster"),
        actual = plays.map { it["speed"] },
      )
    }

  @Test
  fun `phone locked, play in the notification, play from the notification`() = scope.runTest {
    setMediaItems(oneChapter())

    playFromOutside(ListeningEvent.Source.Notification)

    assertEquals(expected = "notification", actual = plays.single()["source"])
  }

  @Test
  fun `headphones on, press the headphone button, play from the headset`() = scope.runTest {
    setMediaItems(oneChapter())

    playFromOutside(ListeningEvent.Source.Headset)

    assertEquals(expected = "headset", actual = plays.single()["source"])
  }

  @Test
  fun `android auto, press play in the car, play from the car`() = scope.runTest {
    setMediaItems(oneChapter())

    playFromOutside(ListeningEvent.Source.Car)

    assertEquals(expected = "car", actual = plays.single()["source"])
  }

  @Test
  fun `play from bluetooth, a watch, another app or an unknown controller`() = scope.runTest {
    setMediaItems(oneChapter())

    listOf(
        ListeningEvent.Source.Bluetooth,
        ListeningEvent.Source.Watch,
        ListeningEvent.Source.OtherApp,
        ListeningEvent.Source.Unknown,
      )
      .forEach {
        playFromOutside(it)
        player.pause()
      }

    assertEquals(
      expected = listOf("bluetooth", "watch", "other_app", "other_app"),
      actual = plays.map { it["source"] },
    )
  }

  @Test
  fun `the end of the book, press play, play counts again`() = scope.runTest {
    setMediaItems(oneChapter())
    player.seekTo(0, 19_000)
    player.prepare()
    awaitReady()
    playFromTheApp(ListeningEvent.Source.App)
    TestPlayerRunHelper.advance(internalPlayer).untilState(Player.STATE_ENDED)
    shadowOf(Looper.getMainLooper()).idle()
    assertEquals(expected = Player.STATE_ENDED, actual = player.playbackState)
    assertTrue(player.playWhenReady)

    Util.handlePlayButtonAction(player)

    assertEquals(expected = 2, actual = plays.size)
  }

  @Test
  fun `the book ended, a seek plays on, tap a bookmark while it plays, no second play`() =
    scope.runTest {
      setMediaItems(oneChapter())
      player.seekTo(0, 19_000)
      player.prepare()
      awaitReady()
      playFromTheApp(ListeningEvent.Source.App)
      TestPlayerRunHelper.advance(internalPlayer).untilState(Player.STATE_ENDED)
      shadowOf(Looper.getMainLooper()).idle()

      player.seekTo(0, 5_000)
      awaitReady()
      shadowOf(Looper.getMainLooper()).idle()
      assertTrue(player.isPlaying)
      player.seekTo(0, 10_000)
      player.play()

      assertEquals(expected = 1, actual = plays.size)
    }

  @Test
  fun `android auto, pick another book while one plays, play from the car`() = scope.runTest {
    setMediaItems(oneChapter())
    player.prepare()
    awaitReady()
    playFromTheApp(ListeningEvent.Source.App)
    TestPlayerRunHelper.advance(internalPlayer).untilPendingCommandsAreFullyHandled()

    // like the session, which sets the items of the picked book and then plays
    setMediaItems(oneChapter(), bookId = BookId("another book"))
    player.prepare()
    playFromOutside(ListeningEvent.Source.Car)

    assertEquals(expected = listOf("app", "car"), actual = plays.map { it["source"] })
  }

  @Test
  fun `android auto, a book plays, pick the same book again, no second play`() = scope.runTest {
    setMediaItems(oneChapter())
    player.prepare()
    awaitReady()
    playFromTheApp(ListeningEvent.Source.App)
    TestPlayerRunHelper.advance(internalPlayer).untilPendingCommandsAreFullyHandled()

    // like the session, which sets the items of the picked book and then plays
    val items = mediaItemProvider.mediaItemsWithStartPosition(currentBook)
    player.setMediaItems(items.mediaItems, items.startIndex, items.startPositionMs)
    player.prepare()
    playFromOutside(ListeningEvent.Source.Car)

    assertEquals(expected = 1, actual = plays.size)
  }

  @Test
  fun `after a playback error, press play to retry, play counts again`() = scope.runTest {
    setMediaItems(oneChapter())
    player.prepare()
    awaitReady()
    playFromTheApp(ListeningEvent.Source.App)
    // like an error, stop leaves the player idle while it still wants to play
    player.stop()
    assertEquals(expected = Player.STATE_IDLE, actual = player.playbackState)
    assertTrue(player.playWhenReady)

    Util.handlePlayButtonAction(player)

    assertEquals(expected = 2, actual = plays.size)
  }

  @Test
  fun `no book loaded, play from a headset, no play`() = scope.runTest {
    playFromOutside(ListeningEvent.Source.Headset)

    assertEquals(expected = emptyList(), actual = plays)
  }

  @Test
  fun `home screen, play on the widget, play from the widget`() = scope.runTest {
    setMediaItems(oneChapter())

    playFromTheApp(ListeningEvent.Source.Widget)

    assertEquals(expected = "widget", actual = plays.single()["source"])
  }

  @Test
  fun `shaking the phone after the sleep timer stopped, the timer starts again, play from the sleep timer`() =
    scope.runTest {
      setMediaItems(oneChapter())

      // like the sleep timer, which records that it starts again, and then plays without a record
      historyRecorder.record(
        type = ListeningEvent.Type.SleepTimerExtended,
        source = ListeningEvent.Source.SleepTimer,
        position = startOfTheBook(),
      )
      player.play()

      assertEquals(expected = "sleep_timer", actual = plays.single()["source"])
    }

  @Test
  fun `after a call, voice carries on by itself, nothing`() = scope.runTest {
    setMediaItems(oneChapter())
    internalPlayer.setAudioAttributes(
      AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
        .build(),
      true,
    )
    player.prepare()
    awaitReady()
    playFromTheApp(ListeningEvent.Source.App)
    TestPlayerRunHelper.advance(internalPlayer).untilPendingCommandsAreFullyHandled()
    val audioManager =
      ApplicationProvider.getApplicationContext<Context>()
        .getSystemService(AudioManager::class.java)
    val focusListener = shadowOf(audioManager).lastAudioFocusRequest.listener

    focusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
    TestPlayerRunHelper.advance(internalPlayer).untilPendingCommandsAreFullyHandled()
    assertEquals(
      expected = Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS,
      actual = player.playbackSuppressionReason,
    )
    focusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
    TestPlayerRunHelper.advance(internalPlayer).untilPendingCommandsAreFullyHandled()

    assertTrue(player.isPlaying)
    assertEquals(expected = 1, actual = plays.size)
    assertEquals(
      expected = listOf<ListeningEvent.Source?>(ListeningEvent.Source.App),
      actual = playPlaces,
    )
  }

  @Test
  fun `paused, press play, the time after it counts for the place of the press`() = scope.runTest {
    setMediaItems(oneChapter())

    playFromOutside(ListeningEvent.Source.Headset)
    player.pause()
    playFromOutside(ListeningEvent.Source.Bluetooth)
    player.pause()
    playFromOutside(ListeningEvent.Source.Car)
    player.pause()
    every { commandSourceResolver.current() } returns null
    playFromTheApp(ListeningEvent.Source.App)

    assertEquals(
      expected =
        listOf<ListeningEvent.Source?>(
          ListeningEvent.Source.Headset,
          ListeningEvent.Source.Bluetooth,
          ListeningEvent.Source.Car,
          ListeningEvent.Source.App,
        ),
      actual = playPlaces,
    )
  }

  @Test
  fun `the sleep timer is set to the end of the chapter, the chapter ends, one night`() =
    scope.runTest {
      val chapter =
        chapter(
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
        expected =
          listOf(ListeningTally(LocalDate.parse("2026-10-07"), bookId, "sleep_timer_night", 1)),
        actual = statsRepo.tallies,
      )
    }

  @Test
  fun `analytics on, any of the above, nothing of it is reported`() = scope.runTest {
    val events = mutableListOf<String>()
    val params = mutableListOf<Map<String, String>>()
    every { analytics.event(capture(events), capture(params)) } just Runs
    val chapter =
      chapter(
        ChapterMark(startMs = 0, endMs = 1_000, name = null),
        ChapterMark(startMs = 1_000, endMs = 2_000, name = null),
      )
    setMediaItems(listOf(chapter))
    player.prepare()
    awaitReady()
    sleepTimer.enable(SleepTimerMode.EndOfChapter)

    playFromOutside(ListeningEvent.Source.Headset)
    TestPlayerRunHelper.runUntilPlayWhenReady(internalPlayer, false)
    shadowOf(Looper.getMainLooper()).idle()
    advanceUntilIdle()

    assertEquals(expected = 1, actual = statsRepo.tallies.size)
    assertEquals(
      expected = listOf<ListeningEvent.Source?>(ListeningEvent.Source.Headset),
      actual = playPlaces,
    )
    // the play was reported before the counts came, and nothing else is
    assertEquals(expected = listOf("play"), actual = events)
    assertEquals(
      expected = setOf("volume_boost", "skip_silence", "speed", "source", "book_from"),
      actual = params.single().keys,
    )
    verify(exactly = 0) { analytics.setUserProperty(any(), any()) }
    verify(exactly = 0) { analytics.screenView(any()) }
  }

  @Test
  fun `an audiobookshelf book, press play, play from audiobookshelf`() = scope.runTest {
    setMediaItems(oneChapter(), bookId = BookId("abs://item/an-item"))

    playFromTheApp(ListeningEvent.Source.App)

    assertEquals(expected = "audiobookshelf", actual = plays.single()["book_from"])
  }

  @Test
  fun `bookmarks screen, playing, tap a bookmark, no second play`() = scope.runTest {
    setMediaItems(oneChapter())
    player.prepare()
    awaitReady()
    playFromTheApp(ListeningEvent.Source.App)

    player.seekTo(0, 10_000)
    // the app doesn't record a play while it already plays
    player.play()

    assertEquals(expected = 1, actual = plays.size)
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
    autoRewindAmountStore.updateData { 5.seconds }

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
    val chapter =
      chapter(
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

    player.seekTo(1, 0)
    player.shouldHavePosition(1, 0)
    awaitReady()
    shadowOf(Looper.getMainLooper()).idle()
    scope.advanceUntilIdle()

    assertEquals(
      expected = SleepTimerState.Enabled.WithEndOfChapter,
      actual = sleepTimer.state.value,
    )
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
  fun `skip silence stays on at the end of the chapter without the end of chapter sleep timer`() =
    scope.runTest {
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
  fun `skip silence comes back in the next chapter when the end of chapter sleep timer was disabled`() =
    scope.runTest {
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

    autoRewindAmountStore.updateData { 5.seconds }

    player.seekTo(1, 3_000)
    player.prepare()
    awaitReady()
    player.shouldHavePosition(1, 3_000)

    player.play()
    player.pause()

    player.shouldHavePosition(1, 0)
  }

  @Test
  fun `seeking back crosses into the previous chapter mark`() = scope.runTest {
    setMediaItems(
      listOf(
        chapter(
          ChapterMark(startMs = 0, endMs = 11_999, name = null),
          ChapterMark(startMs = 12_000, endMs = 20_000, name = null),
        ),
      ),
    )
    player.seekTo(1, 3_000)
    player.prepare()
    awaitReady()

    player.seekBackBy(10.seconds, crossMediaItems = true)

    player.shouldHavePosition(0, 4_999)
  }

  @Test
  fun `seeking back past the start of the book stops at its start`() = scope.runTest {
    setMediaItems(
      listOf(
        chapter(
          ChapterMark(startMs = 0, endMs = 11_999, name = null),
          ChapterMark(startMs = 12_000, endMs = 20_000, name = null),
        ),
      ),
    )
    player.seekTo(1, 3_000)
    player.prepare()
    awaitReady()

    player.seekBackBy(30.seconds, crossMediaItems = true)

    player.shouldHavePosition(0, 0)
  }

  @Test
  fun `pausing while already paused does not rewind again`() = scope.runTest {
    setMediaItems(listOf(chapter(ChapterMark(startMs = 0, endMs = 20_000, name = null))))
    autoRewindAmountStore.updateData { 5.seconds }

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
  fun `pausing without auto rewind does not seek`() = scope.runTest {
    setMediaItems(listOf(chapter(ChapterMark(startMs = 0, endMs = 20_000, name = null))))
    autoRewindAmountStore.updateData { 0.seconds }

    player.seekTo(0, 15_000)
    player.prepare()
    awaitReady()

    var seeks = 0
    player.addListener(
      object : Player.Listener {
        override fun onPositionDiscontinuity(
          oldPosition: Player.PositionInfo,
          newPosition: Player.PositionInfo,
          reason: Int,
        ) {
          if (reason == Player.DISCONTINUITY_REASON_SEEK) seeks++
        }
      },
    )
    player.play()
    player.pause()
    scope.advanceUntilIdle()
    shadowOf(Looper.getMainLooper()).idle()

    assertEquals(0, seeks)
  }

  @Test
  fun `pausing a finished book keeps it at its end`() = scope.runTest {
    setMediaItems(listOf(chapter(ChapterMark(startMs = 0, endMs = 10_000, name = null))))
    autoRewindAmountStore.updateData { 5.seconds }

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
    autoRewindAmountStore.updateData { 5.seconds }
    internalPlayer.setHandleAudioBecomingNoisy(true)

    player.seekTo(0, 8_000)
    player.prepare()
    awaitReady()
    player.play()
    TestPlayerRunHelper.advance(internalPlayer).untilPendingCommandsAreFullyHandled()

    ApplicationProvider.getApplicationContext<Context>()
      .sendBroadcast(Intent(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
    TestPlayerRunHelper.advance(internalPlayer).untilPlayWhenReadyIs(false)
    shadowOf(Looper.getMainLooper()).idle()
    val pausedAt = player.currentPosition

    player.shouldHavePosition(0, pausedAt - 5_000)
  }

  private fun oneChapter(): List<Chapter> =
    listOf(chapter(ChapterMark(startMs = 0, endMs = 20_000, name = null)))

  private fun startOfTheBook(): PlaybackPosition =
    PlaybackPosition(currentBook.id, currentBook.content.currentChapter, 0)

  // like PlayerControllerImpl, which records where a play comes from right before it sends it
  private fun playFromTheApp(source: ListeningEvent.Source) {
    historyRecorder.record(ListeningEvent.Type.Play, source, startOfTheBook())
    player.play()
  }

  private fun playFromOutside(source: ListeningEvent.Source) {
    every { commandSourceResolver.current() } returns CommandSource(source)
    player.play()
  }

  private fun TestScope.setMediaItems(
    chapters: List<Chapter>,
    currentChapter: Chapter = chapters.first(),
    positionInChapter: Long = 0,
    playbackSpeed: Float = 1F,
    lastPlayedAt: Instant = Instant.fromEpochMilliseconds(0),
    gain: Float = 0F,
    skipSilence: Boolean = false,
    bookId: BookId = this@VoicePlayerTest.bookId,
  ) {
    currentBook =
      book(chapters, bookId).update {
        it.copy(
          currentChapter = currentChapter.id,
          positionInChapter = positionInChapter,
          playbackSpeed = playbackSpeed,
          lastPlayedAt = lastPlayedAt,
          gain = gain,
          skipSilence = skipSilence,
        )
      }
    player.applySettings(currentBook.content)
    val items = mediaItemProvider.mediaItemsWithStartPosition(currentBook)
    player.setMediaItems(items.mediaItems, items.startIndex, items.startPositionMs)
    runCurrent()
  }

  @Test
  fun `previous restarts the chapter when outside the 2s window`() = scope.runTest {
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

    player.seekTo(player.previousChapterIndex(), 0)
    player.shouldHavePosition(2, 0)

    player.seekTo(2, 1_000)
    player.seekTo(player.previousChapterIndex(), 0)
    player.shouldHavePosition(1, 0)
  }

  private fun chapter(vararg marks: ChapterMark): Chapter {
    return Chapter(
      id = ChapterId(Uuid.random().toString()),
      name = "chapter",
      duration = marks.maxOf { it.endMs },
      fileLastModified = Instant.fromEpochMilliseconds(0),
      markData =
        marks.map {
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

  private fun twelveChapters(): Chapter {
    return chapter(
      *Array(11) { ChapterMark(startMs = it * 1_000L, endMs = it * 1_000L + 999, name = null) },
      ChapterMark(startMs = 11_000, endMs = 31_000, name = null),
    )
  }

  private fun twoChaptersOfAMinute(): List<Chapter> {
    return List(2) { chapter(ChapterMark(startMs = 0, endMs = 60_000, name = null)) }
  }

  private fun Player.next() {
    val (index, positionMs) = nextChapterTarget() ?: return
    seekTo(index, positionMs)
  }
}
