package voice.core.audiobookshelf.sync

import voice.core.audiobookshelf.AudiobookshelfIds
import voice.core.audiobookshelf.api.AbsBookMedia
import voice.core.audiobookshelf.api.AbsBookMetadata
import voice.core.audiobookshelf.api.AbsChapter
import voice.core.audiobookshelf.api.AbsFileMetadata
import voice.core.audiobookshelf.api.AbsLibraryItem
import voice.core.audiobookshelf.api.AbsSeries
import voice.core.audiobookshelf.api.AbsTrack
import voice.core.data.Book
import voice.core.data.ChapterMark
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class BookMappingTest {

  @Test
  fun `a chapter per file names each file after its chapter`() {
    val item = item(
      tracks = listOf(track(1, 0.0, 54.0, "01.mp3"), track(2, 54.0, 51.0, "02.mp3")),
      chapters = listOf(AbsChapter(0.0, 54.0, "Kapitel 1"), AbsChapter(54.0, 105.0, "Kapitel 2")),
    )

    val chapters = item.chapters()

    assertEquals(listOf("Kapitel 1", "Kapitel 2"), chapters.map { it.name })
    assertEquals(listOf(54_000L, 51_000L), chapters.map { it.duration })
    assertEquals(listOf(ChapterMark("Kapitel 1", 0, 53_999)), chapters.first().chapterMarks)
  }

  @Test
  fun `a single file gets the chapters of the book as its marks`() {
    val item = item(
      tracks = listOf(track(1, 0.0, 107.4, "Das Kapitelbuch.m4b")),
      chapters = listOf(
        AbsChapter(0.0, 28.44, "Teil 1"),
        AbsChapter(28.44, 55.07, "Teil 2"),
        AbsChapter(55.07, 80.82, "Teil 3"),
        AbsChapter(80.82, 107.4, "Teil 4"),
      ),
    )

    val chapter = item.chapters().single()

    assertEquals("Das Kapitelbuch", chapter.name)
    assertEquals(listOf("Teil 1", "Teil 2", "Teil 3", "Teil 4"), chapter.chapterMarks.map { it.name })
    assertEquals(listOf(0L, 28_440L, 55_070L, 80_820L), chapter.chapterMarks.map { it.startMs })
  }

  @Test
  fun `a chapter running into the next file goes on at its start`() {
    val item = item(
      tracks = listOf(track(1, 0.0, 60.0, "a.mp3"), track(2, 60.0, 60.0, "b.mp3")),
      chapters = listOf(AbsChapter(0.0, 90.0, "Long one"), AbsChapter(90.0, 120.0, "Short one")),
    )

    val second = item.chapters()[1]

    assertEquals(listOf("Long one", "Short one"), second.chapterMarks.map { it.name })
    assertEquals(listOf(0L, 30_000L), second.chapterMarks.map { it.startMs })
  }

  @Test
  fun `the tracks keep their order and become stable ids`() {
    val item = item(tracks = listOf(track(2, 10.0, 10.0, "b.mp3", ino = "22"), track(1, 0.0, 10.0, "a.mp3", ino = "11")))

    assertEquals(
      listOf(
        AudiobookshelfIds.chapterId("item", trackIndex = 1, ino = "11"),
        AudiobookshelfIds.chapterId("item", trackIndex = 2, ino = "22"),
      ),
      item.chapters().map { it.id },
    )
  }

  @Test
  fun `the book takes the metadata of the server`() {
    val item = item(
      tracks = listOf(track(1, 0.0, 10.0, "a.mp3")),
      metadata = AbsBookMetadata(
        title = "Title",
        authorName = "Author",
        narratorName = "",
        series = listOf(AbsSeries("Series", "2")),
        genres = listOf("Fantasy", "Adventure"),
      ),
    )

    val content = item.toBookContent(item.chapters(), existing = null)

    assertEquals("Title", content.name)
    assertEquals("Author", content.author)
    assertEquals(null, content.narrator)
    assertEquals("Series", content.series)
    assertEquals("2", content.part)
    assertEquals("Fantasy, Adventure", content.genre)
    assertEquals(Instant.ofEpochMilli(1_000), content.addedAt)
  }

  @Test
  fun `an update keeps where the listener is and how they listen`() {
    val item = item(tracks = listOf(track(1, 0.0, 10.0, "a.mp3"), track(2, 10.0, 10.0, "b.mp3")))
    val chapters = item.chapters()
    val existing = item.toBookContent(chapters, existing = null).copy(
      currentChapter = chapters[1].id,
      positionInChapter = 4_000,
      playbackSpeed = 1.5F,
      lastPlayedAt = Instant.ofEpochMilli(5_000),
    )

    val updated = item.copy(media = item.media.copy(metadata = AbsBookMetadata(title = "Renamed")))
      .toBookContent(chapters, existing)

    assertEquals("Renamed", updated.name)
    assertEquals(chapters[1].id, updated.currentChapter)
    assertEquals(4_000, updated.positionInChapter)
    assertEquals(1.5F, updated.playbackSpeed)
    assertEquals(Instant.ofEpochMilli(5_000), updated.lastPlayedAt)
  }

  @Test
  fun `a position in the book points into the right file`() {
    val item = item(tracks = listOf(track(1, 0.0, 54.0, "a.mp3"), track(2, 54.0, 51.0, "b.mp3")))
    val chapters = item.chapters()
    val book = Book(item.toBookContent(chapters, existing = null), chapters)

    assertEquals(BookPosition(chapters[1].id, 26_000), book.positionAt(80_000))
    assertEquals(BookPosition(chapters[0].id, 0), book.positionAt(-5))
    assertEquals(BookPosition(chapters[1].id, 51_000), book.positionAt(1_000_000))
  }

  @Test
  fun `the last ten seconds count as finished, like on the server`() {
    val item = item(tracks = listOf(track(1, 0.0, 100.0, "a.mp3")))
    val chapters = item.chapters()
    val content = item.toBookContent(chapters, existing = null)

    assertEquals(false, Book(content.copy(positionInChapter = 89_000), chapters).isFinished)
    assertEquals(true, Book(content.copy(positionInChapter = 92_000), chapters).isFinished)
    assertEquals(false, Book(content.copy(positionInChapter = 0), chapters).isFinished)
  }
}

internal fun item(
  tracks: List<AbsTrack>,
  chapters: List<AbsChapter> = emptyList(),
  metadata: AbsBookMetadata = AbsBookMetadata(title = "Book"),
  id: String = "item",
  updatedAt: Long = 2_000,
) = AbsLibraryItem(
  id = id,
  libraryId = "library",
  mediaType = "book",
  addedAt = 1_000,
  updatedAt = updatedAt,
  media = AbsBookMedia(
    metadata = metadata,
    duration = tracks.sumOf { it.duration },
    numTracks = tracks.size,
    tracks = tracks,
    chapters = chapters,
  ),
)

internal fun track(
  index: Int,
  startOffset: Double,
  duration: Double,
  fileName: String,
  ino: String = "ino$index",
) = AbsTrack(
  index = index,
  ino = ino,
  startOffset = startOffset,
  duration = duration,
  metadata = AbsFileMetadata(filename = fileName, size = 1_000),
)
