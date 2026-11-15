package voice.core.playback

import androidx.media3.common.Player
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import voice.core.data.BookId

@RunWith(AndroidJUnit4::class)
class AwaitReadyTest {

  private val player = FakePlayer(BookId("book"))

  private fun loading() {
    player.update { it.setPlaybackState(Player.STATE_BUFFERING) }
  }

  @Test
  fun `a ready player that can seek returns true at once`() = runTest {
    assertTrue(player.awaitReady(Player.COMMAND_SEEK_FORWARD))
  }

  @Test
  fun `a loading player returns true once it is ready`() = runTest {
    loading()
    val result = async { player.awaitReady(Player.COMMAND_SEEK_FORWARD) }
    runCurrent()

    player.update { it.setPlaybackState(Player.STATE_READY) }

    assertTrue(result.await())
  }

  @Test
  fun `a loading player returns true when it ends, as a finished book does`() = runTest {
    loading()
    val result = async { player.awaitReady(Player.COMMAND_SEEK_BACK) }
    runCurrent()

    player.update { it.setPlaybackState(Player.STATE_ENDED) }

    assertTrue(result.await())
  }

  @Test
  fun `a player that never gets ready returns false, also with the seek commands of the book before`() =
    runTest {
      loading()
      assertFalse(player.awaitReady(Player.COMMAND_SEEK_FORWARD))
    }

  @Test
  fun `a ready player without the seek command returns false`() = runTest {
    player.update {
      it.setAvailableCommands(
        Player.Commands.Builder().addAllCommands().remove(Player.COMMAND_SEEK_FORWARD).build(),
      )
    }
    assertFalse(player.awaitReady(Player.COMMAND_SEEK_FORWARD))
  }
}
