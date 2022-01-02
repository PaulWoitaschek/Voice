@file:Suppress("BlockingMethodInNonBlockingContext")

package de.ph1b.audiobook.scanner

import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.ph1b.audiobook.data.repo.BookContentRepo
import de.ph1b.audiobook.data.repo.ChapterRepo
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import java.io.Closeable
import java.io.File
import java.nio.file.Files

@RunWith(AndroidJUnit4::class)
class MediaScanner2Test {

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
        uri = book1.toUri(),
        chapters = book1Chapters.drop(1)
      )
    )
  }

  @Test
  fun metadataPreservedOnDeletion() = test {
    val audiobookFolder = folder("audiobooks")

    val book1 = File(audiobookFolder, "book1")
    val book1Chapters = listOf(
      file(book1, "1.mp3"),
      file(book1, "2.mp3"),
      file(book1, "10.mp3"),
    )

    scan(audiobookFolder)

    val contentWithPositionAtLastChapter = bookContentRepo[book1.toUri()]!!.copy(currentChapter = book1Chapters.last())
    bookContentRepo.put(contentWithPositionAtLastChapter)

    book1Chapters.forEach { it.toFile().delete() }

    scan(audiobookFolder)

    file(book1, "1.mp3")
    file(book1, "2.mp3")
    file(book1, "10.mp3")

    bookContentRepo[book1.toUri()] shouldBe contentWithPositionAtLastChapter
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
      BookContentView(topFileBook, chapters = listOf(topFileBook)),
      BookContentView(book1.toUri(), chapters = book1Chapters),
      BookContentView(book2.toUri(), chapters = book2Chapters),
    )
  }

  private fun test(test: suspend TestEnvironment.() -> Unit) {
    runTest {
      TestEnvironment().use { test(it) }
    }
  }

  private class TestEnvironment : Closeable {

    val bookContentRepo = BookContentRepo()
    private val chapterRepo = ChapterRepo()
    private val mediaAnalyzer = mockk<MediaAnalyzer>()
    private val scanner = MediaScanner2(bookContentRepo, chapterRepo, mediaAnalyzer)

    private val root: File = Files.createTempDirectory(this::class.java.canonicalName!!).toFile()

    suspend fun scan(vararg roots: File) {
      scanner.scan(roots.map(DocumentFile::fromFile))
    }

    fun file(parent: File, name: String): Uri {
      return File(parent, name)
        .also {
          it.parentFile?.mkdirs()
          check(it.createNewFile())
        }
        .also {
          coEvery { mediaAnalyzer.analyze(it.toUri()) } coAnswers {
            MediaAnalyzer.Metadata(duration = 1000L,
              author = "Author",
              bookName = "Book Name",
              chapterName = "Chapter",
              chapters = emptyList())
          }
        }
        .toUri()
    }

    fun folder(name: String): File {
      return File(root, name)
        .also { it.mkdirs() }
    }

    fun assertBookContents(vararg expected: BookContentView) {
      val actual = bookContentRepo.all()
        .filter { it.isActive }
        .map { BookContentView(uri = it.uri, chapters = it.chapters) }
      actual shouldContainExactly expected.toList()
    }

    override fun close() {
      root.delete()
    }
  }

  data class BookContentView(
    val uri: Uri,
    val chapters: List<Uri>
  )
}
