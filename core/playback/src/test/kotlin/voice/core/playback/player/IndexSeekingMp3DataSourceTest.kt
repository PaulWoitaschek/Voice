package voice.core.playback.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class IndexSeekingMp3DataSourceTest {

  @Test
  fun `reports unknown length for mp3 so index seeking is used`() {
    val dataSpec = DataSpec(Uri.parse("content://books/chapter.MP3"))
    val upstream = upstreamDataSource(dataSpec, length = 1234L)

    val length = IndexSeekingMp3DataSource(upstream).open(dataSpec)

    assertEquals(C.LENGTH_UNSET.toLong(), length)
  }

  @Test
  fun `preserves resolved length for other formats`() {
    val dataSpec = DataSpec(Uri.parse("content://books/chapter.m4b"))
    val upstream = upstreamDataSource(dataSpec, length = 1234L)

    val length = IndexSeekingMp3DataSource(upstream).open(dataSpec)

    assertEquals(1234L, length)
  }

  private fun upstreamDataSource(
    dataSpec: DataSpec,
    length: Long,
  ): DataSource {
    return mockk {
      every { open(dataSpec) } returns length
    }
  }
}
