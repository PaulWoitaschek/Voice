package voice.app

import android.content.Context
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import dev.zacsweers.metro.Inject
import io.kotest.matchers.longs.shouldBeGreaterThan
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import voice.common.pref.CurrentBookStore
import voice.common.pref.FadeOutStore
import voice.common.pref.SleepTimerPreferenceStore
import voice.common.rootGraphAs
import voice.common.sleepTimer.SleepTimerPreference
import voice.data.BookContent
import voice.data.BookId
import voice.data.Chapter
import voice.data.ChapterId
import voice.data.repo.BookContentRepo
import voice.data.repo.ChapterRepo
import voice.playback.PlayerController
import voice.playback.playstate.PlayStateManager
import voice.playback.session.SleepTimer
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class SleepTimerIntegrationTest {

  @Inject
  lateinit var playerController: PlayerController

  @field:[Inject CurrentBookStore]
  lateinit var currentBookStore: DataStore<BookId?>

  @field:[Inject SleepTimerPreferenceStore]
  lateinit var sleepTimerPreferenceStore: DataStore<SleepTimerPreference>

  @Inject
  lateinit var bookContentRepo: BookContentRepo

  @Inject
  lateinit var chapterRepo: ChapterRepo

  @Inject
  lateinit var sleepTimer: SleepTimer

  @Inject
  lateinit var playStateManager: PlayStateManager

  @field:[Inject FadeOutStore]
  lateinit var fadeOutStore: DataStore<Duration>

  @Test
  fun test() = runTest {
    rootGraphAs<TestGraph>().inject(this@SleepTimerIntegrationTest)

    prepareTestBook()

    // speed up the tests by using shorter fade out and sleep times
    fadeOutStore.updateData { 1.seconds }
    sleepTimerPreferenceStore.updateData { it.copy(duration = 3.seconds) }

    // play the book and wait for it to start
    playerController.play()
    playStateManager.flow.first { it == PlayStateManager.PlayState.Playing }

    sleepTimer.setActive(true)

    // wait for the sleep timer to trigger
    sleepTimer.leftSleepTimeFlow.first { it == Duration.ZERO }
    playStateManager.flow.first { it == PlayStateManager.PlayState.Paused }

    bookContentRepo.all().single().positionInChapter.shouldBeGreaterThan(0)
  }

  private suspend fun prepareTestBook() {
    val audioFile = copyTestAudioFile()

    val bookId = BookId(UUID.randomUUID().toString())
    val chapterId = ChapterId(audioFile.toUri())

    val chapter = Chapter(
      id = chapterId,
      duration = 119210,
      name = "Test Chapter",
      fileLastModified = Instant.EPOCH,
      markData = emptyList(),
    )

    val bookContent = BookContent(
      id = bookId,
      playbackSpeed = 1.0f,
      skipSilence = false,
      isActive = true,
      lastPlayedAt = Instant.now(),
      author = "Test Author",
      name = "Test Audio Book",
      addedAt = Instant.now(),
      chapters = listOf(chapterId),
      currentChapter = chapterId,
      positionInChapter = 0L,
      cover = null,
      gain = 0f,
    )

    bookContentRepo.put(bookContent)
    chapterRepo.put(chapter)

    currentBookStore.updateData { bookId }
  }

  private fun copyTestAudioFile(): File {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val outputFile = File(context.filesDir, "auphonic_chapters_demo.m4a")
    InstrumentationRegistry.getInstrumentation().context.assets
      .open("auphonic_chapters_demo.m4a").use { inputStream ->
        outputFile.outputStream().use { outputStream ->
          inputStream.copyTo(outputStream)
        }
      }
    return outputFile
  }
}
