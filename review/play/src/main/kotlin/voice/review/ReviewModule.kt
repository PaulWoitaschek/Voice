package voice.review

import android.content.Context
import androidx.datastore.core.DataStore
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.builtins.serializer
import voice.common.AppScope
import voice.datastore.VoiceDataStoreFactory

@ContributesTo(AppScope::class)
@BindingContainer
object ReviewModule {

  @Provides
  @SingleIn(AppScope::class)
  @ReviewDialogShown
  fun reviewDialogShown(factory: VoiceDataStoreFactory): DataStore<Boolean> {
    return factory.create(Boolean.serializer(), false, "reviewDialogShown")
  }

  @Provides
  fun reviewManager(context: Context): ReviewManager {
    return ReviewManagerFactory.create(context)
  }
}

@Qualifier
annotation class ReviewDialogShown
