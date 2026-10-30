package voice.features.bookOverview.editBook

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import voice.core.data.Book
import voice.core.data.CustomMetadata
import voice.core.data.repo.BookRepository
import voice.core.featureflag.MemoryFeatureFlag
import voice.features.bookOverview.book
import voice.features.bookOverview.bottomSheet.BottomSheetItem
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EditBookViewModelTest {

  private val dispatcher = StandardTestDispatcher()
  private val book = book(name = "Dune", author = "Herbert", series = "Dune Chronicles", part = "1")
  private val update = slot<(CustomMetadata) -> CustomMetadata>()
  private val repo = mockk<BookRepository> {
    coEvery { get(book.id) } returns book
    coEvery { updateCustomMetadata(book.id, capture(update)) } returns Unit
  }
  private val editSeriesFeatureFlag = MemoryFeatureFlag(true)
  private val viewModel = EditBookViewModel(repo, EditSeriesFeatureFlag(editSeriesFeatureFlag))

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @AfterTest
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `the sheet starts with the title and author of the book`() = runTest(dispatcher) {
    viewModel.onItemClick(book.id, BottomSheetItem.Edit)

    assertEquals(expected = "Dune", actual = viewModel.state.value?.title)
    assertEquals(expected = "Herbert", actual = viewModel.state.value?.author)
  }

  @Test
  fun `the sheet starts with the series and part of the book`() = runTest(dispatcher) {
    viewModel.onItemClick(book.id, BottomSheetItem.Edit)

    assertEquals(expected = "Dune Chronicles", actual = viewModel.state.value?.series)
    assertEquals(expected = "1", actual = viewModel.state.value?.part)
    assertTrue(viewModel.state.value!!.showSeries)
  }

  @Test
  fun `the series stays hidden without the feature flag`() = runTest(dispatcher) {
    editSeriesFeatureFlag.value = false

    viewModel.onItemClick(book.id, BottomSheetItem.Edit)

    assertFalse(viewModel.state.value!!.showSeries)
  }

  @Test
  fun `saving keeps the series and part the listener typed`() = runTest(dispatcher) {
    val saved = save(series = " Dune ", part = " 2 ")

    assertEquals(expected = "Dune", actual = saved.series)
    assertEquals(expected = "2", actual = saved.part)
  }

  @Test
  fun `an empty series goes back to the one from the files`() = runTest(dispatcher) {
    val edited = book.copy(custom = book.custom.copy(series = "Dune Saga", part = "3"))

    val saved = save(series = "", part = " ", book = edited)

    assertNull(saved.custom.series)
    assertNull(saved.custom.part)
    assertEquals(expected = "Dune Chronicles", actual = saved.series)
  }

  @Test
  fun `changing only the title leaves the series from the files alone`() = runTest(dispatcher) {
    val saved = save(title = "Dune Messiah")

    assertNull(saved.custom.series)
    assertNull(saved.custom.part)
  }

  @Test
  fun `saving keeps the title and author the listener typed`() = runTest(dispatcher) {
    val saved = save(title = " Dune Messiah ", author = " Frank Herbert ")

    assertEquals(expected = "Dune Messiah", actual = saved.name)
    assertEquals(expected = "Frank Herbert", actual = saved.author)
    assertNull(viewModel.state.value)
  }

  @Test
  fun `an empty author goes back to the one from the files`() = runTest(dispatcher) {
    val edited = book.copy(custom = book.custom.copy(author = "Frank Herbert"))

    val saved = save(title = "Dune", author = " ", book = edited)

    assertNull(saved.custom.author)
    assertEquals(expected = "Herbert", actual = saved.author)
  }

  @Test
  fun `changing only the title leaves the author from the files alone`() = runTest(dispatcher) {
    val untrimmed = book.update { it.copy(metadata = it.metadata.copy(author = "Herbert ")) }

    val saved = save(title = "Dune Messiah", author = "Herbert ", book = untrimmed)

    assertEquals(expected = "Dune Messiah", actual = saved.name)
    assertNull(saved.custom.author)
  }

  @Test
  fun `changing only the author leaves the title from the files alone`() = runTest(dispatcher) {
    val saved = save(title = "Dune", author = "Frank Herbert")

    assertNull(saved.custom.name)
    assertEquals(expected = "Frank Herbert", actual = saved.author)
  }

  private suspend fun TestScope.save(
    title: String? = null,
    author: String? = null,
    series: String? = null,
    part: String? = null,
    book: Book = this@EditBookViewModelTest.book,
  ): Book {
    coEvery { repo.get(book.id) } returns book
    viewModel.onItemClick(book.id, BottomSheetItem.Edit)
    title?.let(viewModel::onTitleChange)
    author?.let(viewModel::onAuthorChange)
    series?.let(viewModel::onSeriesChange)
    part?.let(viewModel::onPartChange)
    viewModel.onConfirm()
    advanceUntilIdle()
    coVerify { repo.updateCustomMetadata(book.id, any()) }
    return book.copy(custom = update.captured(book.custom))
  }
}
