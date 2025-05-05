package voice.app.scanner

import android.webkit.MimeTypeMap
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import voice.common.BookId
import voice.data.ChapterId
import voice.data.folders.FolderType
import voice.data.repo.BookContentRepo
import voice.data.repo.BookRepository
import voice.data.repo.ChapterRepo
import voice.data.repo.internals.AppDb
import voice.data.toUri
import voice.documentfile.FileBasedDocumentFactory
import voice.documentfile.FileBasedDocumentFile
import java.io.Closeable
import java.io.File
import java.nio.file.Files

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class MediaScannerTest {

  init {
    Shadows.shadowOf(MimeTypeMap.getSingleton())
      .addExtensionMimeTypeMapping("mp3", "audio/mp3")
  }

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

    bookContentRepo.get(book1Id) shouldBe contentWithPositionAtLastChapter
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

  private fun test(test: suspend TestEnvironment.() -> Unit) {
    runTest {
      TestEnvironment().use { test(it) }
    }
  }

  private class TestEnvironment : Closeable {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java)
      .allowMainThreadQueries()
      .build()
    val bookContentRepo = BookContentRepo(db.bookContentDao())
    private val chapterRepo = ChapterRepo(db.chapterDao())
    private val mediaAnalyzer = mockk<MediaAnalyzer>()
    private val scanner = MediaScanner(
      contentRepo = bookContentRepo,
      chapterParser = ChapterParser(
        chapterRepo = chapterRepo,
        mediaAnalyzer = mediaAnalyzer,
      ),
      bookParser = BookParser(
        contentRepo = bookContentRepo,
        mediaAnalyzer = mediaAnalyzer,
        legacyBookDao = db.legacyBookDao(),
        application = ApplicationProvider.getApplicationContext(),
        bookmarkMigrator = BookmarkMigrator(
          legacyBookDao = db.legacyBookDao(),
          bookmarkDao = db.bookmarkDao(),
        ),
        fileFactory = FileBasedDocumentFactory,
      ),
      deviceHasPermissionBug = mockk(),
    )

    val bookRepo = BookRepository(chapterRepo, bookContentRepo)

    private val root: File = Files.createTempDirectory(this::class.java.canonicalName!!).toFile()

    suspend fun scan(
      type: FolderType = FolderType.Root,
      vararg roots: File,
    ) {
      scanner.scan(mapOf(type to roots.map(::FileBasedDocumentFile)))
    }

    fun audioFile(
      parent: File,
      name: String,
    ): File {
      check(name.endsWith(".mp3"))
      return File(parent, name)
        .also {
          it.parentFile?.mkdirs()
          check(it.createNewFile())
        }
        .also {
          coEvery { mediaAnalyzer.analyze(any()) } coAnswers {
            Metadata(
              duration = 1000L,
              artist = "Author",
              album = "Book Name",
              fileName = "Chapter",
              chapters = emptyList(),
              title = "Title",
            )
          }
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
        .shouldContainExactlyInAnyOrder(expected.toList())
    }

    override fun close() {
      root.delete()
    }
  }

  data class BookContentView(
    val id: File,
    val chapters: List<File>,
  )
}
