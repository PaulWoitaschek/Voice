package voice.core.data.folders

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.provider.DocumentsContract
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class AudiobookFoldersImplTest {

  private val stores = FolderType.entries.associateWith { MemoryDataStore(emptySet<Uri>()) }
  private val withAccess = mutableSetOf<Uri>()
  private val uri = DocumentsContract.buildTreeDocumentUri("com.android.externalstorage.documents", "primary:Audiobooks")

  private fun folders(context: Context = ApplicationProvider.getApplicationContext()) = AudiobookFoldersImpl(
    rootAudioBookFoldersStore = RootAudiobookFoldersStore(stores.getValue(FolderType.Root)),
    singleFolderAudiobookFoldersStore = SingleFolderAudiobookFoldersStore(stores.getValue(FolderType.SingleFolder)),
    singleFileAudiobookFoldersStore = SingleFileAudiobookFoldersStore(stores.getValue(FolderType.SingleFile)),
    authorAudiobookFoldersStore = AuthorAudiobookFoldersStore(stores.getValue(FolderType.Author)),
    context = context,
    cachedDocumentFileFactory = mockk {
      every { create(any()) } returns mockk()
    },
    analytics = mockk(relaxUnitFun = true),
    persistedUriPermissions = object : PersistedUriPermissions {
      override fun persistedUris(): Set<Uri> = withAccess.toSet()
    },
  )

  @Test
  fun `adding a folder again with another type moves it`() = runTest {
    val folders = folders()

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

  @Test
  fun `a folder is kept with the permission to write`() {
    val resolver = mockk<ContentResolver>(relaxUnitFun = true)

    folders(context(resolver)).add(uri, FolderType.Root)

    verify { resolver.takePersistableUriPermission(uri, match { it and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0 }) }
  }

  @Test
  fun `a folder the picker only granted reading is kept with the permission to read`() = runTest {
    val resolver = mockk<ContentResolver>(relaxUnitFun = true)
    every {
      resolver.takePersistableUriPermission(uri, match { it and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0 })
    } throws SecurityException()

    folders(context(resolver)).add(uri, FolderType.Root)
    shadowOf(Looper.getMainLooper()).idle()

    verify { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    assertEquals(setOf(uri), stores.getValue(FolderType.Root).data.first())
  }

  @Test
  fun `a stored folder without access is only listed as without access`() = runTest {
    stores.getValue(FolderType.Root).updateData { setOf(uri) }
    val folders = folders()

    assertEquals(emptyList(), folders.all().first().getValue(FolderType.Root))
    assertEquals(listOf(uri), folders.withoutAccess().first().getValue(FolderType.Root))
  }

  @Test
  fun `allowing access again to a stored folder lists it with access`() = runTest {
    stores.getValue(FolderType.Root).updateData { setOf(uri) }
    val folders = folders()

    folders.withoutAccess().test {
      assertEquals(listOf(uri), awaitItem().getValue(FolderType.Root))

      withAccess += uri
      folders.add(uri, FolderType.Root)

      assertEquals(emptyList(), awaitItem().getValue(FolderType.Root))
    }
    assertEquals(listOf(uri), folders.all().first().getValue(FolderType.Root).map { it.uri })
  }

  @Test
  fun `removing a folder without access drops it`() = runTest {
    stores.getValue(FolderType.Root).updateData { setOf(uri) }
    val folders = folders()

    folders.remove(uri, FolderType.Root)
    shadowOf(Looper.getMainLooper()).idle()

    assertEquals(emptyList(), folders.withoutAccess().first().getValue(FolderType.Root))
  }

  private fun context(resolver: ContentResolver): Context = mockk {
    every { contentResolver } returns resolver
  }
}
