package voice.core.playback.session

import androidx.media3.common.C
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import org.junit.runner.RunWith
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.MarkData
import voice.core.playback.session.search.book
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class MediaItemProviderTest {

  private val provider = MediaItemProvider(mockk(), mockk(), mockk(), mockk(), mockk(), mockk())

  @Test
  fun `only final mark uses end of source`() {
    val chapter = Chapter(
      id = ChapterId("chapter"),
      name = "Chapter",
      duration = 20_000,
      fileLastModified = Instant.EPOCH,
      markData = listOf(
        MarkData(startMs = 0, name = "Intro"),
        MarkData(startMs = 12_000, name = "Chapter 1"),
      ),
      fileSize = 0,
    )

    val mediaItems = provider.playbackItems(book(listOf(chapter)))

    assertEquals(chapter.chapterMarks.first().endMs, mediaItems.first().clippingConfiguration.endPositionMs)
    assertEquals(C.TIME_END_OF_SOURCE, mediaItems.last().clippingConfiguration.endPositionMs)
  }
}
