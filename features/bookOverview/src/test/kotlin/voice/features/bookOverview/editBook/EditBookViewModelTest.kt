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
import voice.core.audiobookshelf.ServerEdit
import voice.core.audiobookshelf.ServerLibrary
import voice.core.data.Book
import voice.core.data.BookId
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
    coEvery { all() } returns listOf(book)
    coEvery { updateCustomMetadata(book.id, capture(update)) } returns Unit
  }
  private val editSeriesFeatureFlag = MemoryFeatureFlag(true)
  private val serverLibrary = mockk<ServerLibrary> {
    coEvery { updateSeries(any(), any(), any()) } returns ServerEdit.Saved
  }
  private val viewModel = EditBookViewModel(repo, EditSeriesFeatureFlag(editSeriesFeatureFlag), serverLibrary)
  private val serverBook = book(name = "Leviathan Wakes", series = "The Expanse", part = "1", id = BookId("abs://item/item"))

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
  fun `typing suggests the series of the other books, those starting with it first`() = runTest(dispatcher) {
    coEvery { repo.all() } returns listOf(
      book,
      book(series = "The Expanse"),
      book(series = "the expanse "),
      book(series = "The Expanse"),
      book(series = "Expeditionary Force"),
      book(series = "Alien Explorers"),
      book(series = "Discworld"),
      book(series = null),
    )
    viewModel.onItemClick(book.id, BottomSheetItem.Edit)

    viewModel.onSeriesChange("exp")

    assertEquals(
      expected = listOf(
        SeriesSuggestion("Expeditionary Force", bookCount = 1),
        SeriesSuggestion("Alien Explorers", bookCount = 1),
        SeriesSuggestion("The Expanse", bookCount = 3),
      ),
      actual = viewModel.state.value?.suggestions,
    )
  }

  @Test
  fun `at most three series are suggested, in natural order`() = runTest(dispatcher) {
    coEvery { repo.all() } returns listOf(
      book,
      book(series = "Saga 10"),
      book(series = "Saga 2"),
      book(series = "Saga 1"),
      book(series = "Saga 3"),
    )
    viewModel.onItemClick(book.id, BottomSheetItem.Edit)

    viewModel.onSeriesChange("saga")

    assertEquals(
      expected = listOf("Saga 1", "Saga 2", "Saga 3"),
      actual = viewModel.state.value?.suggestions?.map { it.name },
    )
  }

  @Test
  fun `nothing is suggested before the listener changes the series`() = runTest(dispatcher) {
    coEvery { repo.all() } returns listOf(book, book(series = "Dune Chronicles Extras"))

    viewModel.onItemClick(book.id, BottomSheetItem.Edit)

    assertEquals(expected = emptyList(), actual = viewModel.state.value?.suggestions)
  }

  @Test
  fun `a series typed in full isn't suggested again`() = runTest(dispatcher) {
    coEvery { repo.all() } returns listOf(book, book(series = "Foundation"), book(series = "Foundation Sequels"))
    viewModel.onItemClick(book.id, BottomSheetItem.Edit)

    viewModel.onSeriesChange("foundation ")

    assertEquals(
      expected = listOf(SeriesSuggestion("Foundation Sequels", bookCount = 1)),
      actual = viewModel.state.value?.suggestions,
    )
  }

  @Test
  fun `the series of the edited book isn't suggested`() = runTest(dispatcher) {
    viewModel.onItemClick(book.id, BottomSheetItem.Edit)

    viewModel.onSeriesChange("Dune")

    assertEquals(expected = emptyList(), actual = viewModel.state.value?.suggestions)
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

  @Test
  fun `server books can be edited only with the feature flag`() = runTest(dispatcher) {
    assertEquals(listOf(BottomSheetItem.Edit), viewModel.items(serverBook.id))

    editSeriesFeatureFlag.value = false

    assertEquals(emptyList(), viewModel.items(serverBook.id))
  }

  @Test
  fun `the series of a server book is saved on the server, and the sheet stays until it's saved`() = runTest(dispatcher) {
    openServerBook()
    viewModel.onSeriesChange("Expanse")
    viewModel.onPartChange("2")

    viewModel.onConfirm()

    assertTrue(viewModel.state.value!!.saving)
    advanceUntilIdle()
    coVerify { val _ = serverLibrary.updateSeries(serverBook.id, "Expanse", "2") }
    assertEquals(ServerEdit.Saved, viewModel.state.value?.serverResult)
    assertFalse(viewModel.state.value!!.saving)
  }

  @Test
  fun `a server that didn't take the series keeps what the listener typed`() = runTest(dispatcher) {
    coEvery { serverLibrary.updateSeries(any(), any(), any()) } returns ServerEdit.NotAllowed
    openServerBook()
    viewModel.onSeriesChange("Expanse")

    viewModel.onConfirm()
    advanceUntilIdle()

    assertEquals(ServerEdit.NotAllowed, viewModel.state.value?.serverResult)
    assertEquals("Expanse", viewModel.state.value?.series)
  }

  @Test
  fun `saving a server book without changes doesn't ask the server`() = runTest(dispatcher) {
    openServerBook()

    viewModel.onConfirm()
    advanceUntilIdle()

    assertEquals(ServerEdit.Saved, viewModel.state.value?.serverResult)
    coVerify(exactly = 0) { val _ = serverLibrary.updateSeries(any(), any(), any()) }
  }

  private suspend fun openServerBook() {
    coEvery { repo.get(serverBook.id) } returns serverBook
    viewModel.onItemClick(serverBook.id, BottomSheetItem.Edit)
    assertTrue(viewModel.state.value!!.remote)
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
