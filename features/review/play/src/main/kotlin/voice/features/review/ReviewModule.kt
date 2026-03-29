package voice.features.review

import android.content.Context
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
interface ReviewModule {

  @Provides
  fun reviewManager(context: Context): ReviewManager {
    return ReviewManagerFactory.create(context)
  }
}
