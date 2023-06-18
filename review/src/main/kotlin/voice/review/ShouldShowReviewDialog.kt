package voice.review

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.first
import voice.data.repo.BookRepository
import voice.playback.playstate.PlayStateManager
import java.time.Clock
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class ShouldShowReviewDialog
@Inject constructor(
  private val installationTimeProvider: InstallationTimeProvider,
  private val clock: Clock,
  @ReviewDialogShown
  private val reviewDialogShown: DataStore<Boolean>,
  private val bookRepository: BookRepository,
  private val playStateManager: PlayStateManager,
  private val reviewTranslated: ReviewTranslated,
) {

  internal suspend fun shouldShow(): Boolean {
    return reviewTranslated.translated() &&
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
