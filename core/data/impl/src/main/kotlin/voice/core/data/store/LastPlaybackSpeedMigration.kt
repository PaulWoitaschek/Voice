package voice.core.data.store

import androidx.datastore.core.DataMigration
import voice.core.data.repo.BookContentRepo
import java.io.File
import java.time.Instant

/**
 * Starts from the speed of the book played last, so listeners who update don't get their first new book at 1×.
 */
internal class LastPlaybackSpeedMigration(
  private val file: File,
  private val contentRepo: BookContentRepo,
) : DataMigration<Float> {

  override suspend fun shouldMigrate(currentData: Float): Boolean = !file.exists()

  override suspend fun migrate(currentData: Float): Float {
    return contentRepo.all()
      .filter { it.lastPlayedAt > Instant.EPOCH }
      .maxByOrNull { it.lastPlayedAt }
      ?.playbackSpeed
      ?: currentData
  }

  override suspend fun cleanUp() {}
}
