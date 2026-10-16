package voice.core.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.BookMetadata
import voice.core.data.ChapterId
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
  private val coverSaver = CoverSaver(repo = repo, context = context)

  @Test
  fun `a custom cover is saved as a webp of at most 1200 px`() = runTest {
    val content = content("book")

    coverSaver.save(content.id, Bitmap.createBitmap(2000, 2000, Bitmap.Config.ARGB_8888))

    val cover = contents.getValue(content.id).customCover!!
    assertEquals(expected = File(context.filesDir, "customCovers"), actual = cover.parentFile)
    assertWebp(cover, width = 1200, height = 1200)
  }

  @Test
  fun `an extracted cover is saved as a webp of at most 1200 px in its own folder`() = runTest {
    val content = content("book")

    val saved = coverSaver.saveExtractedCover(jpeg(width = 3000, height = 2000), content.id)

    assertTrue(saved)
    val cover = contents.getValue(content.id).metadata.cover!!
    assertEquals(expected = File(context.filesDir, "bookCovers"), actual = cover.parentFile)
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

  private fun content(name: String): BookContent {
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
        cover = null,
        fileLastModified = null,
      ),
      customName = null,
      customCover = null,
    ).also { contents[it.id] = it }
  }
}
