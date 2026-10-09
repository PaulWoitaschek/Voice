package voice.features.bookOverview.deleteBook

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.features.bookOverview.book
import voice.features.bookOverview.bottomSheet.BottomSheetItem
import voice.navigation.Destination
import voice.navigation.Navigator
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class DeleteBookViewModelTest {

  private val dispatcher = StandardTestDispatcher()
  private val book = book().let {
    it.copy(content = it.content.copy(id = BookId("content://$AUTHORITY/document/book")))
  }
  private val navigator = mockk<Navigator>(relaxUnitFun = true)

  private val viewModel = DeleteBookViewModel(
    application = ApplicationProvider.getApplicationContext(),
    mediaScanTrigger = mockk(relaxed = true),
    repo = mockk<BookRepository> {
      coEvery { get(book.id) } returns book
    },
    navigator = navigator,
  )

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @AfterTest
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `deleting a book closes its player`() = runTest(dispatcher) {
    Robolectric.setupContentProvider(DocumentsProvider::class.java, AUTHORITY)

    delete()

    verify { navigator.remove(Destination.Playback(book.id)) }
  }

  @Test
  fun `a failed deletion keeps the player`() = runTest(dispatcher) {
    delete()

    verify(exactly = 0) { navigator.remove(any()) }
  }

  private suspend fun TestScope.delete() {
    viewModel.onItemClick(book.id, BottomSheetItem.DeleteBook)
    viewModel.onDeleteCheckBoxCheck(true)
    viewModel.onConfirmDeletion()
    advanceUntilIdle()
  }

  /** Answers every call, which is how a documents provider confirms a deletion. */
  class DocumentsProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun call(
      method: String,
      arg: String?,
      extras: Bundle?,
    ): Bundle = Bundle()

    override fun query(
      uri: Uri,
      projection: Array<out String>?,
      selection: String?,
      selectionArgs: Array<out String>?,
      sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(
      uri: Uri,
      values: ContentValues?,
    ): Uri? = null

    override fun delete(
      uri: Uri,
      selection: String?,
      selectionArgs: Array<out String>?,
    ): Int = 0

    override fun update(
      uri: Uri,
      values: ContentValues?,
      selection: String?,
      selectionArgs: Array<out String>?,
    ): Int = 0
  }
}

private const val AUTHORITY = "voice.test.documents"
