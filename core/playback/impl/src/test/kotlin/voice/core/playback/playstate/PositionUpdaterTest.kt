package voice.core.playback.playstate

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import voice.core.analytics.api.Analytics
import voice.core.data.BookId
import voice.core.featureflag.MemoryFeatureFlag
import voice.core.playback.ExperimentalPlaybackPersistenceFeatureFlag
import voice.core.playback.FakePlayer
import voice.core.playback.MemoryDataStore
import voice.core.playback.PlaybackPersistenceIntervalFeatureFlag
import voice.core.playback.playstate.PlayStateManager.PlayState

@RunWith(AndroidJUnit4::class)
class PositionUpdaterTest {

  private val book = BookId("book")
  private val playStateManager = PlayStateManager()
  private val store = MemoryDataStore<Long?>(null)
  private val analytics = mockk<Analytics>(relaxUnitFun = true)

  private fun TestScope.positionUpdater(): PositionUpdater {
    return PositionUpdater(
        bookRepo = mockk(relaxed = true),
        scope = backgroundScope,
        playStateManager = playStateManager,
        experimentalPlaybackPersistenceFeatureFlag =
          ExperimentalPlaybackPersistenceFeatureFlag(MemoryFeatureFlag(true)),
        playbackPersistenceIntervalFeatureFlag =
          PlaybackPersistenceIntervalFeatureFlag(MemoryFeatureFlag("30")),
        playingSaveIntervalStore = PlayingSaveIntervalStore(store),
        analytics = analytics,
      )
      .also {
        it.attachTo(FakePlayer(book))
        runCurrent()
      }
  }

  @Test
  fun `playback that the process lost is reported once`() = runTest {
    store.updateData { 30_000 }

    val _ = positionUpdater()

    verify(exactly = 1) {
      analytics.event("playback_killed", mapOf("save_interval_ms" to "30000"))
    }
    assertNull(store.data.first())
  }

  @Test
  fun `a clean start reports nothing`() = runTest {
    val _ = positionUpdater()

    verify(exactly = 0) { analytics.event(any(), any()) }
  }

  @Test
  fun `while a book plays, the save interval is stored`() = runTest {
    val _ = positionUpdater()

    playStateManager.playState = PlayState.Playing(book)
    runCurrent()
    assertEquals(30_000, store.data.first())

    playStateManager.playState = PlayState.Paused
    runCurrent()
    assertNull(store.data.first())
  }

  @Test
  fun `a release while a book plays clears the interval`() = runTest {
    val updater = positionUpdater()
    playStateManager.playState = PlayState.Playing(book)
    runCurrent()

    updater.release()

    assertNull(store.data.first())
  }
}
