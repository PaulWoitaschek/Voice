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
class AwaitCommandTest {

  private val player = FakePlayer(BookId("book"))

  private fun withoutSeek() {
    player.update {
      it.setAvailableCommands(
        Player.Commands.Builder().addAllCommands().remove(Player.COMMAND_SEEK_FORWARD).build(),
      )
    }
  }

  @Test
  fun `a book that can seek takes the skip at once`() = runTest {
    assertTrue(player.awaitCommand(Player.COMMAND_SEEK_FORWARD))
  }

  @Test
  fun `a book that Voice just loaded takes the skip once its file is ready`() = runTest {
    withoutSeek()
    val result = async { player.awaitCommand(Player.COMMAND_SEEK_FORWARD) }
    runCurrent()

    player.update {
      it.setAvailableCommands(Player.Commands.Builder().addAllCommands().build())
    }

    assertTrue(result.await())
  }

  @Test
  fun `a book whose file never gets ready drops the skip`() = runTest {
    withoutSeek()
    assertFalse(player.awaitCommand(Player.COMMAND_SEEK_FORWARD))
  }
}
