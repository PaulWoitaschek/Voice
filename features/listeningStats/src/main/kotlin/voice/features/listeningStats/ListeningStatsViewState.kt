package voice.features.listeningStats

import androidx.compose.runtime.Immutable

// The stats state only exposes rendering fields so the UI never understands Room query bounds.
@Immutable
internal data class ListeningStatsViewState(
  val title: String,
  val selectedPeriod: ListeningStatsPeriod,
  val durationMs: Long,
  val empty: Boolean,
) {

  // Preview factories live on the state object so Compose Preview can reuse a stable sample.
  internal companion object {
    // Previews and first composition need stable defaults so the UI never flashes a broken state before data arrives.
    fun preview(): ListeningStatsViewState {
      return ListeningStatsViewState(
        title = "Sample Book",
        selectedPeriod = ListeningStatsPeriod.Day,
        durationMs = 95 * 60 * 1000L,
        empty = false,
      )
    }
  }
}
