@file:Suppress("BlockingMethodInNonBlockingContext")

package voice.app.scanner

import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
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
import voice.common.BookId
import voice.data.Chapter
import voice.data.folders.FolderType
import voice.data.repo.BookContentRepo
import voice.data.repo.BookRepository
import voice.data.repo.ChapterRepo
import voice.data.repo.internals.AppDb
import voice.data.toUri
import java.io.Closeable
import java.io.File
import java.nio.file.Files

@RunWith(AndroidJUnit4::class)
class MediaScannerTest {

  init {
    Shadows.shadowOf(MimeTypeMap.getSingleton())
      .addExtensionMimeTypMapping("mp3", "audio/mp3")
  }

  @Test
  fun singleFileDeletion() = test {
    val audiobookFolder = folder("audiobooks")

    val book1 = File(audiobookFolder, "book1")
    val book1Chapters = listOf(
      file(book1, "1.mp3"),
      file(book1, "2.mp3"),
      file(book1, "10.mp3"),
    )

    scan(audiobookFolder)

    book1Chapters.first().toFile().delete()

    scan(audiobookFolder)

    assertBookContents(
      BookContentView(
        id = BookId(book1.toUri()),
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
      file(book1, "1.mp3"),
      file(book1, "2.mp3"),
      file(book1, "10.mp3"),
    ).map(Chapter::Id)

    scan(audiobookFolder)

    val contentWithPositionAtLastChapter = bookContentRepo.get(BookId(book1.toUri()))!!.copy(currentChapter = book1Chapters.last())
    bookContentRepo.put(contentWithPositionAtLastChapter)

    book1Chapters.forEach { it.toUri().toFile().delete() }

    scan(audiobookFolder)

    file(book1, "1.mp3")
    file(book1, "2.mp3")
    file(book1, "10.mp3")

    bookContentRepo.get(book1Id) shouldBe contentWithPositionAtLastChapter
  }

  @Test
  fun multipleRoots() = test {
    val audiobookFolder1 = folder("audiobooks1")

    val topFileBook = file(parent = audiobookFolder1, "test.mp3")

    val book1 = File(audiobookFolder1, "book1")
    val book1Chapters = listOf(
      file(book1, "1.mp3"),
      file(book1, "2.mp3"),
      file(book1, "10.mp3"),
    )

    val audiobookFolder2 = folder("audiobooks1")

    val book2 = File(audiobookFolder2, "book2")
    val book2Chapters = listOf(file(book2, "1.mp3"))

    scan(audiobookFolder1, audiobookFolder2)

    assertBookContents(
      BookContentView(topFileBook.let(::BookId), chapters = listOf(topFileBook)),
      BookContentView(book1.toUri().let(::BookId), chapters = book1Chapters),
      BookContentView(book2.toUri().let(::BookId), chapters = book2Chapters),
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
        application = ApplicationProvider.getApplicationContext(),
        legacyBookDao = db.legacyBookDao(),
        bookmarkMigrator = BookmarkMigrator(
          legacyBookDao = db.legacyBookDao(),
          bookmarkDao = db.bookmarkDao(),
        ),
        context = ApplicationProvider.getApplicationContext(),
      ),
    )

    val bookRepo = BookRepository(chapterRepo, bookContentRepo)

    private val root: File = Files.createTempDirectory(this::class.java.canonicalName!!).toFile()

    suspend fun scan(vararg roots: File) {
      scanner.scan(mapOf(FolderType.Root to roots.map(DocumentFile::fromFile)))
    }

    fun file(parent: File, name: String): Uri {
      return File(parent, name)
        .also {
          it.parentFile?.mkdirs()
          check(it.createNewFile())
        }
        .also {
          coEvery { mediaAnalyzer.analyze(any()) } coAnswers {
            MediaAnalyzer.Metadata(
              duration = 1000L,
              author = "Author",
              bookName = "Book Name",
              chapterName = "Chapter",
              chapters = emptyList(),
            )
          }
        }
        .toUri()
    }

    fun folder(name: String): File {
      return File(root, name)
        .also { it.mkdirs() }
    }

    suspend fun assertBookContents(vararg expected: BookContentView) {
      bookRepo.all()
        .map {
          BookContentView(
            id = it.id,
            chapters = it.content.chapters.map { chapter ->
              chapter.toUri()
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
    val id: BookId,
    val chapters: List<Uri>,
  )
}
