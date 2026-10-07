package voice.core.data.repo

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import voice.core.initializer.AppInitializer

@ContributesIntoSet(AppScope::class)
public class RemoveExpiredListeningHistory(
  private val repo: ListeningHistoryRepo,
  private val scope: CoroutineScope,
) : AppInitializer {

  override fun onAppStart(application: Application) {
    scope.launch {
      repo.removeExpired()
    }
  }
}
