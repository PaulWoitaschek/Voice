package voice.core.analytics.firebase

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.analytics.FirebaseAnalytics as GmsFirebaseAnalytics
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlin.test.Test
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import voice.core.analytics.api.AnalyticsConsentStore

@RunWith(AndroidJUnit4::class)
class EnableAnalyticsOnConsentTest {

  private val scope = TestScope()
  private val gmsAnalytics = mockk<GmsFirebaseAnalytics>(relaxUnitFun = true)
  private val analytics = FirebaseAnalytics(gmsAnalytics)
  private val consent = MemoryDataStore(false)

  init {
    EnableAnalyticsOnConsent(AnalyticsConsentStore(consent), analytics)
      .onAppStart(ApplicationProvider.getApplicationContext<Application>())
    shadowOf(Looper.getMainLooper()).idle()
  }

  @Test
  fun `analytics off, any of the above, nothing`() {
    verify(exactly = 1) { gmsAnalytics.setAnalyticsCollectionEnabled(false) }
    verify(exactly = 0) { gmsAnalytics.setAnalyticsCollectionEnabled(true) }
  }

  @Test
  fun `user properties set while analytics is off reach firebase once analytics turns on`() =
    scope.runTest {
      analytics.setUserProperty("theme", "dark")

      consent.updateData { true }
      shadowOf(Looper.getMainLooper()).idle()

      verifyOrder {
        gmsAnalytics.setUserProperty("theme", "dark")
        gmsAnalytics.setAnalyticsCollectionEnabled(true)
        gmsAnalytics.setUserProperty("theme", "dark")
      }
    }
}
