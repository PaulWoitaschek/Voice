package voice.core.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.BookMetadata
import voice.core.data.ChapterId
import voice.core.data.repo.BookContentRepo
import voice.core.data.repo.BookRepository
import java.io.File
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CoverSaverTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val contents = mutableMapOf<BookId, BookContent>()
  private val repo = mockk<BookRepository> {
    coEvery { this@mockk.get(any()) } returns null
    coEvery { updateBook(any(), any()) } answers {
      val id = firstArg<BookId>()
      contents[id] = secondArg<(BookContent) -> BookContent>()(contents.getValue(id))
    }
  }
  private val contentRepo = mockk<BookContentRepo> {
    coEvery { all() } answers { contents.values.toList() }
  }
  private val coverSaver = CoverSaver(repo = repo, contentRepo = contentRepo, context = context)

  @Test
  fun `a custom cover is saved as a webp of at most 1200 px`() = runTest {
    val content = content("book")

    coverSaver.save(content.id, Bitmap.createBitmap(2000, 2000, Bitmap.Config.ARGB_8888))

    val cover = contents.getValue(content.id).customCover!!
    assertEquals(expected = File(context.filesDir, "bookCovers"), actual = cover.parentFile)
    assertWebp(cover, width = 1200, height = 1200)
  }

  @Test
  fun `an extracted cover is saved as a webp of at most 1200 px in its own folder`() = runTest {
    val content = content("book")

    val saved = coverSaver.saveExtractedCover(jpeg(width = 3000, height = 2000), content.id)

    assertTrue(saved)
    val cover = contents.getValue(content.id).metadata.cover!!
    assertEquals(expected = File(context.filesDir, "extractedCovers"), actual = cover.parentFile)
    assertWebp(cover, width = 1200, height = 800)
  }

  @Test
  fun `a file that is no image is not saved as a cover`() = runTest {
    val content = content("book")
    val notAnImage = File(context.cacheDir, "notes").apply { writeText("notes") }

    val saved = coverSaver.saveExtractedCover(notAnImage, content.id)

    assertFalse(saved)
    assertNull(contents.getValue(content.id).metadata.cover)
  }

  @Test
  fun `extracted covers move out of the custom covers, for every book that uses them`() = runTest {
    val oldFolder = File(context.filesDir, "bookCovers").apply { mkdirs() }
    val extracted = File(oldFolder, "extracted.png").apply { writeText("extracted") }
    val custom = File(oldFolder, "custom.webp").apply { writeText("custom") }
    val book = content("book", cover = extracted, customCover = custom)
    val movedBook = content("movedBook", cover = extracted)

    coverSaver.moveExtractedCovers()

    val moved = File(context.filesDir, "extractedCovers/extracted.png")
    assertEquals(expected = "extracted", actual = moved.readText())
    assertFalse(extracted.exists())
    assertEquals(expected = moved, actual = contents.getValue(book.id).metadata.cover)
    assertEquals(expected = moved, actual = contents.getValue(movedBook.id).metadata.cover)
    assertEquals(expected = custom, actual = contents.getValue(book.id).customCover)
    assertTrue(custom.exists())
  }

  @Test
  fun `a png cover could be one the listener picked, so it stays backed up`() = runTest {
    val png = File(context.filesDir, "bookCovers/picked.png").apply {
      parentFile!!.mkdirs()
      outputStream().use { Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    val book = content("book", cover = png)

    coverSaver.moveExtractedCovers()

    assertTrue(png.exists())
    assertEquals(expected = png, actual = contents.getValue(book.id).metadata.cover)
  }

  @Test
  fun `a missing extracted cover is left for the scan to find again`() = runTest {
    val missing = File(context.filesDir, "bookCovers/missing.png")
    val book = content("book", cover = missing)

    coverSaver.moveExtractedCovers()

    assertEquals(expected = missing, actual = contents.getValue(book.id).metadata.cover)
    coVerify(exactly = 0) { repo.updateBook(any(), any()) }
  }

  private fun jpeg(
    width: Int,
    height: Int,
  ): File {
    return File(context.cacheDir, "image.jpg").apply {
      outputStream().use {
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it)
      }
    }
  }

  private fun assertWebp(
    file: File,
    width: Int,
    height: Int,
  ) {
    assertEquals(expected = "webp", actual = file.extension)
    assertEquals(expected = "WEBP", actual = file.readBytes().copyOfRange(8, 12).decodeToString())
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    assertEquals(expected = width to height, actual = bounds.outWidth to bounds.outHeight)
  }

  private fun content(
    name: String,
    cover: File? = null,
    customCover: File? = null,
  ): BookContent {
    val chapter = ChapterId("$name.mp3")
    return BookContent(
      id = BookId(name),
      playbackSpeed = 1F,
      skipSilence = false,
      isActive = true,
      lastPlayedAt = Instant.EPOCH,
      addedAt = Instant.EPOCH,
      chapters = listOf(chapter),
      currentChapter = chapter,
      positionInChapter = 0,
      gain = 0F,
      metadata = BookMetadata(
        name = name,
        author = null,
        album = null,
        title = null,
        genre = null,
        narrator = null,
        series = null,
        part = null,
        cover = cover,
        fileLastModified = null,
      ),
      customName = null,
      customCover = customCover,
    ).also { contents[it.id] = it }
  }
}
