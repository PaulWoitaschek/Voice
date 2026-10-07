package voice.core.playback.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.test.utils.FakeMediaSource
import androidx.media3.test.utils.FakeTimeline
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import org.junit.runner.RunWith
import voice.core.data.Book
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.MarkData
import voice.core.playback.session.MediaItemProvider
import voice.core.playback.session.chapterMarkPlaylist
import voice.core.playback.session.realChapterId
import voice.core.playback.session.search.book
import voice.core.playback.session.toMediaIdOrNull
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

@RunWith(AndroidJUnit4::class)
class ChapterMarkPlayerTest {

  private lateinit var book: Book

  private val exoPlayer = TestExoPlayerBuilder(ApplicationProvider.getApplicationContext())
    .setMediaSourceFactory(
      mockk {
        every { createMediaSource(any()) } answers {
          val mediaItem = arg<MediaItem>(0)
          val chapter = book.chapters.single {
            it.id == mediaItem.mediaId.toMediaIdOrNull()!!.realChapterId
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

  private val mediaItemProvider = MediaItemProvider(mockk(), mockk(), mockk(), mockk(), mockk(), mockk())
  private val player = ChapterMarkPlayer(exoPlayer)

  @Test
  fun `presents one media item per mark while the wrapped player holds one per file`() {
    setBook(listOf(singleFileWithThreeMarks()))

    assertEquals(expected = 1, actual = exoPlayer.mediaItemCount)
    assertEquals(expected = 3, actual = player.mediaItemCount)
    assertEquals(expected = "Intro", actual = player.mediaMetadata.title)
    assertEquals(expected = "Two", actual = player.getMediaItemAt(2).mediaMetadata.title)
  }

  @Test
  fun `seeking reports mark relative positions`() {
    setBook(listOf(singleFileWithThreeMarks()))
    val seeks = recordDiscontinuities(Player.DISCONTINUITY_REASON_SEEK)

    player.seekTo(1, 500)
    TestPlayerRunHelper.runUntilPendingCommandsAreFullyHandled(exoPlayer)

    assertEquals(expected = 1, actual = player.currentMediaItemIndex)
    assertEquals(expected = 500, actual = player.currentPosition)
    assertEquals(expected = 1, actual = seeks.size)
    val (old, new) = seeks.single()
    assertEquals(expected = 0, actual = old.mediaItemIndex)
    assertEquals(expected = 0, actual = old.positionMs)
    assertEquals(expected = 1, actual = new.mediaItemIndex)
    assertEquals(expected = 500, actual = new.positionMs)
  }

  @Test
  fun `seeking to the start of a mark does not report an auto transition`() {
    setBook(listOf(singleFileWithThreeMarks()))
    val autoTransitions = recordDiscontinuities(Player.DISCONTINUITY_REASON_AUTO_TRANSITION)

    player.seekTo(1, 0)
    TestPlayerRunHelper.play(exoPlayer).untilPositionAtLeast(0, 7_000)
    TestPlayerRunHelper.runUntilPendingCommandsAreFullyHandled(exoPlayer)

    assertEquals(expected = 1, actual = player.currentMediaItemIndex)
    assertEquals(expected = emptyList(), actual = autoTransitions)
  }

  @Test
  fun `crossing a mark while playing reports a single auto transition`() {
    setBook(listOf(singleFileWithThreeMarks()))
    val autoTransitions = recordDiscontinuities(Player.DISCONTINUITY_REASON_AUTO_TRANSITION)
    val transitions = mutableListOf<Pair<CharSequence?, Int>>()
    player.addListener(
      object : Player.Listener {
        override fun onMediaItemTransition(
          mediaItem: MediaItem?,
          reason: Int,
        ) {
          transitions += mediaItem?.mediaMetadata?.title to reason
        }
      },
    )

    TestPlayerRunHelper.play(exoPlayer).untilPositionAtLeast(0, 6_000)
    TestPlayerRunHelper.runUntilPendingCommandsAreFullyHandled(exoPlayer)

    assertEquals(expected = 1, actual = player.currentMediaItemIndex)
    assertEquals(expected = 1, actual = autoTransitions.size)
    assertEquals(expected = 0, actual = autoTransitions.single().first.mediaItemIndex)
    assertEquals(expected = 1, actual = autoTransitions.single().second.mediaItemIndex)
    assertEquals(
      expected = listOf<Pair<CharSequence?, Int>>("One" to Player.MEDIA_ITEM_TRANSITION_REASON_AUTO),
      actual = transitions,
    )
  }

  @Test
  fun `crossing a file boundary reports a single auto transition`() {
    val chapters = listOf(
      chapter(duration = 10_000, MarkData(0, "One"), MarkData(5_000, "Two")),
      chapter(duration = 10_000, MarkData(0, "Three"), MarkData(5_000, "Four")),
    )
    setBook(chapters, startItemIndex = 1, positionInItemMs = 4_500)
    val autoTransitions = recordDiscontinuities(Player.DISCONTINUITY_REASON_AUTO_TRANSITION)

    TestPlayerRunHelper.play(exoPlayer).untilPositionAtLeast(1, 0)
    TestPlayerRunHelper.runUntilPendingCommandsAreFullyHandled(exoPlayer)

    assertEquals(expected = 2, actual = player.currentMediaItemIndex)
    assertEquals(expected = "Three", actual = player.mediaMetadata.title)
    assertEquals(expected = 1, actual = autoTransitions.size)
    assertEquals(expected = 1, actual = autoTransitions.single().first.mediaItemIndex)
    assertEquals(expected = 2, actual = autoTransitions.single().second.mediaItemIndex)
    assertEquals(expected = 0, actual = autoTransitions.single().second.positionMs)
  }

  @Test
  fun `advertises seeking between the marks of a single file`() {
    setBook(listOf(singleFileWithThreeMarks()))

    assertTrue(player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))
    assertFalse(player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM))

    player.seekTo(2, 0)
    TestPlayerRunHelper.runUntilPendingCommandsAreFullyHandled(exoPlayer)

    assertFalse(player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))
    assertTrue(player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM))
  }

  private fun recordDiscontinuities(reason: Int): List<Pair<Player.PositionInfo, Player.PositionInfo>> {
    val recorded = mutableListOf<Pair<Player.PositionInfo, Player.PositionInfo>>()
    player.addListener(
      object : Player.Listener {
        override fun onPositionDiscontinuity(
          oldPosition: Player.PositionInfo,
          newPosition: Player.PositionInfo,
          discontinuityReason: Int,
        ) {
          if (discontinuityReason == reason) {
            recorded += oldPosition to newPosition
          }
        }
      },
    )
    return recorded
  }

  private fun setBook(
    chapters: List<Chapter>,
    startItemIndex: Int = 0,
    positionInItemMs: Long = 0,
  ) {
    book = book(chapters)
    player.setBook(
      playlist = book.chapterMarkPlaylist(),
      markMediaItems = mediaItemProvider.playbackItems(book),
      fileMediaItems = mediaItemProvider.chapterMediaItems(book),
      startItemIndex = startItemIndex,
      positionInItemMs = positionInItemMs,
    )
    player.prepare()
    TestPlayerRunHelper.runUntilPlaybackState(exoPlayer, Player.STATE_READY)
  }

  private fun singleFileWithThreeMarks(): Chapter {
    return chapter(
      duration = 15_000,
      MarkData(0, "Intro"),
      MarkData(5_000, "One"),
      MarkData(10_000, "Two"),
    )
  }

  private fun chapter(
    duration: Long,
    vararg marks: MarkData,
  ): Chapter {
    return Chapter(
      id = ChapterId(Uuid.random().toString()),
      name = "file",
      duration = duration,
      fileLastModified = Instant.EPOCH,
      markData = marks.toList(),
      fileSize = 0,
    )
  }
}
