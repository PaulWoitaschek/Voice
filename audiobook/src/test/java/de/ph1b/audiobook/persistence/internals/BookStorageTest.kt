package de.ph1b.audiobook.persistence.internals

import android.os.Build
import android.util.SparseArray
import com.squareup.moshi.Moshi
import de.ph1b.audiobook.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import java.io.File
import java.util.*


/**
 * Simple test for book persistence.
 *
 * @author Paul Woitaschek
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, sdk = intArrayOf(Build.VERSION_CODES.LOLLIPOP), manifest = "src/main/AndroidManifest.xml", application = TestApp::class)
class BookStorageTest {

  init {
    ShadowLog.stream = System.out
  }

  lateinit var register: BookStorage

  @Before
  fun setUp() {
    val internalDb = InternalDb(RuntimeEnvironment.application)
    register = BookStorage(internalDb, Moshi.Builder().build())
  }

  @Test fun testHideRevealBook() {
    val mock = BookMocker.mock(-1)
    register.addBook(mock)

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
  @Test fun testAddBookReturn() {
    val mock = BookMocker.mock(-1)
    val inserted = register.addBook(mock)
    val retrieved = register.activeBooks().single()

    assertThat(inserted).isEqualTo(retrieved)
  }

  @Test fun testAddBook() {
    val mock1 = BookMocker.mock(-1)
    val mock2 = BookMocker.mock(-1)
    val firstInserted = register.addBook(mock1)
    val secondInserted = register.addBook(mock2)

    val containing = register.activeBooks()

    assertThat(containing).hasSize(2)

    val mock1WithUpdatedId = mock1.copy(id = firstInserted.id)
    val mock2WithUpdatedId = mock2.copy(id = secondInserted.id)

    assertThat(containing).doesNotContain(mock1, mock2)
    assertThat(containing).contains(mock1WithUpdatedId, mock2WithUpdatedId)
  }

  @Test fun testAddBookWithNullableAuthor() {
    val mock = BookMocker.mock(-1)
    val withNullableAuthor = mock.copy(author = null)
    val inserted = register.addBook(withNullableAuthor)

    assertThat(inserted)
        .isEqualTo(withNullableAuthor.copy(id = inserted.id))

    val retrieved = register.activeBooks()
        .single()

    assertThat(retrieved).isEqualTo(withNullableAuthor.copy(id = retrieved.id))
  }


  @Test fun testUpdateBook() {
    val mock = BookMocker.mock(-1)
    val inserted = register.addBook(mock)

    val oldChapters = inserted.chapters
    val substracted = oldChapters.minus(oldChapters.first())
    val marks1 = SparseArray<String>().apply {
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
        root = "slksjdglkjgaölskjdf")

    register.updateBook(changed)

    val containingBooks = register.activeBooks()

    // check that there is still only one book
    assertThat(containingBooks).hasSize(1)

    assertThat(containingBooks).doesNotContain(inserted)
    assertThat(containingBooks).contains(changed)
  }
}