package voice.features.bookOverview.stats

import android.content.res.Resources
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaInstant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import voice.core.data.repo.ListeningStatsRepo

@Inject
class WeekListening(
  private val repo: ListeningStatsRepo,
  private val clock: Clock,
  private val zone: () -> ZoneId,
  private val firstDayOfWeek: () -> DayOfWeek,
) {

  @OptIn(ExperimentalCoroutinesApi::class)
  fun listened(): Flow<Duration?> =
    days()
      .flatMapLatest { today ->
        val week = PeriodKind.Week.period(today, back = 0, weekStart())
        repo.listened(week.start, week.end)
      }
      // the playback saves often, the library shows whole minutes
      .map { it?.inWholeMinutes?.minutes }
      .distinctUntilChanged()

  // a tick, as a delay until midnight would come late after the phone slept
  internal fun days(): Flow<LocalDate> = flow {
    while (true) {
      emit(today())
      delay(1.minutes)
    }
  }
    .distinctUntilChanged()

  internal fun today(): LocalDate = clock.now().toJavaInstant().atZone(zone()).toLocalDate()

  internal fun weekStart(): DayOfWeek = firstDayOfWeek()
}

@BindingContainer
@ContributesTo(AppScope::class)
object WeekStartProvider {

  // the region of the phone, as the language of the app can come without one
  @Provides
  fun weekStart(): DayOfWeek =
    WeekFields.of(Resources.getSystem().configuration.locales[0]).firstDayOfWeek
}
