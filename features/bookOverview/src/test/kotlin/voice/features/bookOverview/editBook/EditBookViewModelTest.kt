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
import voice.core.data.BookContent
import voice.core.data.repo.BookRepository
import voice.features.bookOverview.book
import voice.features.bookOverview.bottomSheet.BottomSheetItem
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class EditBookViewModelTest {

  private val dispatcher = StandardTestDispatcher()
  private val book = book(name = "Dune", author = "Herbert")
  private val update = slot<(BookContent) -> BookContent>()
  private val repo = mockk<BookRepository> {
    coEvery { get(book.id) } returns book
    coEvery { updateBook(book.id, capture(update)) } returns Unit
  }
  private val viewModel = EditBookViewModel(repo)

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
  fun `saving keeps the title and author the listener typed`() = runTest(dispatcher) {
    val saved = save(title = " Dune Messiah ", author = " Frank Herbert ")

    assertEquals(expected = "Dune Messiah", actual = saved.name)
    assertEquals(expected = "Frank Herbert", actual = saved.author)
    assertNull(viewModel.state.value)
  }

  @Test
  fun `an empty author goes back to the one from the files`() = runTest(dispatcher) {
    val edited = book.content.withCustomAuthor("Frank Herbert")

    val saved = save(title = "Dune", author = " ", content = edited)

    assertNull(saved.customAuthor)
    assertEquals(expected = "Herbert", actual = saved.author)
  }

  @Test
  fun `changing only the title leaves the author from the files alone`() = runTest(dispatcher) {
    val untrimmed = book.content.copy(metadata = book.content.metadata.copy(author = "Herbert "))
    coEvery { repo.get(book.id) } returns book.copy(content = untrimmed)

    val saved = save(title = "Dune Messiah", author = "Herbert ", content = untrimmed)

    assertEquals(expected = "Dune Messiah", actual = saved.name)
    assertNull(saved.customAuthor)
  }

  @Test
  fun `changing only the author leaves the title from the files alone`() = runTest(dispatcher) {
    val saved = save(title = "Dune", author = "Frank Herbert")

    assertNull(saved.customName)
    assertEquals(expected = "Frank Herbert", actual = saved.author)
  }

  private suspend fun TestScope.save(
    title: String,
    author: String,
    content: BookContent = book.content,
  ): BookContent {
    viewModel.onItemClick(book.id, BottomSheetItem.Edit)
    viewModel.onTitleChange(title)
    viewModel.onAuthorChange(author)
    viewModel.onConfirm()
    advanceUntilIdle()
    coVerify { repo.updateBook(book.id, any()) }
    return update.captured(content)
  }
}
