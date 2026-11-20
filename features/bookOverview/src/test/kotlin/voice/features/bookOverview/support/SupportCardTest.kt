package voice.features.bookOverview.support

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import voice.core.analytics.api.Analytics
import voice.core.data.BookId
import voice.core.data.repo.FinishedBook
import voice.core.data.repo.ListeningStatsRepo
import voice.core.data.repo.ListeningSummary
import voice.core.data.supporter.SupporterStatus
import voice.core.data.supporter.SupporterStatusStore
import voice.core.featureflag.MemoryFeatureFlag
import voice.features.bookOverview.MemoryDataStore

class SupportCardTest {

  private val now = Instant.parse("2026-11-20T12:00:00Z")
  private val analytics = mockk<Analytics>(relaxUnitFun = true)
  private val answered = MemoryDataStore(false)

  @Test
  fun `setting on, a book finished yesterday, 5 hours on 3 days, the card shows`() = runTest {
    assertEquals(true, supportCard().show().first())
  }

  @Test
  fun `no answer, a book finished 5 days ago and one yesterday, the card shows`() = runTest {
    assertEquals(true, supportCard(finishedAgo = listOf(1.days, 5.days)).show().first())
  }

  @Test
  fun `setting off, a book finished yesterday, no card`() = runTest {
    assertEquals(false, supportCard(enabled = false).show().first())
  }

  @Test
  fun `setting on, no finished book, no card`() = runTest {
    assertEquals(false, supportCard(finishedAgo = emptyList()).show().first())
  }

  @Test
  fun `setting on, a book finished 5 days ago, no card`() = runTest {
    assertEquals(false, supportCard(finishedAgo = listOf(5.days)).show().first())
  }

  @Test
  fun `setting on, a book finished yesterday, 2 hours listened, no card`() = runTest {
    assertEquals(false, supportCard(listened = 2.hours).show().first())
  }

  @Test
  fun `setting on, a book finished yesterday, listening on 2 days, no card`() = runTest {
    assertEquals(false, supportCard(listeningDays = 2).show().first())
  }

  @Test
  fun `a supporter, a book finished yesterday, no card`() = runTest {
    assertEquals(false, supportCard(supporter = SupporterStatus(tipped = true)).show().first())
  }

  @Test
  fun `the card, tap Support development, the card never comes back`() = runTest {
    val supportCard = supportCard()

    supportCard.open()

    assertEquals(false, supportCard.show().first())
    verify(exactly = 1) { analytics.event("support_open", mapOf("from" to "library_card")) }
  }

  @Test
  fun `the card, tap No thanks, the card never comes back`() = runTest {
    val supportCard = supportCard()

    supportCard.dismiss()

    assertEquals(false, supportCard.show().first())
    verify(exactly = 1) { analytics.event("support_card", mapOf("action" to "dismiss")) }
  }

  private fun supportCard(
    enabled: Boolean = true,
    finishedAgo: List<Duration> = listOf(1.days),
    listened: Duration = 5.hours,
    listeningDays: Int = 3,
    supporter: SupporterStatus = SupporterStatus(),
  ): SupportCard {
    return SupportCard(
      enabled = SupportCardFeatureFlag(MemoryFeatureFlag(enabled)),
      answeredStore = SupportCardAnsweredStore(answered),
      supporterStatusStore = SupporterStatusStore(MemoryDataStore(supporter)),
      statsRepo =
        mockk<ListeningStatsRepo> {
          coEvery { summary(any(), any()) } returns
            ListeningSummary(
              listened = listened,
              listeningDays = listeningDays,
              finishedBooks = finishedAgo.map { FinishedBook(BookId("a"), now - it) },
            )
        },
      clock =
        object : Clock {
          override fun now() = now
        },
      analytics = analytics,
    )
  }
}
