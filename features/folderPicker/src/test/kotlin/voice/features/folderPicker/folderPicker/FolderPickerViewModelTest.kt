package voice.features.folderPicker.folderPicker

import android.net.Uri
import android.provider.DocumentsContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import voice.core.data.folders.AudiobookFolders
import voice.core.data.folders.DocumentFileWithUri
import voice.core.data.folders.FolderType
import voice.core.documentfile.CachedDocumentFile
import voice.core.featureflag.MemoryFeatureFlag
import voice.core.kioskmode.KioskModeFeatureFlag
import voice.core.scanner.MediaScanTrigger
import voice.navigation.Destination
import voice.navigation.Navigator
import voice.navigation.Origin
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class FolderPickerViewModelTest {

  private val foldersWithAccess = MutableStateFlow<Map<FolderType, List<DocumentFileWithUri>>>(emptyMap())
  private val foldersWithoutAccess = MutableStateFlow<Map<FolderType, List<Uri>>>(emptyMap())
  private val audiobookFolders = mockk<AudiobookFolders>(relaxUnitFun = true) {
    every { all() } returns foldersWithAccess
    every { withoutAccess() } returns foldersWithoutAccess
  }
  private val navigator = mockk<Navigator>(relaxUnitFun = true)
  private val mediaScanTrigger = mockk<MediaScanTrigger>(relaxUnitFun = true)
  private val viewModel = FolderPickerViewModel(
    audiobookFolders = audiobookFolders,
    navigator = navigator,
    mediaScanTrigger = mediaScanTrigger,
    kioskModeFeatureFlag = KioskModeFeatureFlag(MemoryFeatureFlag(false)),
  )

  private val fantasy = tree("primary:Audiobooks/Fantasy")
  private val crime = tree("primary:Crime")

  @Test
  fun `a folder without access comes first and is named after its path`() = runTest {
    foldersWithAccess.value = mapOf(FolderType.Root to listOf(DocumentFileWithUri(documentFile("Crime"), crime)))
    foldersWithoutAccess.value = mapOf(FolderType.SingleFolder to listOf(fantasy))

    viewStates {
      assertEquals(
        expected = listOf(
          FolderPickerViewState.Item(
            name = "Audiobooks/Fantasy",
            id = fantasy,
            folderType = FolderType.SingleFolder,
            hasAccess = false,
          ),
          FolderPickerViewState.Item(name = "Crime", id = crime, folderType = FolderType.Root),
        ),
        actual = awaitLoaded().items,
      )
    }
  }

  @Test
  fun `a folder that just got access again is listed once`() = runTest {
    foldersWithAccess.value = mapOf(FolderType.Root to listOf(DocumentFileWithUri(documentFile("Crime"), crime)))
    foldersWithoutAccess.value = mapOf(FolderType.Root to listOf(crime))

    viewStates {
      assertEquals(
        expected = listOf(FolderPickerViewState.Item(name = "Crime", id = crime, folderType = FolderType.Root)),
        actual = awaitLoaded().items,
      )
    }
  }

  @Test
  fun `picking the same folder again allows access to it`() {
    viewModel.onAccessAllowed(uri = fantasy, folderType = FolderType.SingleFolder, pickedUri = fantasy)

    verify {
      audiobookFolders.add(fantasy, FolderType.SingleFolder)
      mediaScanTrigger.scan(restartIfScanning = true)
    }
    verify(exactly = 0) { audiobookFolders.remove(any(), any()) }
  }

  @Test
  fun `picking another folder replaces it and asks for its type again`() {
    viewModel.onAccessAllowed(uri = fantasy, folderType = FolderType.SingleFolder, pickedUri = crime)

    verify {
      audiobookFolders.remove(fantasy, FolderType.SingleFolder)
      navigator.goTo(Destination.SelectFolderType(uri = crime, origin = Origin.Default))
    }
    verify(exactly = 0) { audiobookFolders.add(any(), any()) }
  }

  @Test
  fun `picking another file replaces it`() {
    val file = document("primary:Audiobooks/book.m4b")
    val otherFile = document("primary:Download/book.m4b")

    viewModel.onAccessAllowed(uri = file, folderType = FolderType.SingleFile, pickedUri = otherFile)

    verify {
      audiobookFolders.remove(file, FolderType.SingleFile)
      audiobookFolders.add(otherFile, FolderType.SingleFile)
      mediaScanTrigger.scan(restartIfScanning = true)
    }
  }

  private suspend fun TestScope.viewStates(validate: suspend ReceiveTurbine<FolderPickerViewState>.() -> Unit) {
    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test(validate = validate)
  }

  private suspend fun ReceiveTurbine<FolderPickerViewState>.awaitLoaded(): FolderPickerViewState {
    var viewState = awaitItem()
    while (viewState.loading) {
      viewState = awaitItem()
    }
    return viewState
  }

  private fun documentFile(name: String): CachedDocumentFile = mockk {
    every { this@mockk.name } returns name
    every { isFile } returns false
  }

  private fun tree(documentId: String): Uri = DocumentsContract.buildTreeDocumentUri(AUTHORITY, documentId)

  private fun document(documentId: String): Uri = DocumentsContract.buildDocumentUri(AUTHORITY, documentId)

  private companion object {
    const val AUTHORITY = "com.android.externalstorage.documents"
  }
}
