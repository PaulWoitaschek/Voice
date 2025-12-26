package voice.features.review

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import voice.core.data.repo.BookRepository
import voice.core.data.store.ReviewDialogShownStore
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.ReviewEnabledFeatureFlagQualifier
import voice.core.playback.playstate.PlayStateManager
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
  @ReviewEnabledFeatureFlagQualifier
  private val reviewEnabledFeatureFlag: FeatureFlag<Boolean>,
) {

  internal suspend fun shouldShow(): Boolean {
    return reviewEnabledFeatureFlag.get() &&
      isNotPlaying() &&
      enoughTimeElapsedSinceInstallation() &&
      reviewDialogNotShown() &&
      listenedForEnoughTime()
  }

  internal suspend fun setShown() {
    reviewDialogShown.updateData { true }
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
