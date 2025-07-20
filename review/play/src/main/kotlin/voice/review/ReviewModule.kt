package voice.review

import android.content.Context
import androidx.datastore.core.DataStore
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import dev.zacsweers.metro.ContributesTo
import dagger.Module
import dagger.Provides
import dev.zacsweers.metro.Qualifier
import kotlinx.serialization.builtins.serializer
import voice.common.AppScope
import voice.datastore.VoiceDataStoreFactory
import javax.inject.Singleton

@ContributesTo(AppScope::class)
@Module
object ReviewModule {

  @Provides
  @Singleton
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
