package voice.review

import androidx.datastore.core.DataStore
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import kotlinx.serialization.builtins.serializer
import voice.common.AppScope
import voice.datastore.VoiceDataStoreFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@ContributesTo(AppScope::class)
@Module
object ReviewModule {

  @Provides
  @Singleton
  @RatingDialogShown
  fun ratingDialogShown(factory: VoiceDataStoreFactory): DataStore<Boolean> {
    return factory.create(Boolean.serializer(), false, "ratingDialogShown")
  }
}

@Qualifier
annotation class RatingDialogShown
