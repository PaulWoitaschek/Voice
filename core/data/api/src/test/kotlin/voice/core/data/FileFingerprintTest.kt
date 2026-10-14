package voice.core.data

import android.provider.DocumentsContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class FileFingerprintTest {

  @Test
  fun `files are known by the name in their path and their size`() {
    val inFolder = DocumentsContract.buildDocumentUriUsingTree(
      DocumentsContract.buildTreeDocumentUri("com.android.externalstorage.documents", "primary:Audiobooks"),
      "primary:Audiobooks/Book: Part 1/01 - Intro.mp3",
    ).toString()
    val atTheRoot = "content://com.android.externalstorage.documents/document/primary%3ABook.m4b"
    val file = "file:///storage/emulated/0/Audiobooks/Book/02.mp3"

    val fingerprints = listOf(chapter(inFolder), chapter(atTheRoot), chapter(file)).fingerprints()

    assertEquals(
      expected = listOf("01 - Intro.mp3", "Book.m4b", "02.mp3").map { FileFingerprint(name = it, size = 10) },
      actual = fingerprints,
    )
  }

  @Test
  fun `a book with a file that can't be recognized has no fingerprints`() {
    val withoutPath = chapter("content://com.android.providers.media.documents/document/audio%3A42")
    val withoutSize = chapter("file:///storage/emulated/0/Audiobooks/Book/01.mp3", size = 0)
    val known = chapter("file:///storage/emulated/0/Audiobooks/Book/02.mp3")

    assertNull(listOf(known, withoutPath).fingerprints())
    assertNull(listOf(known, withoutSize).fingerprints())
    assertNull(listOf(known, withoutPath).syncKey())
  }

  @Test
  fun `the same files have the same key on every device`() {
    fun treeFile(
      tree: String,
      path: String,
    ) = DocumentsContract.buildDocumentUriUsingTree(
      DocumentsContract.buildTreeDocumentUri("com.android.externalstorage.documents", tree),
      path,
    ).toString()

    val phone = listOf(
      chapter(treeFile("primary:Audiobooks", "primary:Audiobooks/Book/01.mp3"), size = 10),
      chapter(treeFile("primary:Audiobooks", "primary:Audiobooks/Book/02.mp3"), size = 20),
    )
    val tablet = listOf(
      chapter(treeFile("1234-5678:Sync", "1234-5678:Sync/Audiobooks/Old name/01.mp3"), size = 10),
      chapter("file:///storage/emulated/0/Book/02.mp3", size = 20),
    )

    assertEquals(expected = phone.syncKey(), actual = tablet.syncKey())
    assertNotEquals(illegal = phone.syncKey(), actual = phone.reversed().syncKey())
    assertNotEquals(illegal = phone.syncKey(), actual = listOf(phone.first()).syncKey())
    assertNull(emptyList<Chapter>().syncKey())
  }

  private fun chapter(
    id: String,
    size: Long = 10,
  ) = Chapter(
    id = ChapterId(id),
    name = null,
    duration = 1000,
    fileLastModified = Instant.EPOCH,
    fileSize = size,
    markData = emptyList(),
  )
}
