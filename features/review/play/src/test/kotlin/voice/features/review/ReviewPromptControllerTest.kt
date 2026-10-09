package voice.features.review

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import voice.core.analytics.api.Analytics
import voice.core.common.AppInfoProvider
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.ListeningSession
import voice.core.data.ReviewPromptState
import voice.core.data.repo.BookRepository
import voice.core.data.repo.FinishedBook
import voice.core.data.repo.ListeningStatsRepo
import voice.core.data.repo.ListeningSummary
import voice.core.featureflag.MemoryFeatureFlag
import voice.core.playback.playstate.PlayStateManager
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toKotlinInstant

class ReviewPromptControllerTest {

  private val now = Instant.parse("2026-10-07T18:00:00Z")
  private val store = MemoryDataStore(ReviewPromptState())
  private val playStateManager = PlayStateManager()
  private val enabled = MemoryFeatureFlag(true)
  private val force = MemoryFeatureFlag(false)
  private val analytics = RecordingAnalytics()
  private val statsRepo = FakeListeningStatsRepo()
  private var books = emptyList<Book>()
  private val bookRepository = mockk<BookRepository> {
    coEvery { all() } answers { books }
  }

  private val dune = book(id = "dune", name = "Dune", author = "Frank Herbert", listenedMinutes = 600, durationMinutes = 600)
  private val hyperion = book(id = "hyperion", name = "Hyperion", author = "Dan Simmons", listenedMinutes = 60, durationMinutes = 600)
  private val emma = book(id = "emma", name = "Emma", author = "frank herbert ", listenedMinutes = 0, durationMinutes = 600)

  private fun TestScope.controller() = ReviewPromptController(
    store = store,
    listeningStatsRepo = statsRepo,
    bookRepository = bookRepository,
    playStateManager = playStateManager,
    appInfoProvider = object : AppInfoProvider {
      override val versionName = "1.2.3"
      override val analyticsIncluded = false
      override val installTime = Instant.parse("2026-06-01T00:00:00Z").toKotlinInstant()
    },
    clock = Clock.fixed(now, ZoneOffset.UTC),
    analytics = analytics,
    enabled = enabled,
    force = force,
    scope = this,
  )

  private fun listenedEnough(finishedBooks: List<FinishedBook> = emptyList()) {
    statsRepo.summary = ListeningSummary(listened = 6.hours, listeningDays = 4, finishedBooks = finishedBooks)
  }

  @Test
  fun `a finished book is celebrated with the library stats`() = runTest {
    books = listOf(dune, hyperion, emma)
    listenedEnough(finishedBooks = listOf(FinishedBook(dune.id, at = now.minusSeconds(60))))

    val prompt = controller().prompt()

    assertEquals(
      expected = ReviewPrompt(
        headline = ReviewPrompt.Headline.BookFinished(name = "Dune", cover = null),
        stats = ReviewPrompt.Stats(listenedHours = 11, finishedBooks = 1, authors = 2),
        forced = false,
      ),
      actual = prompt,
    )
    assertEquals(expected = 1, actual = store.data.first().asks)
    assertEquals(
      expected = listOf("review_prompt_shown" to mapOf("trigger" to "book_finished", "ask" to "1")),
      actual = analytics.events,
    )
  }

  @Test
  fun `the counted listening shows when it's more than the library knows`() = runTest {
    books = listOf(hyperion)
    statsRepo.summary = ListeningSummary(listened = 26.hours, listeningDays = 9, finishedBooks = emptyList())

    val prompt = controller().prompt()

    assertEquals(
      expected = ReviewPrompt(
        headline = ReviewPrompt.Headline.Milestone,
        stats = ReviewPrompt.Stats(listenedHours = 26, finishedBooks = 0, authors = 1),
        forced = false,
      ),
      actual = prompt,
    )
    assertEquals(
      expected = listOf("review_prompt_shown" to mapOf("trigger" to "milestone_25", "ask" to "1")),
      actual = analytics.events,
    )
  }

  @Test
  fun `a finished book that's gone from the library is not celebrated`() = runTest {
    books = listOf(hyperion)
    listenedEnough(finishedBooks = listOf(FinishedBook(dune.id, at = now.minusSeconds(60))))

    assertEquals(expected = ReviewPrompt.Headline.Milestone, actual = controller().prompt()?.headline)
  }

  @Test
  fun `nothing interrupts a story`() = runTest {
    books = listOf(dune)
    listenedEnough()
    playStateManager.playState = PlayStateManager.PlayState.Playing

    assertNull(controller().prompt())
    assertEquals(expected = ReviewPromptState(), actual = store.data.first())
  }

  @Test
  fun `nothing shows while the feature is off`() = runTest {
    books = listOf(dune)
    listenedEnough()
    enabled.value = false

    assertNull(controller().prompt())
  }

  @Test
  fun `the shown prompt comes back until it's answered`() = runTest {
    books = listOf(dune)
    listenedEnough()
    val controller = controller()

    val prompt = controller.prompt()!!
    assertEquals(expected = prompt, actual = controller.prompt())

    controller.answer(prompt, ReviewAnswer.Later)
    runCurrent()
    assertNull(controller.prompt())
    assertEquals(expected = 1, actual = store.data.first().asks)
  }

  @Test
  fun `rating is remembered`() = runTest {
    books = listOf(dune)
    listenedEnough()
    val controller = controller()

    controller.answer(controller.prompt()!!, ReviewAnswer.Rate)
    runCurrent()

    assertEquals(expected = true, actual = store.data.first().done)
    assertEquals(expected = "review_prompt_answer" to mapOf("answer" to "rate"), actual = analytics.events.last())
  }

  @Test
  fun `the flag forces the prompt once, without counting it`() = runTest {
    books = listOf(hyperion)
    statsRepo.summary = ListeningSummary(listened = 1.minutes, listeningDays = 1, finishedBooks = emptyList())
    force.value = true
    val controller = controller()

    val prompt = controller.prompt()!!
    assertEquals(expected = true, actual = prompt.forced)
    controller.answer(prompt, ReviewAnswer.Rate)
    runCurrent()

    assertNull(controller.prompt())
    assertEquals(expected = ReviewPromptState(), actual = store.data.first())
    assertEquals(expected = emptyList(), actual = analytics.events)
  }

  private fun book(
    id: String,
    name: String,
    author: String?,
    listenedMinutes: Long,
    durationMinutes: Long,
  ): Book {
    val chapter = Chapter(
      id = ChapterId("$id-chapter"),
      name = null,
      duration = durationMinutes.minutes.inWholeMilliseconds,
      fileLastModified = Instant.EPOCH,
      markData = emptyList(),
      fileSize = 0,
    )
    return Book(
      content = BookContent(
        id = BookId(id),
        playbackSpeed = 1F,
        skipSilence = false,
        isActive = true,
        lastPlayedAt = Instant.EPOCH,
        author = author,
        name = name,
        addedAt = Instant.EPOCH,
        chapters = listOf(chapter.id),
        currentChapter = chapter.id,
        positionInChapter = listenedMinutes.minutes.inWholeMilliseconds,
        cover = null,
        gain = 0F,
        genre = null,
        narrator = null,
        series = null,
        part = null,
      ),
      chapters = listOf(chapter),
    )
  }
}

private class RecordingAnalytics : Analytics {

  val events = mutableListOf<Pair<String, Map<String, String>>>()

  override fun screenView(screenName: String) {}

  override fun event(
    name: String,
    params: Map<String, String>,
  ) {
    events += name to params
  }
}

private class FakeListeningStatsRepo : ListeningStatsRepo {

  var summary = ListeningSummary(listened = 0.hours, listeningDays = 0, finishedBooks = emptyList())

  override suspend fun save(session: ListeningSession): Long = error("Not used")

  override suspend fun summary(
    from: Instant,
    to: Instant,
  ): ListeningSummary = summary

  override suspend fun moveToBook(
    from: List<BookId>,
    to: BookId,
  ) = error("Not used")
}
