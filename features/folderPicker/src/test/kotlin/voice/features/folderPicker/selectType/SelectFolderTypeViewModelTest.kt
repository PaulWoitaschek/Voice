package voice.features.folderPicker.selectType

import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import voice.core.common.DispatcherProvider
import voice.core.data.folders.AudiobookFolders
import voice.core.data.folders.FolderType
import voice.core.documentfile.FileBasedDocumentFactory
import voice.core.documentfile.nameWithoutExtension
import voice.core.scanner.BookPreview
import voice.core.scanner.BookPreviewer
import voice.navigation.Destination
import voice.navigation.Navigator
import voice.navigation.Origin
import java.io.File
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

@RunWith(AndroidJUnit4::class)
class SelectFolderTypeViewModelTest {

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  private val audiobookFolders = mockk<AudiobookFolders>(relaxed = true)
  private val navigator = mockk<Navigator>(relaxed = true)
  private val previewed = mutableListOf<String>()
  private var preview: suspend (name: String) -> BookPreview? = { name -> tagged(name) }
  private val bookPreviewer = BookPreviewer { file ->
    val name = file.nameWithoutExtension()
    previewed += name
    preview(name)
  }

  @Test
  fun `the guessed mode is selected and shows its books`() = runTest {
    val folder = library()
    val viewModel = viewModel(folder)

    viewStates(viewModel) {
      val viewState = awaitLoaded()
      assertEquals(expected = "audiobooks", actual = viewState.folderName)
      assertEquals(expected = FolderMode.Audiobooks, actual = viewState.selectedMode)
      assertEquals(expected = FolderMode.Audiobooks, actual = viewState.guessedMode)
      assertEquals(
        expected = listOf(book("FirstBook", fileCount = 1), book("SecondBook", fileCount = 2)),
        actual = viewState.unanalyzedBooks(),
      )
    }
  }

  @Test
  fun `selecting another mode shows its books`() = runTest {
    val folder = library()
    val viewModel = viewModel(folder)

    viewStates(viewModel) {
      awaitLoaded()
      viewModel.selectMode(FolderMode.SingleBook)
      val viewState = awaitLoaded()
      assertEquals(expected = FolderMode.SingleBook, actual = viewState.selectedMode)
      assertEquals(expected = listOf(book("audiobooks", fileCount = 3)), actual = viewState.unanalyzedBooks())
    }
  }

  @Test
  fun `books fill in what the library will show once they are analyzed`() = runTest {
    val folder = library()
    val viewModel = viewModel(folder)

    viewStates(viewModel) {
      var viewState = awaitLoaded()
      while (viewState.books.any { it.duration == null }) {
        viewState = awaitItem()
      }
      assertEquals(
        expected = listOf(
          book("Tagged FirstBook", fileCount = 1).copy(author = "Author", duration = 2.hours),
          book("Tagged SecondBook", fileCount = 2).copy(author = "Author", duration = 2.hours),
        ),
        actual = viewState.books.sortedBy { it.name },
      )
    }
  }

  @Test
  fun `the book being analyzed is marked`() = runTest {
    val analyzed = CompletableDeferred<Unit>()
    preview = { name ->
      analyzed.await()
      tagged(name)
    }
    val viewModel = viewModel(library())

    viewStates(viewModel) {
      assertEquals(expected = 1, actual = awaitLoaded().books.count { it.analyzing })
      analyzed.complete(Unit)
      val viewState = awaitAnalyzed()
      assertTrue(viewState.books.none { it.analyzing })
    }
  }

  @Test
  fun `books without playable files are left out like in the library`() = runTest {
    preview = { name -> if (name == "FirstBook") null else tagged(name) }
    val viewModel = viewModel(library())

    viewStates(viewModel) {
      val viewState = awaitAnalyzed()
      assertEquals(expected = listOf("Tagged SecondBook"), actual = viewState.books.map { it.name })
      assertEquals(
        expected = listOf("Tagged SecondBook"),
        actual = viewState.options.single { it.mode == FolderMode.Audiobooks }.books.map { it.name },
      )
    }
  }

  @Test
  fun `a book that can't be analyzed keeps what its files tell`() = runTest {
    preview = { name -> if (name == "FirstBook") throw IOException("Can't read $name") else tagged(name) }
    val viewModel = viewModel(library())

    viewStates(viewModel) {
      val viewState = awaitAnalyzed()
      assertEquals(
        expected = listOf(
          book("FirstBook", fileCount = 1),
          book("Tagged SecondBook", fileCount = 2).copy(author = "Author", duration = 2.hours),
        ),
        actual = viewState.books.sortedBy { it.name },
      )
    }
  }

  @Test
  fun `the analyzed author replaces the one from the folder`() = runTest {
    preview = { name -> tagged(name).copy(author = null) }
    val folder = temporaryFolder.newFolder("authors")
    with(temporaryFolder) {
      newFolder("authors", "Dan Simmons", "Hyperion")
      newFile("authors/Dan Simmons/Hyperion/1.mp3")
    }
    val viewModel = viewModel(folder)

    viewStates(viewModel) {
      awaitLoaded()
      viewModel.selectMode(FolderMode.Authors)
      var viewState = awaitAnalyzed()
      while (viewState.selectedMode != FolderMode.Authors || viewState.books.any { it.duration == null }) {
        viewState = awaitItem()
      }
      assertEquals(
        expected = listOf(book("Tagged Hyperion", fileCount = 1).copy(duration = 2.hours)),
        actual = viewState.books,
      )
    }
  }

  @Test
  fun `only the books of the selected mode are analyzed, and each only once`() = runTest {
    val folder = library()
    val viewModel = viewModel(folder)

    viewStates(viewModel) {
      awaitLoaded()
      viewModel.selectMode(FolderMode.SingleBook)
      var viewState = awaitLoaded()
      while (viewState.selectedMode != FolderMode.SingleBook || viewState.books.any { it.duration == null }) {
        viewState = awaitItem()
      }
      viewModel.selectMode(FolderMode.Audiobooks)
      while (viewState.selectedMode != FolderMode.Audiobooks || viewState.books.any { it.duration == null }) {
        viewState = awaitItem()
      }
    }

    assertEquals(expected = listOf("FirstBook", "SecondBook", "audiobooks"), actual = previewed.sorted())
  }

  @Test
  fun `adding during the onboarding adds the selected mode and completes it`() = runTest {
    val folder = library()
    val viewModel = viewModel(folder, origin = Origin.Onboarding)

    viewStates(viewModel) {
      assertEquals(expected = true, actual = awaitLoaded().onboarding)
    }
    viewModel.add()

    verify {
      audiobookFolders.add(folder.toUri(), FolderType.Root)
      navigator.goTo(Destination.OnboardingCompletion)
    }
  }

  @Test
  fun `changing the mode of an added folder saves it and goes back`() = runTest {
    val folder = library()
    val viewModel = viewModel(folder, currentType = FolderType.Root)

    viewStates(viewModel) {
      val viewState = awaitLoaded()
      assertEquals(expected = true, actual = viewState.editing)
      assertEquals(expected = FolderMode.Audiobooks, actual = viewState.selectedMode)
    }
    viewModel.selectMode(FolderMode.SingleBook)
    viewModel.add()

    verify {
      audiobookFolders.add(folder.toUri(), FolderType.SingleFolder)
      navigator.goBack()
    }
  }

  @Test
  fun `keeping the mode of an added folder only goes back`() = runTest {
    val folder = library()
    val viewModel = viewModel(folder, currentType = FolderType.SingleFolder)

    viewStates(viewModel) {
      assertEquals(expected = FolderMode.SingleBook, actual = awaitLoaded().selectedMode)
    }
    viewModel.add()

    verify(exactly = 0) { audiobookFolders.add(any(), any()) }
    verify { navigator.goBack() }
  }

  private fun library(): File {
    val folder = temporaryFolder.newFolder("audiobooks")
    with(temporaryFolder) {
      newFile("audiobooks/FirstBook.mp3")
      newFolder("audiobooks/SecondBook")
      newFile("audiobooks/SecondBook/1.mp3")
      newFile("audiobooks/SecondBook/2.mp3")
    }
    return folder
  }

  private fun TestScope.viewModel(
    folder: File,
    origin: Origin = Origin.Default,
    currentType: FolderType? = null,
  ) = SelectFolderTypeViewModel(
    dispatcherProvider = DispatcherProvider(coroutineContext, coroutineContext, coroutineContext),
    audiobookFolders = audiobookFolders,
    navigator = navigator,
    documentFileFactory = FileBasedDocumentFactory,
    bookPreviewer = bookPreviewer,
    uri = folder.toUri(),
    documentFile = DocumentFile.fromFile(folder),
    origin = origin,
    currentType = currentType,
  )

  private suspend fun TestScope.viewStates(
    viewModel: SelectFolderTypeViewModel,
    validate: suspend ReceiveTurbine<SelectFolderTypeViewState>.() -> Unit,
  ) {
    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      validate()
      // the books keep filling in after what a test looks at
      cancelAndIgnoreRemainingEvents()
    }
  }

  private suspend fun ReceiveTurbine<SelectFolderTypeViewState>.awaitAnalyzed(): SelectFolderTypeViewState {
    var viewState = awaitLoaded()
    while (viewState.books.any { it.analyzing }) {
      viewState = awaitItem()
    }
    return viewState
  }

  @IgnorableReturnValue
  private suspend fun ReceiveTurbine<SelectFolderTypeViewState>.awaitLoaded(): SelectFolderTypeViewState {
    var viewState = awaitItem()
    while (viewState.loading || viewState.folderName.isEmpty()) {
      viewState = awaitItem()
    }
    return viewState
  }

  // what the files tell, whichever book is analyzed first
  private fun SelectFolderTypeViewState.unanalyzedBooks(): List<SelectFolderTypeViewState.Book> {
    return books.map { it.copy(analyzing = false) }.sortedBy { it.name }
  }

  private fun tagged(name: String) = BookPreview(name = "Tagged $name", author = "Author", duration = 2.hours)

  private fun book(
    name: String,
    fileCount: Int,
  ) = SelectFolderTypeViewState.Book(
    name = name,
    author = null,
    fileCount = fileCount,
    partCount = 0,
    possibleBookCount = 0,
  )
}
