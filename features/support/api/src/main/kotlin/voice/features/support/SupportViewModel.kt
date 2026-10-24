package voice.features.support

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.map
import voice.core.data.supporter.SupporterStatus
import voice.core.data.supporter.SupporterStatusStore
import voice.core.featureflag.FeatureFlag
import voice.features.support.SupportViewState.Action
import voice.features.support.SupportViewState.Content
import voice.navigation.Navigator
import java.time.Clock
import java.time.Instant
import java.time.YearMonth

@Inject
class SupportViewModel(
  private val backend: SupportBackend,
  private val navigator: Navigator,
  private val supporterStatusStore: SupporterStatusStore,
  private val supporterNoteFeatureFlag: SupporterNoteFeatureFlag,
  private val clock: Clock,
) : SupportListener {

  private var chosenFrequency by mutableStateOf<SupportFrequency?>(null)
  private var chosenTier by mutableStateOf<SupporterTier?>(null)
  private var thankYou by mutableStateOf(false)
  private var message by mutableStateOf<SupportViewState.Message?>(null)

  @Composable
  fun viewState(): SupportViewState {
    LaunchedEffect(Unit) {
      backend.refresh()
    }
    LaunchedEffect(Unit) {
      backend.events.collect { event ->
        when (event) {
          SupportEvent.ThankYou -> {
            thankYou = true
            message = null
          }
          SupportEvent.Pending -> message = SupportViewState.Message.Pending
          SupportEvent.Failed -> message = SupportViewState.Message.Failed
        }
      }
    }
    val backendState by backend.state.collectAsState()
    val status by remember { supporterStatusStore.data }.collectAsState(initial = SupporterStatus())
    val note by remember { supporterNoteFeatureFlag.flow.map { it.value } }
      .collectAsState(initial = supporterNoteFeatureFlag.get())
    return SupportViewState(
      content = when (val state = backendState) {
        SupportBackendState.KoFi -> Content.KoFi
        SupportBackendState.Loading -> Content.Loading
        SupportBackendState.Unavailable -> Content.Unavailable
        is SupportBackendState.Play -> playContent(state, note.takeIf { it.isNotBlank() }) ?: Content.Unavailable
      },
      badge = status.badge,
      supporterSince = status.supporterSince?.let {
        YearMonth.from(Instant.ofEpochMilli(it).atZone(clock.zone))
      },
      thankYou = thankYou,
      message = message,
    )
  }

  private fun playContent(
    state: SupportBackendState.Play,
    note: String?,
  ): Content.Play? {
    val active = state.activeSubscription
    val tiersByFrequency = SupportFrequency.entries
      .associateWith { state.tiers(it) }
      .filterValues { it.isNotEmpty() }
    val frequencies = tiersByFrequency.keys.toList()
    val frequency = chosenFrequency?.takeIf { it in tiersByFrequency }
      ?: frequencies.firstOrNull { active != null && it.period == active.period }
      ?: frequencies.firstOrNull()
      ?: return null
    val tiers = tiersByFrequency.getValue(frequency)
    val wanted = chosenTier ?: active?.tier ?: SupporterTier.HoneyTea
    val selected = tiers.firstOrNull { it.tier == wanted } ?: tiers.first()
    val period = frequency.period
    val action = when {
      period == null -> Action.Tip(selected.formattedPrice)
      selected.active -> Action.Manage
      active == null -> Action.Subscribe(selected.formattedPrice, period)
      else -> Action.Switch(selected.formattedPrice, period)
    }
    return Content.Play(
      frequencies = frequencies,
      frequency = frequency,
      tiers = tiers,
      selectedTier = selected.tier,
      action = action,
      note = note,
    )
  }

  private fun SupportBackendState.Play.tiers(frequency: SupportFrequency): List<SupportViewState.Tier> {
    val period = frequency.period
    val prices = if (period == null) {
      tips.map { it.tier to it.formattedPrice }
    } else {
      subscriptions.filter { it.period == period }.map { it.tier to it.formattedPrice }
    }
    return prices
      .sortedBy { (tier, _) -> tier.ordinal }
      .map { (tier, formattedPrice) ->
        SupportViewState.Tier(
          tier = tier,
          formattedPrice = formattedPrice,
          active = period != null && activeSubscription == ActiveSubscription(tier, period),
        )
      }
  }

  override fun close() {
    navigator.goBack()
  }

  override fun openKoFi() {
    backend.openKoFi()
  }

  override fun selectFrequency(frequency: SupportFrequency) {
    chosenFrequency = frequency
  }

  override fun selectTier(tier: SupporterTier) {
    chosenTier = tier
  }

  override fun confirm(activity: Activity) {
    val state = backend.state.value as? SupportBackendState.Play ?: return
    val content = playContent(state, note = null) ?: return
    message = null
    val tier = content.selectedTier
    when (val action = content.action) {
      Action.Manage -> backend.manageSubscription()
      is Action.Subscribe -> backend.subscribe(activity, tier, action.period)
      is Action.Switch -> backend.subscribe(activity, tier, action.period)
      is Action.Tip -> backend.tip(activity, tier)
    }
  }

  override fun manageSubscription() {
    backend.manageSubscription()
  }

  override fun retry() {
    message = null
    backend.refresh()
  }
}
