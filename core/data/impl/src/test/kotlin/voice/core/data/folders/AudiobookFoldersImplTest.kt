package voice.core.data.folders

import android.net.Uri
import android.os.Looper
import android.provider.DocumentsContract
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import voice.core.analytics.api.Analytics
import voice.core.documentfile.CachedDocumentFileFactory
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class AudiobookFoldersImplTest {

  private val stores = FolderType.entries.associateWith { MemoryDataStore(emptySet<Uri>()) }
  private val folders = AudiobookFoldersImpl(
    rootAudioBookFoldersStore = stores.getValue(FolderType.Root),
    singleFolderAudiobookFoldersStore = stores.getValue(FolderType.SingleFolder),
    singleFileAudiobookFoldersStore = stores.getValue(FolderType.SingleFile),
    authorAudiobookFoldersStore = stores.getValue(FolderType.Author),
    context = ApplicationProvider.getApplicationContext(),
    cachedDocumentFileFactory = object : CachedDocumentFileFactory {
      override fun create(uri: Uri) = error("not needed")
    },
    analytics = object : Analytics {
      override fun screenView(screenName: String) {}

      override fun event(
        name: String,
        params: Map<String, String>,
      ) {}
    },
    persistedUriPermissions = object : PersistedUriPermissions {
      override fun persistedUris(): Set<Uri> = emptySet()
    },
  )

  @Test
  fun `adding a folder again with another type moves it`() = runTest {
    val uri = DocumentsContract.buildTreeDocumentUri("com.android.externalstorage.documents", "primary:Audiobooks")

    folders.add(uri, FolderType.SingleFolder)
    shadowOf(Looper.getMainLooper()).idle()
    folders.add(uri, FolderType.Root)
    shadowOf(Looper.getMainLooper()).idle()

    assertEquals(
      expected = mapOf(
        FolderType.SingleFile to emptySet(),
        FolderType.SingleFolder to emptySet(),
        FolderType.Root to setOf(uri),
        FolderType.Author to emptySet(),
      ),
      actual = stores.mapValues { it.value.data.first() },
    )
  }
}
