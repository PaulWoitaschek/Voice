package voice.features.bookOverview.stats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import voice.core.analytics.api.Analytics
import voice.core.data.BookId
import voice.core.data.repo.BookContentRepo
import voice.core.data.repo.BookRepository
import voice.core.data.repo.ListeningStatsRepo
import voice.core.data.supporter.SupporterBadge
import voice.core.data.supporter.SupporterStatusStore
import voice.navigation.Destination
import voice.navigation.Navigator

@Inject
class ListeningStatsViewModel(
  private val statsRepo: ListeningStatsRepo,
  private val bookRepo: BookRepository,
  private val contentRepo: BookContentRepo,
  private val supporterStatusStore: SupporterStatusStore,
  private val weekListening: WeekListening,
  private val zone: () -> ZoneId,
  private val navigator: Navigator,
  private val analytics: Analytics,
) {

  private var kind by mutableStateOf(PeriodKind.Week)
  private var back by mutableIntStateOf(0)

  init {
    reportPeriod()
  }

  @Composable
  internal fun viewState(): ListeningStatsViewState? {
    val sessions = remember { statsRepo.sessions() }.collectAsState(initial = null).value
    val contents = remember { contentRepo.flow() }.collectAsState(initial = null).value
    val books = remember { bookRepo.flow() }.collectAsState(initial = null).value
    val supporter = remember { supporterStatusStore.data }.collectAsState(initial = null).value
    val today = remember { weekListening.days() }.collectAsState(initial = null).value
    if (
      sessions == null || contents == null || books == null || supporter == null || today == null
    ) {
      return null
    }

    val zone = zone()
    val firstDay = remember(sessions) { sessions.minOfOrNull { it.day() } }
    val finishes = remember(sessions, contents, zone) { finishes(sessions, contents, zone) }
    val period = kind.period(today, back, weekListening.weekStart())
    val stats = remember(period, sessions, finishes) { period.stats(sessions, finishes) }
    val booksById = remember(books) { books.associateBy { it.id } }
    val contentsById = remember(contents) { contents.associateBy { it.id } }
    return ListeningStatsViewState(
      period = period,
      current = back == 0,
      canGoBack = kind != PeriodKind.AllTime && firstDay != null && firstDay < period.start,
      canGoForward = back > 0,
      listened = stats.listened,
      days = stats.days,
      bars = stats.bars,
      saved = stats.saved.takeIf { it >= 1.minutes },
      countsSince = firstDay.takeIf { kind == PeriodKind.AllTime },
      finished =
        stats.finished.mapNotNull { finish ->
          val book = booksById[finish.bookId]
          if (book != null) {
            FinishedBookViewState(book.id, book.name, book.author, book.coverUrl, finish.day, true)
          } else {
            val content = contentsById[finish.bookId] ?: return@mapNotNull null
            FinishedBookViewState(
              content.id,
              content.metadata.name,
              content.metadata.author,
              cover = null,
              day = finish.day,
              inLibrary = false,
            )
          }
        },
      supporterBadge = supporter.badge,
    )
  }

  internal fun onKindClick(kind: PeriodKind) {
    if (kind == this.kind) return
    this.kind = kind
    back = 0
    reportPeriod()
  }

  internal fun onEarlierClick() {
    back++
  }

  internal fun onLaterClick() {
    back--
  }

  internal fun onBookClick(id: BookId) {
    navigator.goTo(Destination.Playback(id))
  }

  internal fun onSupportClick() {
    analytics.event("support_open", mapOf("from" to "listening_stats"))
    navigator.goTo(Destination.SupportVoice)
  }

  internal fun onCloseClick() {
    navigator.goBack()
  }

  private fun reportPeriod() {
    analytics.event("listening_stats_period", mapOf("period" to kind.analyticsName))
  }

  @ContributesTo(AppScope::class)
  interface Provider {
    val listeningStatsViewModel: ListeningStatsViewModel
  }
}

@Immutable
internal data class ListeningStatsViewState(
  val period: Period,
  val current: Boolean,
  val canGoBack: Boolean,
  val canGoForward: Boolean,
  val listened: Duration,
  val days: Int,
  val bars: List<Duration>,
  val saved: Duration?,
  val countsSince: LocalDate?,
  val finished: List<FinishedBookViewState>,
  val supporterBadge: SupporterBadge?,
) {
  val empty: Boolean
    get() = listened.inWholeMinutes == 0L && finished.isEmpty()
}

@Immutable
internal data class FinishedBookViewState(
  val id: BookId,
  val name: String,
  val author: String?,
  val cover: String?,
  val day: LocalDate,
  val inLibrary: Boolean,
)
