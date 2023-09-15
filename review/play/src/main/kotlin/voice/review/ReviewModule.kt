package voice.review

import android.content.Context
import androidx.datastore.core.DataStore
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.serialization.builtins.serializer
import voice.common.AppScope
import voice.datastore.VoiceDataStoreFactory

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

  @Provides
  fun remoteConfig(): FirebaseRemoteConfig {
    return FirebaseRemoteConfig.getInstance()
  }
}

@Qualifier
annotation class ReviewDialogShown
