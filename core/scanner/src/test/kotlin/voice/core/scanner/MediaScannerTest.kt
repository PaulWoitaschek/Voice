package voice.core.scanner

import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
import voice.core.data.ChapterId
import voice.core.data.MAX_CHAPTER_IDS_BYTES
import voice.core.data.folders.FolderType
import voice.core.data.repo.BookContentRepo
import voice.core.data.repo.BookContentRepoImpl
import voice.core.data.repo.BookRepositoryImpl
import voice.core.data.repo.BookmarkRepo
import voice.core.data.repo.ChapterRepoImpl
import voice.core.data.repo.internals.AppDb
import voice.core.data.store.CurrentBookStore
import voice.core.data.toUri
import voice.core.documentfile.FileBasedDocumentFactory
import voice.core.documentfile.FileBasedDocumentFile
import java.io.Closeable
import java.io.File
import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class MediaScannerTest {

  @Test
  fun singleFileDeletion() = test {
    val audiobookFolder = folder("audiobooks")

    val book1 = File(audiobookFolder, "book1")
    val book1Chapters = listOf(
      audioFile(book1, "1.mp3"),
      audioFile(book1, "2.mp3"),
      audioFile(book1, "10.mp3"),
    )

    scan(FolderType.Root, audiobookFolder)

    book1Chapters.first().delete()

    scan(FolderType.Root, audiobookFolder)

    assertBookContents(
      BookContentView(
        id = book1,
        chapters = book1Chapters.drop(1),
      ),
    )
  }

  @Test
  fun `a previewed book is scanned without analyzing its files again`() = test {
    val audiobookFolder = folder("audiobooks")
    val book = File(audiobookFolder, "book")
    audioFile(book, "1.mp3")
    audioFile(book, "2.mp3")
    audioFile(book, "3.mp3")

    val preview = previewer.preview(FileBasedDocumentFile(book))

    assertEquals(expected = BookPreview(name = "Book Name", author = "Author", duration = 3.seconds), actual = preview)
    assertEquals(expected = 3, actual = analyzeCalls)

    scan(FolderType.Root, audiobookFolder)

    // only the first file again, for the name of the book
    assertEquals(expected = 4, actual = analyzeCalls)
    assertBookContents(BookContentView(id = book, chapters = book.listFiles()!!.sortedBy { it.name }))
  }

  @Test
  fun `a book in the library is previewed the way the library shows it`() = test {
    val audiobookFolder = folder("audiobooks")
    val book = File(audiobookFolder, "book")
    audioFile(book, "1.mp3")
    audioFile(book, "2.mp3")
    scan(FolderType.Root, audiobookFolder)
    val content = bookContentRepo.get(BookId(book.toUri()))!!
    bookContentRepo.put(content.copy(customName = "Renamed", metadata = content.metadata.copy(author = null)))
    val analyzeCallsAfterScan = analyzeCalls

    val preview = previewer.preview(FileBasedDocumentFile(book))

    assertEquals(expected = BookPreview(name = "Renamed", author = null, duration = 2.seconds), actual = preview)
    assertEquals(expected = analyzeCallsAfterScan, actual = analyzeCalls)
  }

  @Test
  fun metadataPreservedOnDeletion() = test {
    val audiobookFolder = folder("audiobooks")

    val book1 = File(audiobookFolder, "book1")
    val book1Id = BookId(book1.toUri())
    val book1Chapters = listOf(
      audioFile(book1, "1.mp3"),
      audioFile(book1, "2.mp3"),
      audioFile(book1, "10.mp3"),
    )

    scan(FolderType.Root, audiobookFolder)

    val contentWithPositionAtLastChapter =
      bookContentRepo.get(BookId(book1.toUri()))!!.copy(currentChapter = ChapterId(book1Chapters.last().toUri()))
    bookContentRepo.put(contentWithPositionAtLastChapter)

    book1Chapters.forEach { it.toUri().toFile().delete() }

    scan(FolderType.Root, audiobookFolder)

    audioFile(book1, "1.mp3")
    audioFile(book1, "2.mp3")
    audioFile(book1, "10.mp3")

    assertEquals(expected = contentWithPositionAtLastChapter, actual = bookContentRepo.get(book1Id))
  }

  @Test
  fun multipleRoots() = test {
    val audiobookFolder1 = folder("audiobooks1")

    val topFileBook = audioFile(parent = audiobookFolder1, "test.mp3")

    val book1 = File(audiobookFolder1, "book1")
    val book1Chapters = listOf(
      audioFile(book1, "1.mp3"),
      audioFile(book1, "2.mp3"),
      audioFile(book1, "10.mp3"),
    )

    val audiobookFolder2 = folder("audiobooks1")

    val book2 = File(audiobookFolder2, "book2")
    val book2Chapters = listOf(audioFile(book2, "1.mp3"))

    scan(FolderType.Root, audiobookFolder1, audiobookFolder2)

    assertBookContents(
      BookContentView(topFileBook, chapters = listOf(topFileBook)),
      BookContentView(book1, chapters = book1Chapters),
      BookContentView(book2, chapters = book2Chapters),
    )
  }

  @Test
  fun scanRoot() = test {
    val audiobookFolder = folder("audiobooks1")

    val topFileBook = audioFile(parent = audiobookFolder, "test.mp3")

    val book1 = File(audiobookFolder, "book1")
    val book1Chapters = listOf(
      audioFile(book1, "1.mp3"),
      audioFile(book1, "2.mp3"),
      audioFile(book1, "10.mp3"),
    )

    val book2 = File(audiobookFolder, "book2")
    val book2Chapters = listOf(
      audioFile(book2, "1.mp3"),
      audioFile(book2, "2.mp3"),
      audioFile(book2, "10.mp3"),
    )

    scan(FolderType.Root, audiobookFolder)

    assertBookContents(
      BookContentView(topFileBook, chapters = listOf(topFileBook)),
      BookContentView(book1, chapters = book1Chapters),
      BookContentView(book2, chapters = book2Chapters),
    )
  }

  @Test
  fun `books on a server stay active when the scan doesn't find them`() = test {
    val audiobookFolder = folder("audiobooks")
    audioFile(File(audiobookFolder, "book"), "1.mp3")
    val serverChapter = ChapterId("abs://item/remote/track/1/ino")
    val serverBook = BookContent(
      id = BookId("abs://item/remote"),
      playbackSpeed = 1F,
      skipSilence = false,
      isActive = true,
      lastPlayedAt = Instant.EPOCH,
      addedAt = Instant.EPOCH,
      chapters = listOf(serverChapter),
      currentChapter = serverChapter,
      positionInChapter = 0,
      gain = 0F,
      metadata = BookMetadata(
        name = "On the server",
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
    bookContentRepo.put(serverBook)

    scan(FolderType.Root, audiobookFolder)

    assertEquals(expected = true, actual = bookContentRepo.get(serverBook.id)!!.isActive)
  }

  @Test
  fun scanSingleFile() = test {
    val book = audioFile(parent = folder("audiobooks1"), "test.mp3")
    scan(FolderType.SingleFile, book)
    assertBookContents(
      BookContentView(book, chapters = listOf(book)),
    )
  }

  @Test
  fun scanSingleFolder() = test {
    val folder = folder("book")
    val book = audioFile(parent = folder, "test.mp3")
    scan(FolderType.SingleFolder, folder)
    assertBookContents(
      BookContentView(folder, chapters = listOf(book)),
    )
  }

  @Test
  fun `a book with more files than fit into one book is skipped`() = test {
    val audiobookFolder = folder("audiobooks")
    val book = File(audiobookFolder, "Book")
    val chapter = audioFile(parent = book, "1.mp3")
    filesThatDontFitIntoOneBook(File(audiobookFolder, "Huge"))

    scan(FolderType.Root, audiobookFolder)

    assertBookContents(BookContentView(book, chapters = listOf(chapter)))
  }

  @Test
  fun newBookReusesFirstChapterMetadata() = test {
    val folder = folder("book")
    audioFile(parent = folder, "1.mp3")
    audioFile(parent = folder, "2.mp3")

    scan(FolderType.SingleFolder, folder)

    assertEquals(expected = 2, actual = analyzeCalls)
  }

  @Test
  fun scanAuthor() = test {
    val audioBooks = folder("audiobooks")

    val book1 = audioFile(parent = audioBooks, "test.mp3")

    val book2 = audioFile(parent = audioBooks, "author1/test.mp3")

    val book3 = File(audioBooks, "author1/book1")
    val book3Chapter1 = audioFile(parent = book3, "c1.mp3")
    val book3Chapter2 = audioFile(parent = book3, "c2.mp3")

    val book4 = File(audioBooks, "author1/book2")
    val book4Chapter1 = audioFile(book4, "a.mp3")

    scan(FolderType.Author, audioBooks)
    assertBookContents(
      BookContentView(book1, chapters = listOf(book1)),
      BookContentView(book2, chapters = listOf(book2)),
      BookContentView(book3, chapters = listOf(book3Chapter1, book3Chapter2)),
      BookContentView(book4, chapters = listOf(book4Chapter1)),
    )
  }

  @Test
  fun scanAuthorParsesEachBookOnce() = test {
    val audioBooks = folder("audiobooks")

    audioFile(parent = audioBooks, "test.mp3")
    audioFile(parent = audioBooks, "author1/test.mp3")
    audioFile(parent = File(audioBooks, "author1/book1"), "c1.mp3")
    audioFile(parent = File(audioBooks, "author1/book1"), "c2.mp3")
    audioFile(parent = File(audioBooks, "author1/book2"), "a.mp3")

    scan(FolderType.Author, audioBooks)

    assertEquals(expected = 4, actual = scannedBooks.size)
  }

  @Test
  fun `progress follows the chapter when a single book becomes a library`() = test {
    val audiobookFolder = folder("audiobooks")
    val book1 = File(audiobookFolder, "book1")
    audioFile(book1, "1.mp3")
    audioFile(book1, "2.mp3")
    val book2 = File(audiobookFolder, "book2")
    audioFile(book2, "1.mp3")
    val book2Chapter2 = audioFile(book2, "2.mp3")

    scan(FolderType.SingleFolder, audiobookFolder)
    val playedAt = Instant.parse("2026-01-01T10:00:00Z")
    play(BookId(audiobookFolder.toUri()), book2Chapter2, position = 500, playedAt = playedAt)

    scan(FolderType.Root, audiobookFolder)

    val book2Content = bookContentRepo.get(BookId(book2.toUri()))!!
    assertEquals(expected = ChapterId(book2Chapter2.toUri()), actual = book2Content.currentChapter)
    assertEquals(expected = 500, actual = book2Content.positionInChapter)
    assertEquals(expected = playedAt, actual = book2Content.lastPlayedAt)
    assertEquals(expected = 1.5F, actual = book2Content.playbackSpeed)
    assertEquals(expected = 0, actual = bookContentRepo.get(BookId(book1.toUri()))!!.positionInChapter)
  }

  @Test
  fun `progress follows the most recently played book when a library becomes a single book`() = test {
    val audiobookFolder = folder("audiobooks")
    val book1 = File(audiobookFolder, "book1")
    val book1Chapter2 = audioFile(book1, "2.mp3")
    audioFile(book1, "1.mp3")
    val book2 = File(audiobookFolder, "book2")
    val book2Chapter1 = audioFile(book2, "1.mp3")

    scan(FolderType.Root, audiobookFolder)
    play(BookId(book1.toUri()), book1Chapter2, position = 100, playedAt = Instant.parse("2026-01-01T10:00:00Z"))
    play(BookId(book2.toUri()), book2Chapter1, position = 200, playedAt = Instant.parse("2026-01-02T10:00:00Z"))

    scan(FolderType.SingleFolder, audiobookFolder)

    val content = bookContentRepo.get(BookId(audiobookFolder.toUri()))!!
    assertEquals(expected = ChapterId(book2Chapter1.toUri()), actual = content.currentChapter)
    assertEquals(expected = 200, actual = content.positionInChapter)
  }

  @Test
  fun `the current book and bookmarks follow a book into its new folder mode`() = test {
    val audiobookFolder = folder("audiobooks")
    val book = File(audiobookFolder, "book")
    val chapter = audioFile(book, "1.mp3")

    scan(FolderType.SingleFolder, book)
    val oldId = BookId(book.toUri())
    play(oldId, chapter, position = 100, playedAt = Instant.parse("2026-01-01T10:00:00Z"))
    currentBookStore.updateData { oldId }
    val bookmark = Bookmark(
      bookId = oldId,
      chapterId = ChapterId(chapter.toUri()),
      title = null,
      time = 100,
      addedAt = Instant.EPOCH,
      id = Bookmark.Id.random(),
    )
    coEvery { bookmarkRepo.bookmarks(match { it.id == oldId }) } returns listOf(bookmark)

    scan(FolderType.SingleFile, chapter)

    val newId = BookId(chapter.toUri())
    assertEquals(expected = newId, actual = currentBookStore.data.first())
    coVerify(exactly = 1) { bookmarkRepo.addBookmark(any()) }
    coVerify { bookmarkRepo.addBookmark(bookmark.copy(bookId = newId)) }
  }

  @Test
  fun `a book played after the carry over keeps its own progress on the next scan`() = test {
    val audiobookFolder = folder("audiobooks")
    val book = File(audiobookFolder, "book")
    val chapter1 = audioFile(book, "1.mp3")
    val chapter2 = audioFile(book, "2.mp3")

    scan(FolderType.SingleFolder, audiobookFolder)
    play(BookId(audiobookFolder.toUri()), chapter1, position = 100, playedAt = Instant.parse("2026-01-01T10:00:00Z"))
    scan(FolderType.Root, audiobookFolder)
    play(BookId(book.toUri()), chapter2, position = 300, playedAt = Instant.parse("2026-01-02T10:00:00Z"))

    scan(FolderType.Root, audiobookFolder)

    val content = bookContentRepo.get(BookId(book.toUri()))!!
    assertEquals(expected = ChapterId(chapter2.toUri()), actual = content.currentChapter)
    assertEquals(expected = 300, actual = content.positionInChapter)
  }

  @Test
  fun `a book keeps its progress and bookmarks when its folder is renamed`() = test {
    val audiobookFolder = folder("audiobooks")
    val book = File(audiobookFolder, "book")
    audioFile(book, "1.mp3")
    val chapter2 = audioFile(book, "2.mp3")
    scan(FolderType.Root, audiobookFolder)
    val oldId = BookId(book.toUri())
    play(oldId, chapter2, position = 100, playedAt = Instant.parse("2026-01-01T10:00:00Z"))
    currentBookStore.updateData { oldId }
    val bookmark = Bookmark(
      bookId = oldId,
      chapterId = ChapterId(chapter2.toUri()),
      title = null,
      time = 50,
      addedAt = Instant.EPOCH,
      id = Bookmark.Id.random(),
    )
    coEvery { bookmarkRepo.bookmarks(match { it.id == oldId }) } returns listOf(bookmark)

    val renamed = File(audiobookFolder, "book (2020)")
    check(book.renameTo(renamed))
    scan(FolderType.Root, audiobookFolder)

    val newId = BookId(renamed.toUri())
    val renamedChapter2 = ChapterId(File(renamed, "2.mp3").toUri())
    val content = bookContentRepo.get(newId)!!
    assertEquals(expected = renamedChapter2, actual = content.currentChapter)
    assertEquals(expected = 100, actual = content.positionInChapter)
    assertEquals(expected = newId, actual = currentBookStore.data.first())
    coVerify { bookmarkRepo.addBookmark(bookmark.copy(bookId = newId, chapterId = renamedChapter2)) }
  }

  @Test
  fun `a book renamed back takes the progress from its other name along`() = test {
    val audiobookFolder = folder("audiobooks")
    val book = File(audiobookFolder, "book")
    val chapter1 = audioFile(book, "1.mp3")
    audioFile(book, "2.mp3")
    scan(FolderType.Root, audiobookFolder)
    play(BookId(book.toUri()), chapter1, position = 100, playedAt = Instant.parse("2026-01-01T10:00:00Z"))
    val renamed = File(audiobookFolder, "book (2020)")
    check(book.renameTo(renamed))
    scan(FolderType.Root, audiobookFolder)
    play(BookId(renamed.toUri()), File(renamed, "2.mp3"), position = 300, playedAt = Instant.parse("2026-01-02T10:00:00Z"))

    check(renamed.renameTo(book))
    scan(FolderType.Root, audiobookFolder)

    val content = bookContentRepo.get(BookId(book.toUri()))!!
    assertEquals(expected = ChapterId(File(book, "2.mp3").toUri()), actual = content.currentChapter)
    assertEquals(expected = 300, actual = content.positionInChapter)
  }

  @Test
  fun `a copy in the library keeps its own progress while the other copy is missing`() = test {
    val card = folder("card")
    val original = File(card, "book")
    val originalChapter = audioFile(original, "1.mp3")
    val internal = folder("internal")
    val copy = File(internal, "book")
    audioFile(copy, "1.mp3")
    scan(FolderType.Root, card, internal)
    play(BookId(original.toUri()), originalChapter, position = 100, playedAt = Instant.parse("2026-01-01T10:00:00Z"))

    scan(FolderType.Root, internal)

    assertEquals(expected = Instant.EPOCH, actual = bookContentRepo.get(BookId(copy.toUri()))!!.lastPlayedAt)
  }

  @Test
  fun `a book takes what its files say when they change`() = test {
    val audiobookFolder = folder("audiobooks")
    val book = File(audiobookFolder, "book")
    val chapter1 = audioFile(book, "1.mp3")
    audioFile(book, "2.mp3")
    scan(FolderType.Root, audiobookFolder)

    album = "New Name"
    artist = "New Author"
    changed(chapter1)
    scan(FolderType.Root, audiobookFolder)

    val content = bookContentRepo.get(BookId(book.toUri()))!!
    assertEquals(expected = "New Name", actual = content.name)
    assertEquals(expected = "New Name", actual = content.metadata.album)
    assertEquals(expected = "Title", actual = content.metadata.title)
    assertEquals(expected = "New Author", actual = content.metadata.author)
  }

  @Test
  fun `a book keeps the name the listener gave it when its files change`() = test {
    val audiobookFolder = folder("audiobooks")
    val book = File(audiobookFolder, "book")
    val chapter1 = audioFile(book, "1.mp3")
    scan(FolderType.Root, audiobookFolder)
    val id = BookId(book.toUri())
    bookContentRepo.put(bookContentRepo.get(id)!!.withCustomName("Mine"))

    album = "New Name"
    artist = "New Author"
    changed(chapter1)
    scan(FolderType.Root, audiobookFolder)

    val content = bookContentRepo.get(id)!!
    assertEquals(expected = "Mine", actual = content.name)
    assertEquals(expected = "New Name", actual = content.metadata.name)
    assertEquals(expected = "New Author", actual = content.metadata.author)
  }

  @Test
  fun `a book takes its names from the file that comes first now`() = test {
    val audiobookFolder = folder("audiobooks")
    val book = File(audiobookFolder, "book")
    val chapter1 = audioFile(book, "1.mp3")
    audioFile(book, "2.mp3")
    scan(FolderType.Root, audiobookFolder)

    album = "New Name"
    check(chapter1.delete())
    scan(FolderType.Root, audiobookFolder)

    assertEquals(expected = "New Name", actual = bookContentRepo.get(BookId(book.toUri()))!!.name)
  }

  @Test
  fun `a book whose files didn't change isn't analyzed again`() = test {
    val audiobookFolder = folder("audiobooks")
    audioFile(File(audiobookFolder, "book"), "1.mp3")
    scan(FolderType.Root, audiobookFolder)
    val analyzeCallsAfterScan = analyzeCalls

    scan(FolderType.Root, audiobookFolder)

    assertEquals(expected = analyzeCallsAfterScan, actual = analyzeCalls)
  }

  @Test
  fun `a book stored before its names were kept apart keeps only a name the listener gave it`() = test {
    val audiobookFolder = folder("audiobooks")
    val named = File(audiobookFolder, "named")
    audioFile(named, "1.mp3")
    val renamed = File(audiobookFolder, "renamed")
    audioFile(renamed, "1.mp3")
    scan(FolderType.Root, audiobookFolder)
    fun BookContent.migrated(name: String) = copy(
      customName = name,
      metadata = metadata.copy(name = name, fileLastModified = null),
    )
    bookContentRepo.put(bookContentRepo.get(BookId(named.toUri()))!!.migrated("Book Name"))
    bookContentRepo.put(bookContentRepo.get(BookId(renamed.toUri()))!!.migrated("Mine"))
    val analyzeCallsBefore = analyzeCalls

    scan(FolderType.Root, audiobookFolder)
    scan(FolderType.Root, audiobookFolder)

    assertEquals(expected = null, actual = bookContentRepo.get(BookId(named.toUri()))!!.customName)
    val renamedContent = bookContentRepo.get(BookId(renamed.toUri()))!!
    assertEquals(expected = "Mine", actual = renamedContent.name)
    assertEquals(expected = "Book Name", actual = renamedContent.metadata.name)
    assertEquals(expected = analyzeCallsBefore + 2, actual = analyzeCalls)
  }

  @Test
  fun `a book in a folder stored before its names were kept apart drops the name of its first file`() = test {
    val audiobookFolder = folder("audiobooks")
    val byTitle = File(audiobookFolder, "byTitle")
    val byFileName = File(audiobookFolder, "byFileName")
    val byFolder = File(audiobookFolder, "byFolder")
    listOf(byTitle, byFileName, byFolder).forEach { audioFile(it, "1.mp3") }
    scan(FolderType.Root, audiobookFolder)
    mapOf(byTitle to "Title", byFileName to "Chapter", byFolder to "byFolder").forEach { (book, name) ->
      val content = bookContentRepo.get(BookId(book.toUri()))!!
      bookContentRepo.put(
        content.copy(customName = name, metadata = content.metadata.copy(name = name, fileLastModified = null)),
      )
    }

    scan(FolderType.Root, audiobookFolder)

    assertEquals(expected = "Book Name", actual = bookContentRepo.get(BookId(byTitle.toUri()))!!.name)
    assertEquals(expected = "Book Name", actual = bookContentRepo.get(BookId(byFileName.toUri()))!!.name)
    assertEquals(expected = "byFolder", actual = bookContentRepo.get(BookId(byFolder.toUri()))!!.name)
  }

  @Test
  fun `a book that was never played keeps the name the listener gave it when its folder is renamed`() = test {
    val audiobookFolder = folder("audiobooks")
    val book = File(audiobookFolder, "book")
    audioFile(book, "1.mp3")
    scan(FolderType.Root, audiobookFolder)
    bookContentRepo.put(bookContentRepo.get(BookId(book.toUri()))!!.withCustomName("Mine"))

    val renamed = File(audiobookFolder, "book (2020)")
    check(book.renameTo(renamed))
    scan(FolderType.Root, audiobookFolder)

    assertEquals(expected = "Mine", actual = bookContentRepo.get(BookId(renamed.toUri()))!!.name)
  }

  private fun test(test: suspend TestEnvironment.() -> Unit) {
    runTest {
      TestEnvironment().use { test(it) }
    }
  }

  private class TestEnvironment : Closeable {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java)
      .allowMainThreadQueries()
      .build()
    val bookContentRepo = BookContentRepoImpl(db.bookContentDao())
    private val chapterRepo = ChapterRepoImpl(db.chapterDao())
    private val mediaAnalyzer = mockk<MediaAnalyzer>()
    var analyzeCalls = 0
    var album = "Book Name"
    var artist = "Author"
    val bookmarkRepo = mockk<BookmarkRepo>(relaxUnitFun = true) {
      coEvery { bookmarks(any()) } returns emptyList()
    }
    val currentBookStore = MemoryDataStore<BookId?>(null)
    private val scannedRepo = ScannedBooksRecordingRepo(bookContentRepo)
    val scannedBooks: List<BookId> get() = scannedRepo.scannedBooks
    private val scanner = MediaScanner(
      contentRepo = scannedRepo,
      chapterParser = ChapterParser(
        chapterRepo = chapterRepo,
        mediaAnalyzer = mediaAnalyzer,
      ),
      bookParser = BookParser(
        contentRepo = bookContentRepo,
        mediaAnalyzer = mediaAnalyzer,
        fileFactory = FileBasedDocumentFactory,
      ),
      deviceHasPermissionBug = mockk(),
      progressCarryOver = ProgressCarryOver(
        bookmarkRepo = bookmarkRepo,
        listeningHistoryRepo = mockk(relaxUnitFun = true),
        listeningStatsRepo = mockk(relaxUnitFun = true),
        currentBookStore = CurrentBookStore(currentBookStore),
      ),
      chapterRepo = chapterRepo,
    )

    val bookRepo = BookRepositoryImpl(chapterRepo, bookContentRepo)

    // with its own chapter repo like in the app, so only the database is shared with the scanner
    val previewer = BookPreviewerImpl(
      contentRepo = bookContentRepo,
      chapterParser = ChapterParser(
        chapterRepo = ChapterRepoImpl(db.chapterDao()),
        mediaAnalyzer = mediaAnalyzer,
      ),
      bookParser = BookParser(
        contentRepo = bookContentRepo,
        mediaAnalyzer = mediaAnalyzer,
        fileFactory = FileBasedDocumentFactory,
      ),
    )

    private val root: File = Files.createTempDirectory(this::class.java.canonicalName!!).toFile()

    suspend fun scan(
      type: FolderType = FolderType.Root,
      vararg roots: File,
    ) {
      scanner.scan(mapOf(type to roots.map(::FileBasedDocumentFile)))
    }

    @IgnorableReturnValue
    fun audioFile(
      parent: File,
      name: String,
    ): File {
      check(name.endsWith(".mp3"))
      return File(parent, name)
        .also {
          it.parentFile?.mkdirs()
          check(it.createNewFile())
          it.writeText(name)
        }
        .also {
          coEvery { mediaAnalyzer.analyze(any()) } coAnswers {
            analyzeCalls++
            Metadata(
              duration = 1000L,
              artist = artist,
              album = album,
              fileName = "Chapter",
              chapters = emptyList(),
              title = "Title",
              genre = "Genre",
              narrator = "Narrator",
              series = "Series",
              part = "Part",
            )
          }
        }
    }

    suspend fun play(
      id: BookId,
      chapter: File,
      position: Long,
      playedAt: Instant,
    ) {
      val content = bookContentRepo.get(id)!!
      bookContentRepo.put(
        content.copy(
          currentChapter = ChapterId(chapter.toUri()),
          positionInChapter = position,
          lastPlayedAt = playedAt,
          playbackSpeed = 1.5F,
        ),
      )
    }

    fun changed(file: File) {
      check(file.setLastModified(file.lastModified() + 60_000))
    }

    // long names, so fewer files are needed
    fun filesThatDontFitIntoOneBook(parent: File) {
      val folder = File(parent, "a".repeat(200)).also { it.mkdirs() }
      var bytes = 0
      var index = 0
      while (bytes <= MAX_CHAPTER_IDS_BYTES) {
        val file = File(folder, "${index++}".padStart(200, '0') + ".mp3")
        check(file.createNewFile())
        bytes += ChapterId(file.toUri()).value.length + 3
      }
    }

    fun folder(name: String): File {
      return File(root, name)
        .also { it.mkdirs() }
    }

    suspend fun assertBookContents(vararg expected: BookContentView) {
      bookRepo.all()
        .map {
          BookContentView(
            id = it.id.toUri().toFile(),
            chapters = it.content.chapters.map { chapter ->
              chapter.toUri().toFile()
            },
          )
        }
        .let { actual ->
          assertEquals(
            expected = expected.sortedBy { it.id },
            actual = actual.sortedBy { it.id },
          )
        }
    }

    override fun close() {
      root.deleteRecursively()
    }
  }

  data class BookContentView(
    val id: File,
    val chapters: List<File>,
  )

  private class ScannedBooksRecordingRepo(private val delegate: BookContentRepo) : BookContentRepo by delegate {

    val scannedBooks = mutableListOf<BookId>()

    override suspend fun setAllInactiveExcept(ids: List<BookId>) {
      scannedBooks += ids
      delegate.setAllInactiveExcept(ids)
    }
  }
}
