package voice.core.scanner

import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.BookMetadata
import voice.core.data.Bookmark
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.repo.BookmarkRepo
import voice.core.data.repo.ChapterRepo
import voice.core.data.repo.ListeningHistoryRepo
import voice.core.data.repo.ListeningStatsRepo
import voice.core.data.store.CurrentBookStore
import java.io.File
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class ProgressCarryOverTest {

  private val bookmarkRepo = mockk<BookmarkRepo>(relaxUnitFun = true) {
    coEvery { bookmarks(any()) } returns emptyList()
  }
  private val listeningHistoryRepo = mockk<ListeningHistoryRepo>(relaxUnitFun = true)
  private val listeningStatsRepo = mockk<ListeningStatsRepo>(relaxUnitFun = true)
  private val currentBookStore = MemoryDataStore<BookId?>(null)
  private val progressCarryOver =
    ProgressCarryOver(bookmarkRepo, listeningHistoryRepo, listeningStatsRepo, CurrentBookStore(currentBookStore))
  private val chapters = mutableMapOf<ChapterId, Chapter>()
  private val chapterRepo = mockk<ChapterRepo> {
    coEvery { get(ids = any()) } answers { firstArg<List<ChapterId>>().mapNotNull { chapters[it] } }
  }

  @Test
  fun `a document has the same key in every folder it is opened through`() {
    val throughParent = documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Book/1.mp3")
    val throughBook = documentUri(tree = "primary:Audiobooks/Book", document = "primary:Audiobooks/Book/1.mp3")
    val singleFile = "content://com.android.externalstorage.documents/document/primary%3AAudiobooks%2FBook%2F1.mp3"

    assertEquals(expected = throughParent.documentKey(), actual = throughBook.documentKey())
    assertEquals(expected = throughParent.documentKey(), actual = singleFile.toUri().documentKey())
  }

  @Test
  fun `a book added through another folder keeps its progress, name and bookmarks`() = runTest {
    val oldChapters = listOf("1.mp3", "2.mp3").map {
      ChapterId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Book/$it"))
    }
    val old = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Book")),
      chapters = oldChapters,
    ).copy(
      customName = "Renamed",
      customCover = File("cover.webp"),
      currentChapter = oldChapters[1],
      positionInChapter = 42,
      lastPlayedAt = Instant.parse("2026-01-01T10:00:00Z"),
    )
    val bookmark = Bookmark(
      bookId = old.id,
      chapterId = oldChapters[1],
      title = "Twist",
      time = 7,
      addedAt = Instant.EPOCH,
      id = Bookmark.Id.random(),
    )
    coEvery { bookmarkRepo.bookmarks(old) } returns listOf(bookmark)
    currentBookStore.updateData { old.id }

    val newChapters = listOf("1.mp3", "2.mp3").map {
      ChapterId(documentUri(tree = "primary:Audiobooks/Book", document = "primary:Audiobooks/Book/$it"))
    }
    val new = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks/Book", document = "primary:Audiobooks/Book")),
      chapters = newChapters,
    )

    val carried = carryOver(new, previous = listOf(old))

    assertEquals(
      expected = new.copy(
        customName = "Renamed",
        customCover = File("cover.webp"),
        currentChapter = newChapters[1],
        positionInChapter = 42,
        lastPlayedAt = Instant.parse("2026-01-01T10:00:00Z"),
      ),
      actual = carried,
    )
    coVerify(exactly = 1) { bookmarkRepo.addBookmark(any()) }
    coVerify { bookmarkRepo.addBookmark(bookmark.copy(bookId = new.id, chapterId = newChapters[1])) }
    assertEquals(expected = new.id, actual = currentBookStore.data.first())
  }

  @Test
  fun `a book added through another folder keeps its listening history`() = runTest {
    val oldChapters = listOf("1.mp3", "2.mp3").map {
      ChapterId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Book/$it"))
    }
    val old = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Book")),
      chapters = oldChapters,
    )

    val newChapters = listOf("1.mp3", "2.mp3").map {
      ChapterId(documentUri(tree = "primary:Audiobooks/Book", document = "primary:Audiobooks/Book/$it"))
    }
    val new = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks/Book", document = "primary:Audiobooks/Book")),
      chapters = newChapters,
    )

    val _ = carryOver(new, previous = listOf(old))

    coVerify {
      listeningHistoryRepo.moveToBook(
        from = listOf(old.id),
        to = new.id,
        chapters = oldChapters.zip(newChapters).toMap(),
      )
    }
  }

  @Test
  fun `a book added through another folder keeps its listening sessions`() = runTest {
    val oldChapters = listOf("1.mp3", "2.mp3").map {
      ChapterId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Book/$it"))
    }
    val old = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Book")),
      chapters = oldChapters,
    )

    val newChapters = listOf("1.mp3", "2.mp3").map {
      ChapterId(documentUri(tree = "primary:Audiobooks/Book", document = "primary:Audiobooks/Book/$it"))
    }
    val new = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks/Book", document = "primary:Audiobooks/Book")),
      chapters = newChapters,
    )

    val _ = carryOver(new, previous = listOf(old))

    coVerify { listeningStatsRepo.moveToBook(from = listOf(old.id), to = new.id) }
  }

  @Test
  fun `a book split out of a bigger one takes the listening history of its chapters`() = runTest {
    val chapters = listOf("Book/1.mp3", "Other/1.mp3").map {
      ChapterId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/$it"))
    }
    val old = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks")),
      chapters = chapters,
    )
    val new = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Book")),
      chapters = chapters.take(1),
    )

    val _ = carryOver(new, previous = listOf(old))

    coVerify {
      listeningHistoryRepo.moveToBook(from = listOf(old.id), to = new.id, chapters = mapOf(chapters[0] to chapters[0]))
    }
  }

  @Test
  fun `a book split out of a bigger one keeps the name from its files`() = runTest {
    val chapters = listOf("Book/1.mp3", "Other/1.mp3").map {
      ChapterId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/$it"))
    }
    val old = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks")),
      chapters = chapters,
    ).copy(customName = "Everything", lastPlayedAt = Instant.parse("2026-01-01T10:00:00Z"))
    val new = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Book")),
      chapters = chapters.take(1),
    )

    val carried = carryOver(new, previous = listOf(old))

    assertEquals(expected = new.copy(lastPlayedAt = old.lastPlayedAt), actual = carried)
  }

  @Test
  fun `the current book moves into a merged book even when another book was played later`() = runTest {
    val chapters = listOf("Dune/1.mp3", "Hyperion/1.mp3").map {
      ChapterId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/$it"))
    }
    val dune = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Dune")),
      chapters = chapters.take(1),
    ).copy(lastPlayedAt = Instant.parse("2026-01-01T10:00:00Z"))
    val hyperion = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Hyperion")),
      chapters = chapters.drop(1),
    ).copy(lastPlayedAt = Instant.parse("2026-01-02T10:00:00Z"))
    currentBookStore.updateData { dune.id }
    val merged = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks")),
      chapters = chapters,
    )

    val carried = carryOver(merged, previous = listOf(dune, hyperion))

    assertEquals(expected = chapters[1], actual = carried.currentChapter)
    assertEquals(expected = merged.id, actual = currentBookStore.data.first())
  }

  @Test
  fun `switching back to a mode moves the current book back`() = runTest {
    val chapter = ChapterId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Dune/1.mp3"))
    val lastPlayedAt = Instant.parse("2026-01-01T10:00:00Z")
    val merged = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks")),
      chapters = listOf(chapter),
    ).copy(lastPlayedAt = lastPlayedAt)
    currentBookStore.updateData { merged.id }
    // it still has the progress it handed over to the merged book
    val dune = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Dune")),
      chapters = listOf(chapter),
    ).copy(lastPlayedAt = lastPlayedAt)

    val carried = carryOver(dune, previous = listOf(merged))

    assertEquals(expected = dune, actual = carried)
    assertEquals(expected = dune.id, actual = currentBookStore.data.first())
  }

  @Test
  fun `a book whose folder was renamed keeps its progress, name and bookmarks`() = runTest {
    val oldChapters = listOf("1.mp3", "2.mp3").map { file("primary:Audiobooks/Book/$it") }
    val old = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Book")),
      chapters = oldChapters,
    ).copy(
      customName = "Renamed",
      customCover = File("cover.webp"),
      currentChapter = oldChapters[1],
      positionInChapter = 42,
      lastPlayedAt = Instant.parse("2026-01-01T10:00:00Z"),
    )
    val bookmark = Bookmark(
      bookId = old.id,
      chapterId = oldChapters[1],
      title = "Twist",
      time = 7,
      addedAt = Instant.EPOCH,
      id = Bookmark.Id.random(),
    )
    coEvery { bookmarkRepo.bookmarks(old) } returns listOf(bookmark)
    currentBookStore.updateData { old.id }

    val newChapters = listOf("1.mp3", "2.mp3").map { file("primary:Audiobooks/Book (2020)/$it") }
    val new = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Book (2020)")),
      chapters = newChapters,
    )

    val carried = carryOver(new, previous = listOf(old))

    assertEquals(
      expected = new.copy(
        customName = "Renamed",
        customCover = File("cover.webp"),
        currentChapter = newChapters[1],
        positionInChapter = 42,
        lastPlayedAt = Instant.parse("2026-01-01T10:00:00Z"),
      ),
      actual = carried,
    )
    coVerify { bookmarkRepo.addBookmark(bookmark.copy(bookId = new.id, chapterId = newChapters[1])) }
    coVerify {
      listeningHistoryRepo.moveToBook(from = listOf(old.id), to = new.id, chapters = oldChapters.zip(newChapters).toMap())
    }
    assertEquals(expected = new.id, actual = currentBookStore.data.first())
  }

  @Test
  fun `a moved book that was never played keeps what the listener changed and takes its names from its files`() = runTest {
    val old = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Book")),
      chapters = listOf(file("primary:Audiobooks/Book/1.mp3")),
    ).copy(
      addedAt = Instant.parse("2025-01-01T10:00:00Z"),
      customName = "Renamed",
      customCover = File("picked.webp"),
    )
    val oldWithCoverFromFiles = old.copy(metadata = old.metadata.copy(cover = File("found.webp")))
    val new = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Book (2020)")),
      chapters = listOf(file("primary:Audiobooks/Book (2020)/1.mp3")),
    ).copy(metadata = old.metadata.copy(name = "Book (2020)"))

    val carried = carryOver(new, previous = listOf(oldWithCoverFromFiles))

    assertEquals(
      expected = new.copy(
        addedAt = old.addedAt,
        customName = "Renamed",
        customCover = File("picked.webp"),
        metadata = new.metadata.copy(cover = File("found.webp")),
      ),
      actual = carried,
    )
  }

  @Test
  fun `a moved book follows the name from its files when the listener didn't give it one`() = runTest {
    val old = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Book")),
      chapters = listOf(file("primary:Audiobooks/Book/1.mp3")),
    ).copy(lastPlayedAt = Instant.parse("2026-01-01T10:00:00Z"))
    val new = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Book (2020)")),
      chapters = listOf(file("primary:Audiobooks/Book (2020)/1.mp3")),
    ).copy(metadata = old.metadata.copy(name = "Book (2020)"))

    val carried = carryOver(new, previous = listOf(old))

    assertEquals(expected = "Book (2020)", actual = carried.name)
  }

  @Test
  fun `a book that was in the library already keeps what the listener changed about it`() = runTest {
    val chapters = listOf("1.mp3", "2.mp3")
    val old = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Book")),
      chapters = chapters.map { ChapterId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Book/$it")) },
    ).copy(customName = "Theirs", lastPlayedAt = Instant.parse("2026-01-02T10:00:00Z"))
    val new = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks/Book", document = "primary:Audiobooks/Book")),
      chapters = chapters.map {
        ChapterId(documentUri(tree = "primary:Audiobooks/Book", document = "primary:Audiobooks/Book/$it"))
      },
    ).copy(customName = "Mine", lastPlayedAt = Instant.parse("2026-01-01T10:00:00Z"))

    val carried = carryOver(new, previous = listOf(old), inLibrary = true)

    assertEquals(expected = "Mine", actual = carried.name)
    assertEquals(expected = old.lastPlayedAt, actual = carried.lastPlayedAt)
  }

  @Test
  fun `a book that was in the library already doesn't take over a removed copy`() = runTest {
    val old = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Book")),
      chapters = listOf(file("primary:Audiobooks/Book/1.mp3")),
    ).copy(positionInChapter = 42, lastPlayedAt = Instant.parse("2026-01-02T10:00:00Z"))
    val copy = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Copy")),
      chapters = listOf(file("primary:Audiobooks/Copy/1.mp3")),
    )

    val carried = carryOver(copy, previous = listOf(old), inLibrary = true)

    assertEquals(expected = copy, actual = carried)
  }

  @Test
  fun `a book that only shares an intro with a removed book starts over`() = runTest {
    val oldIntro = file("primary:Audiobooks/Part 1/intro.mp3")
    val old = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Part 1")),
      chapters = listOf(oldIntro, file("primary:Audiobooks/Part 1/1.mp3", size = 20)),
    ).copy(currentChapter = oldIntro, positionInChapter = 42, lastPlayedAt = Instant.parse("2026-01-01T10:00:00Z"))
    val new = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Part 2")),
      chapters = listOf(file("primary:Audiobooks/Part 2/intro.mp3"), file("primary:Audiobooks/Part 2/1.mp3", size = 30)),
    )

    val carried = carryOver(new, previous = listOf(old))

    assertEquals(expected = new, actual = carried)
    coVerify(exactly = 0) { listeningHistoryRepo.moveToBook(any(), any(), any()) }
  }

  @Test
  fun `files with the same name keep their place in a moved book`() = runTest {
    fun chapters(folder: String) = listOf(
      file("primary:Audiobooks/$folder/CD1/01.mp3"),
      file("primary:Audiobooks/$folder/CD2/01.mp3"),
    )
    val oldChapters = chapters("Book")
    val old = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Book")),
      chapters = oldChapters,
    ).copy(currentChapter = oldChapters[1], positionInChapter = 42, lastPlayedAt = Instant.parse("2026-01-01T10:00:00Z"))
    val newChapters = chapters("Moved")
    val new = bookContent(
      id = BookId(documentUri(tree = "primary:Audiobooks", document = "primary:Audiobooks/Moved")),
      chapters = newChapters,
    )

    val carried = carryOver(new, previous = listOf(old))

    assertEquals(expected = newChapters[1], actual = carried.currentChapter)
  }

  private suspend fun carryOver(
    content: BookContent,
    previous: List<BookContent>,
    inLibrary: Boolean = false,
  ): BookContent {
    return progressCarryOver.carryOver(
      content = content,
      previousBooks = PreviousBooks(
        library = if (inLibrary) previous + content else previous,
        scanned = setOf(content.id),
        chapterRepo = chapterRepo,
      ),
    )
  }

  private fun file(
    document: String,
    size: Long = 10,
  ): ChapterId {
    val id = ChapterId(documentUri(tree = "primary:Audiobooks", document = document))
    chapters[id] = Chapter(
      id = id,
      name = null,
      duration = 1000,
      fileLastModified = Instant.EPOCH,
      fileSize = size,
      markData = emptyList(),
    )
    return id
  }

  private fun documentUri(
    tree: String,
    document: String,
  ) = DocumentsContract.buildDocumentUriUsingTree(
    DocumentsContract.buildTreeDocumentUri("com.android.externalstorage.documents", tree),
    document,
  )

  private fun bookContent(
    id: BookId,
    chapters: List<ChapterId>,
  ) = BookContent(
    id = id,
    playbackSpeed = 1F,
    skipSilence = false,
    isActive = true,
    lastPlayedAt = Instant.EPOCH,
    addedAt = Instant.parse("2026-02-01T10:00:00Z"),
    chapters = chapters,
    currentChapter = chapters.first(),
    positionInChapter = 0,
    gain = 0F,
    metadata = BookMetadata(
      name = "Book",
      author = null,
      album = null,
      title = null,
      genre = null,
      narrator = null,
      series = null,
      part = null,
      cover = null,
      fileLastModified = null,
    ),
    customName = null,
    customCover = null,
  )
}
