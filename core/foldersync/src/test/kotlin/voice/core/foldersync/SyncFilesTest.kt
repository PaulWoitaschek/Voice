package voice.core.foldersync

import android.content.ContentResolver
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
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
}
