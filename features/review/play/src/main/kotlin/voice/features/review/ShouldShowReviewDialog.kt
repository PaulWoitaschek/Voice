package voice.features.review

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import voice.core.data.repo.BookRepository
import voice.core.data.store.ReviewDialogShownStore
import voice.core.playback.playstate.PlayStateManager
import voice.core.remoteconfig.core.RemoteConfig
import java.time.Clock
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@Inject
class ShouldShowReviewDialog(
  private val installationTimeProvider: InstallationTimeProvider,
  private val clock: Clock,
  @ReviewDialogShownStore
  private val reviewDialogShown: DataStore<Boolean>,
  private val bookRepository: BookRepository,
  private val playStateManager: PlayStateManager,
  private val remoteConfig: RemoteConfig,
) {

  internal suspend fun shouldShow(): Boolean {
    return enabledInRemoteConfig() &&
      isNotPlaying() &&
      enoughTimeElapsedSinceInstallation() &&
      reviewDialogNotShown() &&
      listenedForEnoughTime()
  }

  internal suspend fun setShown() {
    reviewDialogShown.updateData { true }
  }

  private fun enabledInRemoteConfig(): Boolean {
    return remoteConfig.boolean("review_enabled")
  }

  private fun enoughTimeElapsedSinceInstallation(): Boolean {
    val timeSinceInstallation = ChronoUnit.MINUTES.between(
      installationTimeProvider.installationTime(),
      clock.instant(),
    ).minutes
    return timeSinceInstallation >= 2.days
  }

  private suspend fun reviewDialogNotShown() = !reviewDialogShown.data.first()

  private fun isNotPlaying() = playStateManager.playState != PlayStateManager.PlayState.Playing

  private suspend fun listenedForEnoughTime(): Boolean {
    val totalListeningTime = bookRepository.all().sumOf {
      it.position.milliseconds.inWholeMinutes
    }.minutes
    return totalListeningTime >= 5.hours
  }
}
