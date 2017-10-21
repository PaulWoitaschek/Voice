package de.ph1b.audiobook.data.repo.internals

import android.support.test.InstrumentationRegistry
import android.support.v4.util.SparseArrayCompat
import com.squareup.moshi.Moshi
import de.ph1b.audiobook.BookFactory
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.Chapter
import de.ph1b.audiobook.data.repo.ClearDbRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Random


class BookStorageTest {

  private lateinit var register: BookStorage

  @Rule
  @JvmField
  val clearAppDbRule = ClearDbRule()

  @Before
  fun setUp() {
    val context = InstrumentationRegistry.getTargetContext()
    val internalDb = InternalDb(context)
    register = BookStorage(internalDb, Moshi.Builder().build())
  }

  @Test
  fun hideRevealBook() {
    val book = BookFactory.create()
    register.addBook(book)

    val activeBooks = register.activeBooks()
    val inactiveBooks = register.orphanedBooks()

    assertThat(activeBooks).hasSize(1)
    assertThat(inactiveBooks).isEmpty()

    register.hideBook(activeBooks.first().id)

    assertThat(register.activeBooks()).isEmpty()
    assertThat(register.orphanedBooks()).hasSize(1)

    val hiddenBook = register.orphanedBooks().single()

    register.revealBook(hiddenBook.id)

    assertThat(register.activeBooks()).hasSize(1)
    assertThat(register.orphanedBooks()).isEmpty()
  }

  /**
   * Tests that the returned book matches the retrieved one
   */
  @Test
  fun addBookReturn() {
    val mock = BookFactory.create()
    val inserted = register.addBook(mock)
    val retrieved = register.activeBooks().single()

    assertThat(inserted).isEqualTo(retrieved)
  }

  @Test
  fun addBook() {
    val mock1 = BookFactory.create()
    val mock2 = BookFactory.create()
    val firstInserted = register.addBook(mock1)
    val secondInserted = register.addBook(mock2)

    val containing = register.activeBooks()

    assertThat(containing).hasSize(2)

    val mock1WithUpdatedId = mock1.copy(id = firstInserted.id)
    val mock2WithUpdatedId = mock2.copy(id = secondInserted.id)

    assertThat(containing).doesNotContain(mock1, mock2)
    assertThat(containing).contains(mock1WithUpdatedId, mock2WithUpdatedId)
  }

  @Test
  fun addBookWithNullableAuthor() {
    val mock = BookFactory.create()
    val withNullableAuthor = mock.copy(author = null)
    val inserted = register.addBook(withNullableAuthor)

    assertThat(inserted)
        .isEqualTo(withNullableAuthor.copy(id = inserted.id))

    val retrieved = register.activeBooks()
        .single()

    assertThat(retrieved).isEqualTo(withNullableAuthor.copy(id = retrieved.id))
  }

  @Test
  fun updateBook() {
    val mock = BookFactory.create()
    val inserted = register.addBook(mock)

    val oldChapters = inserted.chapters
    val substracted = oldChapters.minus(oldChapters.first())
    val marks1 = SparseArrayCompat<String>().apply {
      put(15114, "The time has come")
      put(2361341, "This is another chapter")
    }
    val newChapters = substracted.plus(Chapter(File("/root/", "salkjsdg.mp3"), "askhsdglkjsdf", 113513516, 131351, marks1))

    val changed = inserted.copy(
        type = Book.Type.SINGLE_FILE,
        author = if (Random().nextBoolean()) "lkajsdflkj" else null,
        currentFile = newChapters.last().file,
        time = 135135135,
        name = "252587245",
        chapters = newChapters,
        playbackSpeed = 1.7f,
        root = "slksjdglkjgaölskjdf"
    )

    register.updateBook(changed)

    val containingBooks = register.activeBooks()

    // check that there is still only one book
    assertThat(containingBooks).hasSize(1)

    assertThat(containingBooks).doesNotContain(inserted)
    assertThat(containingBooks).contains(changed)
  }
}

