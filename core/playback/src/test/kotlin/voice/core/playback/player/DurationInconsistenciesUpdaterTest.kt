package voice.core.playback.player

import androidx.media3.common.MediaItem.ClippingConfiguration
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.repo.ChapterRepo
import voice.core.playback.session.MediaId
import voice.core.playback.session.MediaType
import java.time.Instant
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class DurationInconsistenciesUpdaterTest {

  private val scope = TestScope()
  private val chapter = Chapter(
    id = ChapterId("chapter"),
    name = "Chapter",
    duration = 696_276,
    fileLastModified = Instant.EPOCH,
    markData = emptyList(),
    fileSize = 0,
  )
  private val chapterRepo = mockk<ChapterRepo> {
    coEvery { get(chapter.id) } returns chapter
    coEvery { put(any()) } just Runs
  }

  @Test
  fun `updates chapter duration from final mark timeline`() = scope.runTest {
    val markStartMs = 120_000L
    val playerDurationMs = 498_912L
    val player = player(
      startMs = markStartMs,
      endMs = ClippingConfiguration.UNSET.endPositionMs,
      durationMs = playerDurationMs,
    )
    val updater = DurationInconsistenciesUpdater(chapterRepo, scope)
    updater.attachTo(player)

    updater.onTimelineChanged(Timeline.EMPTY, Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE)
    runCurrent()

    coVerify(exactly = 1) {
      chapterRepo.put(chapter.copy(duration = markStartMs + playerDurationMs))
    }
  }

  @Test
  fun `does not update chapter duration from bounded mark timeline`() = scope.runTest {
    val player = player(
      startMs = 120_000,
      endMs = 200_000,
      durationMs = 80_000,
    )
    val updater = DurationInconsistenciesUpdater(chapterRepo, scope)
    updater.attachTo(player)

    updater.onTimelineChanged(Timeline.EMPTY, Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE)
    runCurrent()

    coVerify(exactly = 0) { chapterRepo.put(any()) }
  }

  private fun player(
    startMs: Long,
    endMs: Long,
    durationMs: Long,
  ): Player {
    val clippingConfiguration = ClippingConfiguration.Builder()
      .setStartPositionMs(startMs)
      .apply {
        if (endMs != ClippingConfiguration.UNSET.endPositionMs) {
          setEndPositionMs(endMs)
        }
      }
      .build()
    val mediaItem = voice.core.playback.session.MediaItem(
      title = "Chapter",
      mediaId = MediaId.ChapterMark(
        bookId = BookId("book"),
        chapterId = chapter.id,
        markIndex = 0,
        startMs = startMs,
        endMs = endMs,
      ),
      isPlayable = true,
      browsable = false,
      clippingConfiguration = clippingConfiguration,
      mediaType = MediaType.AudioBookChapter,
    )
    return mockk(relaxed = true) {
      every { currentMediaItem } returns mediaItem
      every { duration } returns durationMs
    }
  }
}
