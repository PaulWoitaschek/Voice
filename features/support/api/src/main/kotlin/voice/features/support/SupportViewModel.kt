package voice.features.support

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.map
import voice.core.data.store.SupporterStatusStore
import voice.core.data.supporter.SupporterStatus
import voice.core.featureflag.FeatureFlag
import voice.core.featureflag.SupporterNoteFeatureFlagQualifier
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
  @SupporterStatusStore
  private val supporterStatusStore: DataStore<SupporterStatus>,
  @SupporterNoteFeatureFlagQualifier
  private val supporterNoteFeatureFlag: FeatureFlag<String>,
  private val clock: Clock,
) : SupportListener {

  private var period by mutableStateOf<SupportPeriod?>(null)
  private var tier by mutableStateOf<SupporterTier?>(null)
  private var showTips by mutableStateOf(false)
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
        is SupportBackendState.Play -> playContent(state, note.takeIf { it.isNotBlank() })
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
  ): Content {
    val subscription = selection(state)?.let { selection ->
      SupportViewState.Subscription(
        period = selection.period,
        tiers = selection.tiers.map { offer ->
          SupportViewState.Tier(
            tier = offer.tier,
            formattedPrice = offer.formattedPrice,
            active = state.activeSubscription == ActiveSubscription(offer.tier, offer.period),
          )
        },
        selectedTier = selection.offer.tier,
        action = selection.action,
        subscribed = state.activeSubscription != null,
      )
    }
    if (subscription == null && state.tips.isEmpty()) {
      return Content.Unavailable
    }
    return Content.Play(
      subscription = subscription,
      tips = state.tips,
      showTips = showTips || subscription == null,
      note = note,
    )
  }

  private fun selection(state: SupportBackendState.Play): Selection? {
    val active = state.activeSubscription
    val period = period ?: active?.period ?: SupportPeriod.Monthly
    val tiers = state.subscriptions
      .filter { it.period == period }
      .sortedBy { it.tier.ordinal }
    val wanted = tier ?: active?.tier ?: SupporterTier.HoneyTea
    val offer = tiers.firstOrNull { it.tier == wanted } ?: tiers.firstOrNull() ?: return null
    val action = when (active) {
      ActiveSubscription(offer.tier, period) -> Action.Manage
      null -> Action.Subscribe(offer.formattedPrice, period)
      else -> Action.Switch(offer.formattedPrice, period)
    }
    return Selection(period, tiers, offer, action)
  }

  override fun close() {
    navigator.goBack()
  }

  override fun openKoFi() {
    backend.openKoFi()
  }

  override fun selectPeriod(period: SupportPeriod) {
    this.period = period
  }

  override fun selectTier(tier: SupporterTier) {
    this.tier = tier
  }

  override fun confirm(activity: Activity) {
    val state = backend.state.value as? SupportBackendState.Play ?: return
    val selection = selection(state) ?: return
    message = null
    when (selection.action) {
      Action.Manage -> backend.manageSubscription()
      is Action.Subscribe, is Action.Switch -> backend.subscribe(activity, selection.offer.tier, selection.period)
    }
  }

  override fun toggleTips() {
    showTips = !showTips
  }

  override fun tip(
    activity: Activity,
    tip: TipOffer,
  ) {
    message = null
    backend.tip(activity, tip)
  }

  override fun manageSubscription() {
    backend.manageSubscription()
  }

  override fun retry() {
    message = null
    backend.refresh()
  }

  private data class Selection(
    val period: SupportPeriod,
    val tiers: List<SubscriptionOffer>,
    val offer: SubscriptionOffer,
    val action: Action,
  )
}
