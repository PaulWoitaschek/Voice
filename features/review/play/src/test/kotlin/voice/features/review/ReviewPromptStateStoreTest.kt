package voice.features.review

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.runner.RunWith
import voice.core.datastore.VoiceDataStoreFactory
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class ReviewPromptStateStoreTest {

  private val application: Application = ApplicationProvider.getApplicationContext()

  private fun store() = ReviewPromptStateStore(VoiceDataStoreFactory(Json.Default, application), application)

  @Test
  fun `a shown review dialog means the review prompt is done`() = runTest {
    val oldDataStoreFile = legacyReviewDialogShownFile("true")

    assertEquals(expected = ReviewPromptState(done = true), actual = store().data.first())
    assertEquals(expected = false, actual = oldDataStoreFile.exists())
  }

  @Test
  fun `a review dialog not shown yet keeps the review prompt open`() = runTest {
    val oldDataStoreFile = legacyReviewDialogShownFile("false")

    assertEquals(expected = ReviewPromptState(), actual = store().data.first())
    assertEquals(expected = false, actual = oldDataStoreFile.exists())
  }

  @Test
  fun `the review prompt starts fresh without a legacy file`() = runTest {
    assertEquals(expected = ReviewPromptState(), actual = store().data.first())
  }

  private fun legacyReviewDialogShownFile(content: String): File {
    val file = File(application.filesDir, "datastore/reviewDialogShown")
    file.parentFile!!.mkdirs()
    file.writeText(content)
    return file
  }
}
