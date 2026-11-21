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
  fun `setting Letter, a book finished yesterday, 5 hours on 3 days, the letter is offered`() =
    runTest {
      assertEquals(
        SupportCardOffer.Now(SupportCardLook.Letter, BookId("book0"), 5.hours),
        supportCard(look = SupportCardLook.Letter).offer().first(),
      )
    }

  @Test
  fun `no answer, a book finished yesterday and one 5 days ago, the newest book is offered`() =
    runTest {
      assertEquals(
        SupportCardOffer.Now(SupportCardLook.Letter, BookId("book0"), 5.hours),
        supportCard(finishedAgo = listOf(1.days, 5.days)).offer().first(),
      )
    }

  @Test
  fun `setting Letter, no finished book, no card`() = runTest {
    assertEquals(SupportCardOffer.NotNow, supportCard(finishedAgo = emptyList()).offer().first())
  }

  @Test
  fun `setting Letter, a book finished 5 days ago, no card`() = runTest {
    assertEquals(SupportCardOffer.NotNow, supportCard(finishedAgo = listOf(5.days)).offer().first())
  }

  @Test
  fun `setting Letter, a book finished yesterday, 2 hours listened, no card`() = runTest {
    assertEquals(SupportCardOffer.NotNow, supportCard(listened = 2.hours).offer().first())
  }

  @Test
  fun `setting Letter, a book finished yesterday, listening on 2 days, no card`() = runTest {
    assertEquals(SupportCardOffer.NotNow, supportCard(listeningDays = 2).offer().first())
  }

  @Test
  fun `a supporter, a book finished yesterday, no card`() = runTest {
    assertEquals(
      SupportCardOffer.NotNow,
      supportCard(supporter = SupporterStatus(tipped = true)).offer().first(),
    )
  }

  @Test
  fun `the letter, tap Buy Paul a tea, the card never comes back`() = runTest {
    val supportCard = supportCard()

    supportCard.open()

    assertEquals(SupportCardOffer.NotNow, supportCard.offer().first())
    verify(exactly = 1) { analytics.event("support_open", mapOf("from" to "library_card")) }
  }

  @Test
  fun `the book card, tap No thanks, the card never comes back`() = runTest {
    val supportCard = supportCard(look = SupportCardLook.Book)
    supportCard.offer().first()

    supportCard.dismiss()

    assertEquals(SupportCardOffer.NotNow, supportCard.offer().first())
    verify(exactly = 1) {
      analytics.event("support_card", mapOf("action" to "dismiss", "kind" to "book"))
    }
  }

  @Test
  fun `the letter shows, the setting changes to Book, tap No thanks, Voice reports the letter`() =
    runTest {
      val flag = MemoryFeatureFlag(SupportCardLook.Letter)
      val supportCard = supportCard(flag = flag)
      supportCard.offer().first()
      flag.value = SupportCardLook.Book

      supportCard.dismiss()

      verify(exactly = 1) {
        analytics.event("support_card", mapOf("action" to "dismiss", "kind" to "letter"))
      }
    }

  private fun supportCard(
    look: SupportCardLook = SupportCardLook.Letter,
    finishedAgo: List<Duration> = listOf(1.days),
    listened: Duration = 5.hours,
    listeningDays: Int = 3,
    supporter: SupporterStatus = SupporterStatus(),
    flag: MemoryFeatureFlag<SupportCardLook> = MemoryFeatureFlag(look),
  ): SupportCard {
    return SupportCard(
      look = SupportCardFeatureFlag(flag),
      answeredStore = SupportCardAnsweredStore(answered),
      supporterStatusStore = SupporterStatusStore(MemoryDataStore(supporter)),
      statsRepo =
        mockk<ListeningStatsRepo> {
          coEvery { summary(any(), any()) } returns
            ListeningSummary(
              listened = listened,
              listeningDays = listeningDays,
              finishedBooks =
                finishedAgo.mapIndexed { index, ago ->
                  FinishedBook(BookId("book$index"), now - ago)
                },
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
