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
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import voice.core.common.DispatcherProvider
import voice.core.data.folders.AudiobookFolders
import voice.core.data.folders.FolderType
import voice.core.documentfile.FileBasedDocumentFactory
import voice.navigation.Destination
import voice.navigation.Navigator
import voice.navigation.Origin
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class SelectFolderTypeViewModelTest {

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  private val audiobookFolders = mockk<AudiobookFolders>(relaxed = true)
  private val navigator = mockk<Navigator>(relaxed = true)

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
        actual = viewState.books.sortedBy { it.name },
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
      assertEquals(expected = listOf(book("audiobooks", fileCount = 3)), actual = viewState.books)
    }
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
    }.test(validate = validate)
  }

  @IgnorableReturnValue
  private suspend fun ReceiveTurbine<SelectFolderTypeViewState>.awaitLoaded(): SelectFolderTypeViewState {
    var viewState = awaitItem()
    while (viewState.loading || viewState.folderName.isEmpty()) {
      viewState = awaitItem()
    }
    return viewState
  }

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
