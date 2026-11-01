package voice.core.analytics.firebase

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics as GmsFirebaseAnalytics
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@BindingContainer
@ContributesTo(AppScope::class)
object AnalyticsModule {

  @Provides
  @SingleIn(AppScope::class)
  fun firebaseAnalytics(context: Context): GmsFirebaseAnalytics {
    return GmsFirebaseAnalytics.getInstance(context)
  }
}
