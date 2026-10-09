package voice.core.playback.session

import android.content.Context
import android.os.Bundle
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import org.junit.After
import org.junit.runner.RunWith
import voice.core.data.BookId
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
  private val callback = LibrarySessionCallback(
    mediaItemProvider = mockk(),
    scope = TestScope(),
    player = mockk(relaxed = true),
    bookSearchParser = BookSearchParser(),
    bookSearchHandler = mockk(),
    currentBookStoreId = MemoryDataStore<BookId?>(null),
    bookRepository = mockk(),
    commandSourceResolver = mockk(relaxed = true),
    context = context,
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
