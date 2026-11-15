package voice.core.playback

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.playback.session.MediaId
import voice.core.playback.session.encode

@RunWith(AndroidJUnit4::class)
class NextChapterTargetTest {

  @Test
  fun `last chapter, the book just loaded, next goes to the end`() {
    val bookId = BookId("book")
    val mark =
      MediaId.ChapterMark(
        bookId = bookId,
        chapterId = ChapterId("chapter"),
        markIndex = 11,
        startMs = 11_000,
        endMs = 30_999,
      )
    val player = FakePlayer(bookId, contentPositionMs = 5_000)
    player.update {
      it
        .setPlaylist(
          listOf(
            SimpleBasePlayer.MediaItemData.Builder("mark")
              .setMediaItem(MediaItem.Builder().setMediaId(mark.encode()).build())
              .build(),
          ),
        )
        .setPlaybackState(Player.STATE_IDLE)
    }

    assertEquals(expected = C.TIME_UNSET, actual = player.duration)
    assertEquals(expected = 0 to 19_999L, actual = player.nextChapterTarget())
  }
}
