package voice.features.review

import android.app.Application
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import voice.core.datastore.VoiceDataStoreFactory
import java.io.File

@SingleIn(AppScope::class)
class ReviewPromptStateStore(store: DataStore<ReviewPromptState>) : DataStore<ReviewPromptState> by store {

  @Inject
  constructor(
    factory: VoiceDataStoreFactory,
    application: Application,
  ) : this(
    factory.create(
      serializer = ReviewPromptState.serializer(),
      defaultValue = ReviewPromptState(),
      fileName = "reviewPromptState",
      migrations = listOf(ReviewDialogShownMigration(application)),
    ),
  )
}

/**
 * The prompt used to ask once and remember that it did. Whoever saw it isn't asked again.
 */
private class ReviewDialogShownMigration(application: Application) : DataMigration<ReviewPromptState> {

  private val oldDataStoreFile = File(application.applicationContext.filesDir, "datastore/reviewDialogShown")

  override suspend fun shouldMigrate(currentData: ReviewPromptState): Boolean = oldDataStoreFile.exists()

  override suspend fun migrate(currentData: ReviewPromptState): ReviewPromptState {
    val shown = oldDataStoreFile.readText().trim().toBooleanStrictOrNull() ?: false
    return if (shown) currentData.copy(done = true) else currentData
  }

  override suspend fun cleanUp() {
    oldDataStoreFile.delete()
  }
}
