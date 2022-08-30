package voice.folderPicker.selectType

import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import app.cash.turbine.test
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import voice.common.DispatcherProvider

@RunWith(AndroidJUnit4::class)
class SelectFolderTypeViewModelTest {

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun test() = runTest {
    val audiobookFolder = temporaryFolder.newFolder("audiobooks")
    with(temporaryFolder) {
      newFile("audiobooks/FirstBook.mp3")
      newFolder("audiobooks/SecondBook")
      newFile("audiobooks/SecondBook/1.mp3")
      newFile("audiobooks/SecondBook/2.mp3")
    }
    val viewModel = SelectFolderTypeViewModel(DispatcherProvider(coroutineContext, coroutineContext), mockk(), mockk())
    viewModel.args = SelectFolderTypeViewModel.Args(audiobookFolder.toUri(), DocumentFile.fromFile(audiobookFolder))
    viewModel.setFolderMode(FolderMode.Audiobooks)

    backgroundScope.launchMolecule(clock = RecompositionClock.Immediate) {
      viewModel.viewState()
    }.test {
      suspend fun expectItem(folderMode: FolderMode, vararg books: SelectFolderTypeViewState.Book) {
        with(awaitItem()) {
          this.books.shouldContainExactlyInAnyOrder(books.toList())
          this.selectedFolderMode shouldBe folderMode
        }
      }
      expectItem(FolderMode.Audiobooks)

      expectItem(
        FolderMode.Audiobooks,
        SelectFolderTypeViewState.Book("FirstBook", 1),
        SelectFolderTypeViewState.Book("SecondBook", 2),
      )

      viewModel.setFolderMode(FolderMode.SingleBook)

      expectItem(
        FolderMode.SingleBook,
        SelectFolderTypeViewState.Book("FirstBook", 1),
        SelectFolderTypeViewState.Book("SecondBook", 2),
      )

      expectItem(
        FolderMode.SingleBook,
        SelectFolderTypeViewState.Book("audiobooks", 3),
      )
    }
  }
}
