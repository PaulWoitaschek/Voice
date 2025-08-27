package voice.core.scanner

import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.file.shouldBeAFile
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import voice.core.logging.core.LogWriter
import voice.core.logging.core.Logger
import voice.core.scanner.matroska.MatroskaCoverExtractor
import java.io.File

@RunWith(AndroidJUnit4::class)
class MatroskaCoverExtractorTest {

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun mka() {
    val coverExtractor = MatroskaCoverExtractor(ApplicationProvider.getApplicationContext())
    val testFile = File(javaClass.classLoader!!.getResource("mka_with_cover.mka")!!.file)
    val coverFile = temporaryFolder.newFile("cover.jpg")
    val extracted = coverExtractor.extract(
      input = testFile.toUri(),
      outputFile = coverFile,
    )
    extracted.shouldBeTrue()
    coverFile.shouldBeAFile()
  }

  companion object {

    @BeforeClass
    @JvmStatic
    fun setup() {
      Logger.install(
        object : LogWriter {
          override fun log(
            severity: Logger.Severity,
            message: String,
            throwable: Throwable?,
          ) {
            println(
              buildString {
                append("${severity.name}: ")
                append(message)
                if (throwable != null) {
                  append(", $throwable")
                }
              },
            )
          }
        },
      )
    }
  }
}
