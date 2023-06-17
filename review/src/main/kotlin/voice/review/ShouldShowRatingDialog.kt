package voice.review

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.first
import voice.data.repo.BookRepository
import voice.playback.playstate.PlayStateManager
import java.time.Clock
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

class ShouldShowRatingDialog
@Inject constructor(
  private val installationTimeProvider: InstallationTimeProvider,
  private val clock: Clock,
  @RatingDialogShown
  private val ratingDialogShown: DataStore<Boolean>,
  private val bookRepository: BookRepository,
  private val playStateManager: PlayStateManager,
) {

  suspend fun shouldShow(): Boolean {
    return isNotPlaying() &&
      enoughTimeElapsedSinceInstallation() &&
      ratingNotShownYet() &&
      listenedForEnoughTime()
  }

  private fun enoughTimeElapsedSinceInstallation(): Boolean {
    val timeSinceInstallation = ChronoUnit.MILLIS.between(
      installationTimeProvider.installationTime(),
      clock.instant(),
    ).milliseconds
    return timeSinceInstallation >= 2.days
  }

  private suspend fun ratingNotShownYet() = !ratingDialogShown.data.first()

  private fun isNotPlaying() = playStateManager.playState != PlayStateManager.PlayState.Playing

  private suspend fun listenedForEnoughTime() = bookRepository.all().sumOf { it.position }.milliseconds >= 1.days

  suspend fun setShown() {
    ratingDialogShown.updateData { true }
  }
}
