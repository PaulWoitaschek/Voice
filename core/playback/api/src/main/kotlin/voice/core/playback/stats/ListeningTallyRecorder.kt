package voice.core.playback.stats

import dev.zacsweers.metro.Inject
import java.time.ZoneId
import kotlin.time.Clock
import kotlin.time.toJavaInstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import voice.core.data.BookId
import voice.core.data.ListeningTally
import voice.core.data.repo.ListeningStatsRepo

@Inject
class ListeningTallyRecorder(
  private val repo: ListeningStatsRepo,
  private val clock: Clock,
  private val zone: () -> ZoneId,
  private val scope: CoroutineScope,
) {

  fun add(
    bookId: BookId,
    name: String,
    number: Long = 1,
  ) {
    val day = clock.now().toJavaInstant().atZone(zone()).toLocalDate()
    scope.launch {
      repo.add(ListeningTally(day, bookId, name, number))
    }
  }
}
