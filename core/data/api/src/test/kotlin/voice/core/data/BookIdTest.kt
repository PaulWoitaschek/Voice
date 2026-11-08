package voice.core.data

import android.net.Uri
import android.provider.DocumentsContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookIdTest {

  private val authority = "com.android.externalstorage.documents"
  private val audiobooks = DocumentsContract.buildTreeDocumentUri(authority, "primary:Audiobooks")

  @Test
  fun `a book in a folder is in it`() {
    val book = bookIn(audiobooks, "primary:Audiobooks/Dune")

    assertTrue(book.isIn(audiobooks))
  }

  @Test
  fun `a folder added as a book is in it`() {
    val book = bookIn(audiobooks, "primary:Audiobooks")

    assertTrue(book.isIn(audiobooks))
  }

  @Test
  fun `a book from another folder isn't in it`() {
    val kids = DocumentsContract.buildTreeDocumentUri(authority, "primary:Audiobooks/Kids")
    val book = bookIn(kids, "primary:Audiobooks/Kids/Gruffalo")

    assertFalse(book.isIn(audiobooks))
    assertTrue(book.isIn(kids))
  }

  @Test
  fun `nothing is in a single file`() {
    val file = DocumentsContract.buildDocumentUri(authority, "primary:Dune.m4b")
    val book = BookId(file)

    assertFalse(book.isIn(file))
    assertFalse(book.isIn(audiobooks))
    assertFalse(bookIn(audiobooks, "primary:Audiobooks/Dune").isIn(file))
  }

  @Test
  fun `a book from a server isn't in a folder`() {
    assertFalse(BookId("abs://item/dune").isIn(audiobooks))
  }

  private fun bookIn(
    folder: Uri,
    documentId: String,
  ) = BookId(DocumentsContract.buildDocumentUriUsingTree(folder, documentId))
}
