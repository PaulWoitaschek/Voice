package voice.core.playback.session

import android.content.Context
import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.runner.RunWith
import voice.core.data.BookId
import voice.core.data.store.CurrentBookStore
import voice.core.playback.LockScreenSliderStore
import voice.core.playback.MemoryDataStore
import voice.core.playback.session.search.BookSearchParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class LibrarySessionCallbackTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val player = TestExoPlayerBuilder(context).build()
  private val session = MediaSession.Builder(context, player).build()
  private val scope = TestScope()
  private val lockScreenSliderStore = MemoryDataStore(true)
  private val notificationSession = mockk<MediaSession>(relaxed = true) {
    every { isMediaNotificationController(any()) } returns true
  }
  private val notification = controller(context.packageName, context.applicationInfo.uid)
  private val callback = LibrarySessionCallback(
    mediaItemProvider = mockk(),
    scope = scope.backgroundScope,
    player = mockk(relaxed = true),
    bookSearchParser = BookSearchParser(),
    bookSearchHandler = mockk(),
    currentBookStoreId = CurrentBookStore(MemoryDataStore<BookId?>(null)),
    bookRepository = mockk(),
    commandSourceResolver = mockk(relaxed = true),
    context = context,
    lockScreenSliderStore = LockScreenSliderStore(lockScreenSliderStore),
  )
  private val customCommand = SessionCommand(CustomCommand.CUSTOM_COMMAND_ACTION, Bundle.EMPTY)

  @After
  fun tearDown() {
    session.release()
    player.release()
  }

  @Test
  fun `the app's own controllers can send custom commands`() {
    val result = callback.onConnect(session, controller(context.packageName, context.applicationInfo.uid))

    assertTrue(result.availableSessionCommands.contains(customCommand))
  }

  @Test
  fun `other apps can't send custom commands`() {
    val result = callback.onConnect(session, controller(OTHER_PACKAGE, OTHER_UID))

    assertTrue(result.isAccepted)
    assertFalse(result.availableSessionCommands.contains(customCommand))
  }

  @Test
  fun `a malformed custom command is rejected`() {
    val args = Bundle().apply {
      putString(CustomCommand.CUSTOM_COMMAND_EXTRA, "{")
    }

    val result = callback.onCustomCommand(session, controller(context.packageName, context.applicationInfo.uid), customCommand, args)

    assertEquals(SessionError.ERROR_NOT_SUPPORTED, result.get().resultCode)
  }

  @Test
  fun `turning the lock screen slider off stops seeking from the notification`() = scope.runTest {
    lockScreenSliderStore.updateData { false }

    callback.onPostConnect(notificationSession, notification)
    runCurrent()

    val commands = notificationCommands().last()
    assertFalse(commands.contains(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM))
    assertTrue(commands.contains(Player.COMMAND_SEEK_BACK))
    assertTrue(commands.contains(Player.COMMAND_SEEK_FORWARD))
  }

  @Test
  fun `turning the lock screen slider back on lets the notification seek again`() = scope.runTest {
    lockScreenSliderStore.updateData { false }
    callback.onPostConnect(notificationSession, notification)
    runCurrent()

    lockScreenSliderStore.updateData { true }
    runCurrent()

    assertTrue(notificationCommands().last().contains(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM))
  }

  @Test
  fun `the app's own player can still seek with the lock screen slider off`() = scope.runTest {
    lockScreenSliderStore.updateData { false }

    val result = callback.onConnect(session, controller(context.packageName, context.applicationInfo.uid))

    assertTrue(result.availablePlayerCommands.contains(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM))
  }

  private fun notificationCommands(): List<Player.Commands> {
    val commands = mutableListOf<Player.Commands>()
    verify { notificationSession.setAvailableCommands(notification, any(), capture(commands)) }
    return commands
  }

  private fun controller(
    packageName: String,
    uid: Int,
  ) = MediaSession.ControllerInfo.createTestOnlyControllerInfo(
    packageName,
    0,
    uid,
    MEDIA3_CONTROLLER_VERSION,
    MEDIA3_CONTROLLER_VERSION,
    false,
    Bundle.EMPTY,
    true,
  )

  private companion object {
    const val OTHER_PACKAGE = "com.example.remote"
    const val OTHER_UID = 10_123
    const val MEDIA3_CONTROLLER_VERSION = 1
  }
}
