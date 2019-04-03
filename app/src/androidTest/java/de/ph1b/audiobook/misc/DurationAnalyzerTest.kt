package de.ph1b.audiobook.misc

import android.content.Context
import android.net.Uri
import androidx.annotation.RawRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import de.ph1b.audiobook.injection.PlaybackModule
import de.ph1b.audiobook.playback.OnlyAudioRenderersFactory
import de.ph1b.audiobook.playback.utils.DataSourceConverter
import de.ph1b.audiobook.test.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DurationAnalyzerTest {

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  private val durationAnalyzer: DurationAnalyzer

  init {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dataSourceConverter = DataSourceConverter(context)
    durationAnalyzer = runBlocking(Dispatchers.Main) {
      DurationAnalyzer(dataSourceConverter, PlaybackModule.exoPlayer(context, OnlyAudioRenderersFactory(context)))
    }
  }

  @Test(timeout = 1000)
  fun defectFile_noDuration() {
    val duration = durationOfResource(R.raw.defect)
    assertThat(duration).isNull()
  }

  @Test
  fun intactFile_correctDuration() {
    val duration = durationOfResource(R.raw.intact)
    assertThat(duration).isEqualTo(119040)
  }

  @Test(timeout = 1000)
  fun subsequentCalls() {
    assertThat(durationOfResource(R.raw.intact)).isEqualTo(119040)
    assertThat(durationOfResource(R.raw.defect)).isNull()
    assertThat(durationOfResource(R.raw.intact2)).isEqualTo(119040)
  }

  private fun durationOfResource(@RawRes resource: Int): Int? {
    val file = resourceToTemporaryFile(resource)
    return runBlocking {
      durationAnalyzer.duration(Uri.fromFile(file))
    }
  }

  private fun resourceToTemporaryFile(@RawRes resource: Int): File {
    val file = temporaryFolder.newFile()
    val context = InstrumentationRegistry.getInstrumentation().context
    context.resources.openRawResource(resource).use { input ->
      file.outputStream().use { output ->
        input.copyTo(output)
      }
    }

    require(file.length() > 0)
    return file
  }
}
