package voice.core.foldersync

import android.content.ContentResolver
import android.database.MatrixCursor
import android.os.Bundle
import android.provider.DocumentsContract.Document
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertNull
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncFilesTest {

  private val contentResolver = mockk<ContentResolver>()
  private val syncFiles =
    SyncFiles(mockk { every { contentResolver } returns this@SyncFilesTest.contentResolver })

  @Test
  fun `a folder the storage provider can't list is not read`() {
    every { contentResolver.query(any(), any(), any(), any(), any()) } throws
      IllegalArgumentException()

    assertNull(syncFiles.read(folder()))
  }

  @Test
  fun `a sync file the storage provider can't create is skipped`() {
    every { contentResolver.query(any(), any(), any(), any(), any()) } returns null
    every { contentResolver.call(any<String>(), any(), any(), any()) } throws NullPointerException()

    syncFiles.write(folder(), "sync-device.json", SyncFile(emptyMap()))
  }

  @Test
  fun `deleting a sync file keeps the voice folder`() {
    every { contentResolver.query(any(), any(), any(), any(), any()) } returnsMany
      listOf(cursor("voice" to ".voice"), cursor("own" to "sync-device.json"))
    every { contentResolver.call(any<String>(), any(), any(), any()) } returns Bundle()

    syncFiles.delete(folder(), "sync-device.json")

    verify(exactly = 1) {
      contentResolver.call(any<String>(), "android:deleteDocument", any(), any())
    }
  }

  private fun cursor(vararg children: Pair<String, String>) =
    MatrixCursor(arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME)).apply {
      children.forEach { (id, name) -> addRow(arrayOf(id, name)) }
    }
}
