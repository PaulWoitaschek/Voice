package voice.core.playback.playstate

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer.MediaItemData
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.store.CurrentBookStore
import voice.core.playback.FakePlayer
import voice.core.playback.MemoryDataStore
import voice.core.playback.playstate.PlayStateManager.PlayState
import voice.core.playback.session.MediaId
import voice.core.playback.session.encode

@RunWith(AndroidJUnit4::class)
class PlayStateDelegatingListenerTest {

  private val scope = TestScope()
  private val playStateManager = PlayStateManager()
  private val currentBookStore = MemoryDataStore<BookId?>(BookId("opened"))
  private val dune = BookId("dune")
  private val player = FakePlayer(dune)

  init {
    PlayStateDelegatingListener(playStateManager, CurrentBookStore(currentBookStore), scope)
      .attachTo(player)
  }

  @Test
  fun `a playing player names its book`() {
    assertEquals(expected = PlayState.Playing(dune), actual = playStateManager.playState)
  }

  @Test
  fun `the book that plays becomes the current book`() = scope.runTest {
    runCurrent()

    assertEquals(expected = dune, actual = currentBookStore.data.first())
  }

  @Test
  fun `a pause is paused`() {
    player.update { it.setPlayWhenReady(false, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) }

    assertEquals(expected = PlayState.Paused, actual = playStateManager.playState)
  }

  @Test
  fun `another book in the player plays`() {
    val hyperion = BookId("hyperion")
    val mediaItem =
      MediaItem.Builder()
        .setMediaId(MediaId.Chapter(hyperion, ChapterId("chapter")).encode())
        .build()

    player.update {
      it.setPlaylist(listOf(MediaItemData.Builder("hyperion").setMediaItem(mediaItem).build()))
    }

    assertEquals(expected = PlayState.Playing(hyperion), actual = playStateManager.playState)
  }
}
