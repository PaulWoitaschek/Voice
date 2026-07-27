package voice.core.data.repo.internals

import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.BookSource
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.toUri
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LocalChapterUriResolverTest {

  private val resolver = LocalChapterUriResolver()

  @Test
  fun `resolves local chapters to their id uri`() {
    val chapter = chapter(id = ChapterId("content://tree/local/chapter1"))
    val content = bookContent(source = BookSource.LOCAL)

    assertEquals(chapter.id.toUri(), resolver.resolve(chapter, content))
  }

  @Test
  fun `defers to other resolvers for non-local chapters`() {
    val chapter = chapter(id = ChapterId("voice-abs://server/item/track/0"))
    val content = bookContent(source = BookSource.AUDIOBOOKSHELF)

    assertNull(resolver.resolve(chapter, content))
  }

  private fun chapter(id: ChapterId): Chapter {
    return Chapter(
      id = id,
      name = "chapter",
      duration = 1000,
      fileLastModified = Instant.EPOCH,
      fileSize = 0,
      markData = emptyList(),
    )
  }

  private fun bookContent(source: BookSource): BookContent {
    val chapterId = ChapterId("content://tree/local/chapter1")
    return BookContent(
      id = BookId("book1"),
      playbackSpeed = 1F,
      skipSilence = false,
      isActive = true,
      lastPlayedAt = Instant.EPOCH,
      author = null,
      name = "book",
      addedAt = Instant.EPOCH,
      chapters = listOf(chapterId),
      currentChapter = chapterId,
      positionInChapter = 0,
      cover = null,
      gain = 0F,
      genre = null,
      narrator = null,
      series = null,
      part = null,
      source = source,
    )
  }
}
