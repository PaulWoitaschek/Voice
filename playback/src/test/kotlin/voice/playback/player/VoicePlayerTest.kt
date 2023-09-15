package voice.playback.player

import androidx.media3.common.AdPlaybackState
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.test.utils.FakeMediaSource
import androidx.media3.test.utils.FakeTimeline
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.paulwoitaschek.flowpref.inmemory.InMemoryPref
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import voice.common.BookId
import voice.data.Book
import voice.data.Chapter
import voice.data.ChapterId
import voice.data.ChapterMark
import voice.data.MarkData
import voice.logging.core.LogWriter
import voice.logging.core.Logger
import voice.playback.session.MediaId
import voice.playback.session.MediaItemProvider
import voice.playback.session.search.book
import voice.playback.session.toMediaIdOrNull
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
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

  private val seekTimePref = InMemoryPref(2)

  private val internalPlayer = TestExoPlayerBuilder(ApplicationProvider.getApplicationContext())
    .setMediaSourceFactory(
      mockk {
        every { createMediaSource(any()) } answers {
          val mediaItem = arg<MediaItem>(0)
          val mediaId = mediaItem.mediaId
          val chapter = currentBook.chapters.single {
            it.id == (mediaId.toMediaIdOrNull()!! as MediaId.Chapter).chapterId
          }
          chapter.duration
          FakeMediaSource(
            FakeTimeline(
              FakeTimeline.TimelineWindowDefinition(
                /* periodCount = */
                1,
                /* id = */
                mediaId,
                /* isSeekable = */
                true,
                /* isDynamic = */
                false,
                /* isLive = */
                false,
                /* isPlaceholder = */
                false,
                /* durationUs = */
                TimeUnit.MILLISECONDS.toMicros(chapter.duration),
                /* defaultPositionUs = */
                0,
                /* windowOffsetInFirstPeriodUs = */
                0,
                /* adPlaybackStates = */
                listOf(AdPlaybackState.NONE),
                /* mediaItem = */
                mediaItem,
              ),
            ),
          )
        }
      },
    )
    .build()

  private val scope = TestScope()
  private val mediaItemProvider = MediaItemProvider(mockk(), mockk(), mockk(), mockk(), mockk(), mockk())
  private val bookId = BookId(UUID.randomUUID().toString())
  private lateinit var currentBook: Book
  private val player = VoicePlayer(
    player = internalPlayer,
    repo = mockk {
      coEvery { get(bookId) } answers { currentBook }
      coEvery { updateBook(any(), any()) } just Runs
    },
    currentBookId = mockk {
      every { data } returns flowOf(bookId)
    },
    seekTimePref = seekTimePref,
    autoRewindAmountPref = mockk(),
    scope = scope,
    chapterRepo = mockk {
      coEvery { this@mockk.get(any()) } answers {
        currentBook.chapters.single { it.id == firstArg() }
      }
    },
    mediaItemProvider = mediaItemProvider,
    volumeGain = mockk(relaxed = true),
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

    seekTimePref.value = 7

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

    seekTimePref.value = 5

    player.seekTo(1, 12_000)
    player.prepare()
    awaitReady()

    player.shouldHavePosition(1, 12_000)

    player.seekToPrevious()
    player.shouldHavePosition(1, 7_000)

    player.seekToPrevious()
    player.shouldHavePosition(1, 2_000)

    player.seekToPrevious()
    player.shouldHavePosition(0, 9_000)

    player.seekToPrevious()
    player.shouldHavePosition(0, 4_000)

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
    player.shouldHavePosition(0, 12_000)

    player.forceSeekToNext()
    player.shouldHavePosition(1, 0)

    player.forceSeekToNext()
    player.shouldHavePosition(1, 12_000)

    player.forceSeekToNext()
    player.shouldHavePosition(1, 12_000)
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

    player.seekTo(1, 18_000)
    player.prepare()
    awaitReady()
    player.shouldHavePosition(1, 18_000)

    player.forceSeekToPrevious()
    player.shouldHavePosition(1, 12_000)

    player.forceSeekToPrevious()
    player.shouldHavePosition(1, 0)

    player.forceSeekToPrevious()
    player.shouldHavePosition(0, 12_000)

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

    player.seekTo(1, 13_000)
    player.prepare()
    awaitReady()
    player.shouldHavePosition(1, 13_000)

    player.forceSeekToPrevious()
    player.shouldHavePosition(1, 0)

    player.seekTo(1, 1_000)
    player.forceSeekToPrevious()
    player.shouldHavePosition(0, 12_000)
  }

  private fun TestScope.setMediaItems(chapters: List<Chapter>) {
    currentBook = book(chapters, bookId)
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

    player.seekTo(1, 15_000)
    player.prepare()
    awaitReady()
    player.shouldHavePosition(1, 15_000)

    player.forceSeekToPrevious()
    player.shouldHavePosition(1, 12_000)

    player.seekTo(1, 5_000)
    player.forceSeekToPrevious()
    player.shouldHavePosition(1, 0)
  }

  private fun chapter(vararg marks: ChapterMark): Chapter {
    return Chapter(
      id = ChapterId(UUID.randomUUID().toString()),
      name = "chapter",
      duration = marks.maxOf { it.endMs },
      fileLastModified = Instant.EPOCH,
      markData = marks.map {
        MarkData(it.startMs, it.name ?: "mark ")
      },
    )
  }

  private fun awaitReady() {
    TestPlayerRunHelper.runUntilPlaybackState(internalPlayer, Player.STATE_READY)
  }

  private fun Player.shouldHavePosition(
    currentMediaItemIndex: Int,
    currentPosition: Long,
  ): Player {
    scope.advanceUntilIdle()
    this should havePosition(currentMediaItemIndex, currentPosition)
    return this
  }
}

private fun havePosition(
  currentMediaItemIndex: Int,
  currentPosition: Long,
) = object : io.kotest.matchers.Matcher<Player> {
  override fun test(value: Player): MatcherResult {
    val actualCurrentMediaItemIndex = value.currentMediaItemIndex
    val actualCurrentPosition = value.currentPosition
    return MatcherResult(
      passed = actualCurrentMediaItemIndex == currentMediaItemIndex && actualCurrentPosition == currentPosition,
      failureMessageFn = {
        "position was ($actualCurrentMediaItemIndex,$actualCurrentPosition) but we expected ($currentMediaItemIndex,$currentPosition)"
      },
      negatedFailureMessageFn = { "position should not be ($actualCurrentMediaItemIndex,$actualCurrentPosition)" },
    )
  }
}
