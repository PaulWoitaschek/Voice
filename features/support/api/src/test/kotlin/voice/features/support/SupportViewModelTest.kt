package voice.features.support

import android.app.Activity
import androidx.navigation3.runtime.get
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import voice.core.data.supporter.SupporterBadge
import voice.core.data.supporter.SupporterStatus
import voice.core.data.supporter.SupporterStatusStore
import voice.core.featureflag.MemoryFeatureFlag
import voice.features.support.SupportViewState.Action
import voice.features.support.SupportViewState.Content
import voice.navigation.BottomSheetNav
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.navigation.Navigator
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SupportViewModelTest {

  private val scope = TestScope()
  private val backend = FakeSupportBackend()
  private val navigator = mockk<Navigator> {
    every { goBack() } just Runs
  }
  private val supporterStatusStore = MemoryDataStore(SupporterStatus())
  private val supporterNoteFeatureFlag = MemoryFeatureFlag("")
  private val activity = mockk<Activity>()
  private val viewModel = SupportViewModel(
    backend = backend,
    navigator = navigator,
    supporterStatusStore = SupporterStatusStore(supporterStatusStore),
    supporterNoteFeatureFlag = SupporterNoteFeatureFlag(supporterNoteFeatureFlag),
    clock = Clock.fixed(Instant.parse("2026-10-07T10:00:00Z"), ZoneOffset.UTC),
  )

  private val offers = listOf(
    SubscriptionOffer(SupporterTier.GoldenMic, SupportPeriod.Monthly, "€6"),
    SubscriptionOffer(SupporterTier.Tea, SupportPeriod.Monthly, "€1"),
    SubscriptionOffer(SupporterTier.HoneyTea, SupportPeriod.Monthly, "€3"),
    SubscriptionOffer(SupporterTier.Tea, SupportPeriod.Yearly, "€10"),
    SubscriptionOffer(SupporterTier.HoneyTea, SupportPeriod.Yearly, "€30"),
    SubscriptionOffer(SupporterTier.GoldenMic, SupportPeriod.Yearly, "€60"),
  )
  private val tips = listOf(
    TipOffer(SupporterTier.GoldenMic, "€10"),
    TipOffer(SupporterTier.Tea, "€2"),
    TipOffer(SupporterTier.HoneyTea, "€5"),
  )

  @Test
  fun `ko-fi stays ko-fi`() = scope.runTest {
    backend.mutableState.value = SupportBackendState.KoFi

    assertEquals(expected = Content.KoFi, actual = viewState().content)
    assertEquals(expected = 1, actual = backend.refreshCount)
  }

  @Test
  fun `honey tea per month is picked to start with`() = scope.runTest {
    backend.mutableState.value = SupportBackendState.Play(offers, tips, activeSubscription = null)

    val content = assertIs<Content.Play>(viewState().content)

    assertEquals(expected = SupportFrequency.entries, actual = content.frequencies)
    assertEquals(expected = SupportFrequency.Monthly, actual = content.frequency)
    assertEquals(
      expected = listOf(SupporterTier.Tea, SupporterTier.HoneyTea, SupporterTier.GoldenMic),
      actual = content.tiers.map { it.tier },
    )
    assertEquals(expected = SupporterTier.HoneyTea, actual = content.selectedTier)
    assertEquals(expected = Action.Subscribe("€3", SupportPeriod.Monthly), actual = content.action)
  }

  @Test
  fun `picking yearly shows the yearly prices`() = scope.runTest {
    backend.mutableState.value = SupportBackendState.Play(offers, tips, activeSubscription = null)

    viewModel.selectFrequency(SupportFrequency.Yearly)
    viewModel.selectTier(SupporterTier.GoldenMic)
    val content = assertIs<Content.Play>(viewState().content)

    assertEquals(expected = listOf("€10", "€30", "€60"), actual = content.tiers.map { it.formattedPrice })
    assertEquals(expected = Action.Subscribe("€60", SupportPeriod.Yearly), actual = content.action)

    viewModel.confirm(activity)

    assertEquals(expected = listOf(SupporterTier.GoldenMic to SupportPeriod.Yearly), actual = backend.subscribes)
  }

  @Test
  fun `the running subscription is picked and managed`() = scope.runTest {
    backend.mutableState.value = SupportBackendState.Play(
      subscriptions = offers,
      tips = tips,
      activeSubscription = ActiveSubscription(SupporterTier.Tea, SupportPeriod.Yearly),
    )

    val content = assertIs<Content.Play>(viewState().content)

    assertEquals(expected = SupportFrequency.Yearly, actual = content.frequency)
    assertEquals(expected = SupporterTier.Tea, actual = content.selectedTier)
    assertEquals(expected = Action.Manage, actual = content.action)
    assertEquals(expected = listOf(true, false, false), actual = content.tiers.map { it.active })

    viewModel.confirm(activity)

    assertEquals(expected = 1, actual = backend.manageCount)
    assertEquals(expected = emptyList<Pair<SupporterTier, SupportPeriod>>(), actual = backend.subscribes)
  }

  @Test
  fun `another tier switches the running subscription`() = scope.runTest {
    backend.mutableState.value = SupportBackendState.Play(
      subscriptions = offers,
      tips = tips,
      activeSubscription = ActiveSubscription(SupporterTier.Tea, SupportPeriod.Yearly),
    )

    viewModel.selectTier(SupporterTier.HoneyTea)
    val content = assertIs<Content.Play>(viewState().content)

    assertEquals(expected = Action.Switch("€30", SupportPeriod.Yearly), actual = content.action)

    viewModel.confirm(activity)

    assertEquals(expected = listOf(SupporterTier.HoneyTea to SupportPeriod.Yearly), actual = backend.subscribes)
  }

  @Test
  fun `another period switches the running subscription`() = scope.runTest {
    backend.mutableState.value = SupportBackendState.Play(
      subscriptions = offers,
      tips = tips,
      activeSubscription = ActiveSubscription(SupporterTier.Tea, SupportPeriod.Monthly),
    )

    viewModel.selectFrequency(SupportFrequency.Yearly)
    val content = assertIs<Content.Play>(viewState().content)

    assertEquals(expected = SupporterTier.Tea, actual = content.selectedTier)
    assertEquals(expected = Action.Switch("€10", SupportPeriod.Yearly), actual = content.action)

    viewModel.confirm(activity)

    assertEquals(expected = listOf(SupporterTier.Tea to SupportPeriod.Yearly), actual = backend.subscribes)
  }

  @Test
  fun `once shows the tips as tiers`() = scope.runTest {
    backend.mutableState.value = SupportBackendState.Play(offers, tips, activeSubscription = null)

    viewModel.selectFrequency(SupportFrequency.Once)
    val content = assertIs<Content.Play>(viewState().content)

    assertEquals(expected = listOf("€2", "€5", "€10"), actual = content.tiers.map { it.formattedPrice })
    assertEquals(expected = SupporterTier.HoneyTea, actual = content.selectedTier)
    assertEquals(expected = Action.Tip("€5"), actual = content.action)

    viewModel.confirm(activity)

    assertEquals(expected = listOf(SupporterTier.HoneyTea), actual = backend.tips)
    assertEquals(expected = emptyList<Pair<SupporterTier, SupportPeriod>>(), actual = backend.subscribes)
  }

  @Test
  fun `a tip keeps the running subscription as it is`() = scope.runTest {
    backend.mutableState.value = SupportBackendState.Play(
      subscriptions = offers,
      tips = tips,
      activeSubscription = ActiveSubscription(SupporterTier.Tea, SupportPeriod.Monthly),
    )

    viewModel.selectFrequency(SupportFrequency.Once)
    val content = assertIs<Content.Play>(viewState().content)

    assertEquals(expected = SupporterTier.Tea, actual = content.selectedTier)
    assertEquals(expected = Action.Tip("€2"), actual = content.action)
    assertEquals(expected = listOf(false, false, false), actual = content.tiers.map { it.active })

    viewModel.confirm(activity)

    assertEquals(expected = listOf(SupporterTier.Tea), actual = backend.tips)
    assertEquals(expected = emptyList<Pair<SupporterTier, SupportPeriod>>(), actual = backend.subscribes)
    assertEquals(expected = 0, actual = backend.manageCount)
  }

  @Test
  fun `only tips leave once as the only choice`() = scope.runTest {
    backend.mutableState.value = SupportBackendState.Play(emptyList(), tips, activeSubscription = null)

    val content = assertIs<Content.Play>(viewState().content)

    assertEquals(expected = listOf(SupportFrequency.Once), actual = content.frequencies)
    assertEquals(expected = SupportFrequency.Once, actual = content.frequency)
    assertEquals(expected = Action.Tip("€5"), actual = content.action)
  }

  @Test
  fun `no tips leave out once`() = scope.runTest {
    backend.mutableState.value = SupportBackendState.Play(offers, emptyList(), activeSubscription = null)

    viewModel.selectFrequency(SupportFrequency.Once)
    val content = assertIs<Content.Play>(viewState().content)

    assertEquals(expected = listOf(SupportFrequency.Monthly, SupportFrequency.Yearly), actual = content.frequencies)
    assertEquals(expected = SupportFrequency.Monthly, actual = content.frequency)
  }

  @Test
  fun `nothing to buy is unavailable`() = scope.runTest {
    backend.mutableState.value = SupportBackendState.Play(emptyList(), emptyList(), activeSubscription = null)

    assertEquals(expected = Content.Unavailable, actual = viewState().content)
  }

  @Test
  fun `the badge and start come from the supporter status`() = scope.runTest {
    supporterStatusStore.updateData {
      SupporterStatus(
        supporterSince = Instant.parse("2026-03-15T00:00:00Z").toEpochMilli(),
        subscribedMonths = 6,
      )
    }

    val viewState = viewState()

    assertEquals(expected = SupporterBadge.SixMonths, actual = viewState.badge)
    assertEquals(expected = YearMonth.of(2026, 3), actual = viewState.supporterSince)
  }

  @Test
  fun `the note comes from remote config`() = scope.runTest {
    backend.mutableState.value = SupportBackendState.Play(offers, tips, activeSubscription = null)
    supporterNoteFeatureFlag.value = "Paid for the new chapter list."

    val content = assertIs<Content.Play>(viewState().content)

    assertEquals(expected = "Paid for the new chapter list.", actual = content.note)
  }

  @Test
  fun `a blank note is no note`() = scope.runTest {
    backend.mutableState.value = SupportBackendState.Play(offers, tips, activeSubscription = null)

    assertNull(assertIs<Content.Play>(viewState().content).note)
  }

  @Test
  fun `a purchase that went through says thank you`() = scope.runTest {
    backend.mutableState.value = SupportBackendState.Play(offers, tips, activeSubscription = null)

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      runCurrent()
      backend.eventFlow.emit(SupportEvent.Failed)
      runCurrent()
      assertEquals(expected = SupportViewState.Message.Failed, actual = expectMostRecentItem().message)

      backend.eventFlow.emit(SupportEvent.ThankYou)
      runCurrent()
      expectMostRecentItem().let {
        assertTrue(it.thankYou)
        assertNull(it.message)
      }
    }
  }

  @Test
  fun `closing goes back`() {
    viewModel.close()

    verify { navigator.goBack() }
  }

  @Test
  @Suppress("UNCHECKED_CAST")
  fun `support nav entry is bottom sheet`() {
    val provider = SupportProvider.supportNavEntryProvider() as NavEntryProvider<Destination.SupportVoice>
    val navEntry = provider.create(Destination.SupportVoice)

    assertNotNull(navEntry.metadata[BottomSheetNav.BottomSheetKey])
  }

  private suspend fun TestScope.viewState(): SupportViewState {
    lateinit var viewState: SupportViewState
    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      runCurrent()
      viewState = expectMostRecentItem()
      cancelAndIgnoreRemainingEvents()
    }
    return viewState
  }
}

private class FakeSupportBackend : SupportBackend {
  val mutableState = MutableStateFlow<SupportBackendState>(SupportBackendState.Loading)
  val eventFlow = MutableSharedFlow<SupportEvent>()
  var refreshCount = 0
  var manageCount = 0
  val subscribes = mutableListOf<Pair<SupporterTier, SupportPeriod>>()
  val tips = mutableListOf<SupporterTier>()

  override val state: MutableStateFlow<SupportBackendState> = mutableState
  override val events: Flow<SupportEvent> = eventFlow

  override fun refresh() {
    refreshCount++
  }

  override fun openKoFi() {}

  override fun subscribe(
    activity: Activity,
    tier: SupporterTier,
    period: SupportPeriod,
  ) {
    subscribes += tier to period
  }

  override fun tip(
    activity: Activity,
    tier: SupporterTier,
  ) {
    tips += tier
  }

  override fun manageSubscription() {
    manageCount++
  }
}
