package voice.features.cover

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.features.cover.SelectCoverFromInternetViewModel.Events
import voice.features.cover.SelectCoverFromInternetViewModel.Results
import voice.features.cover.api.CoverApi
import voice.features.cover.api.SearchResponse
import voice.navigation.Navigator
import kotlin.test.assertEquals
import kotlin.test.assertIs

@RunWith(AndroidJUnit4::class)
class SelectCoverFromInternetViewModelTest {

  private val bookId = BookId("book")
  private val cover = SearchResponse.ImageResult(width = 600, height = 600, image = "image", thumbnail = "thumbnail")

  private val book = mockk<Book> {
    every { content.name } returns "Dune"
    every { content.author } returns null
  }
  private val bookRepository = mockk<BookRepository> {
    coEvery { get(bookId) } returns book
  }
  private val coverDownloader = mockk<CoverDownloader>()

  private fun viewModel(api: CoverApi) = SelectCoverFromInternetViewModel(
    api = api,
    bookRepository = bookRepository,
    navigator = mockk<Navigator>(relaxed = true),
    context = ApplicationProvider.getApplicationContext<Context>(),
    coverDownloader = coverDownloader,
    bookId = bookId,
  )

  @Test
  fun `retry loads the covers after the search failed`() = runTest {
    val api = mockk<CoverApi> {
      coEvery { token(any()) } returnsMany listOf(null, "token")
      coEvery { search(any(), any(), any()) } returns SearchResponse(next = null, results = listOf(cover))
    }
    val viewModel = viewModel(api)
    val events = MutableSharedFlow<Events>(extraBufferCapacity = 1)
    val state = backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState(events)
    }
    settle()
    assertEquals(Results.Error, state.value.results)

    events.emit(Events.Retry)
    settle()

    val results = assertIs<Results.Content>(state.value.results)
    assertEquals(listOf(cover), results.items.itemSnapshotList.items)
  }

  @Test
  fun `a cover that can't be downloaded is reported`() = runTest {
    val api = mockk<CoverApi> {
      coEvery { token(any()) } returns "token"
      coEvery { search(any(), any(), any()) } returns SearchResponse(next = null, results = listOf(cover))
    }
    coEvery { coverDownloader.download(any()) } returns null
    val viewModel = viewModel(api)
    val events = MutableSharedFlow<Events>(extraBufferCapacity = 1)
    val state = backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState(events)
    }
    settle()

    events.emit(Events.CoverClick(cover))
    settle()

    assertEquals(null, state.value.downloading)
    assertEquals(true, state.value.downloadFailed)
  }

  // paging hands its pages to the main looper, which only runs when it is idled.
  // runCurrent rather than advanceUntilIdle, which skips the background scope the presenter runs in.
  private fun TestScope.settle() {
    repeat(5) {
      shadowOf(Looper.getMainLooper()).idle()
      testScheduler.runCurrent()
    }
  }
}
