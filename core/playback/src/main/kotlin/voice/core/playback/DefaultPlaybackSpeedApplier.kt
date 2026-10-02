package voice.core.playback

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import voice.core.data.BookContent
import voice.core.data.repo.BookRepository
import voice.core.data.store.DefaultPlaybackSpeedStore
import java.time.Instant

private const val INITIAL_PLAYBACK_SPEED = 1F

@Inject
class DefaultPlaybackSpeedApplier(
  @DefaultPlaybackSpeedStore
  private val defaultPlaybackSpeedStore: DataStore<Float>,
  private val repo: BookRepository,
) {

  @IgnorableReturnValue
  suspend fun applyTo(content: BookContent): Float {
    if (!content.isNew()) return content.playbackSpeed
    val defaultSpeed = defaultPlaybackSpeedStore.data.first()
    if (defaultSpeed != content.playbackSpeed) {
      repo.updateBook(content.id) { if (it.isNew()) it.copy(playbackSpeed = defaultSpeed) else it }
    }
    return defaultSpeed
  }

  private fun BookContent.isNew(): Boolean {
    return lastPlayedAt == Instant.EPOCH && playbackSpeed == INITIAL_PLAYBACK_SPEED
  }
}
