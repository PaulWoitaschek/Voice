package voice.core.playback.session

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.runner.RunWith
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.repo.BookRepository
import voice.core.data.repo.BookmarkRepo
import voice.core.playback.impl.R
import voice.core.playback.player.VoicePlayer
import voice.core.playback.playstate.PositionUpdater

@RunWith(AndroidJUnit4::class)
class MediaButtonsTest {

  private val scope = TestScope()
  private val connected = MutableStateFlow(false)
  private val listeners = mutableListOf<Player.Listener>()
  private val player =
    mockk<VoicePlayer>(relaxed = true) {
      every { playbackParameters } returns PlaybackParameters(1.5F)
      every { addListener(capture(listeners)) } just Runs
    }
  private val bookRepository = mockk<BookRepository>()
  private val bookmarkRepo = mockk<BookmarkRepo>()
  private val positionUpdater = mockk<PositionUpdater>()
  private val mediaButtons =
    MediaButtons(
      context = ApplicationProvider.getApplicationContext(),
      player = player,
      androidAutoConnection =
        mockk {
          every { this@mockk.connected } returns this@MediaButtonsTest.connected
        },
      bookRepository = bookRepository,
      bookmarkRepo = bookmarkRepo,
      positionUpdater = positionUpdater,
      scope = scope.backgroundScope,
    )

  @Test
  fun `the car gets buttons for chapters, a bookmark and the speed it plays at`() = scope.runTest {
    mediaButtons.preferences().test {
      assertEquals(
        expected = listOf(CommandButton.ICON_SKIP_BACK, CommandButton.ICON_SKIP_FORWARD),
        actual = awaitItem().map { it.icon },
      )

      connected.value = true
      val car = awaitItem()
      assertEquals(
        expected =
          listOf(
            CommandButton.ICON_SKIP_BACK,
            CommandButton.ICON_SKIP_FORWARD,
            CommandButton.ICON_PREVIOUS,
            CommandButton.ICON_NEXT,
            CommandButton.ICON_UNDEFINED,
            CommandButton.ICON_PLAYBACK_SPEED_1_5,
          ),
        actual = car.map { it.icon },
      )
      assertEquals(expected = R.drawable.ic_bookmark_add, actual = car[4].iconResId)

      listeners.single().onPlaybackParametersChanged(PlaybackParameters(1.8F))
      assertEquals(
        expected = CommandButton.ICON_PLAYBACK_SPEED_1_8,
        actual = awaitItem().last().icon,
      )

      connected.value = false
      assertEquals(expected = 2, actual = awaitItem().size)
    }
  }

  @Test
  fun `the speed button goes to the next speed`() {
    assertTrue(mediaButtons.onCustomCommand(MediaButtons.NEXT_SPEED))

    verify { player.setPlaybackSpeed(1.8F) }
  }

  @Test
  fun `the speed steps wrap around and pick up speeds set in the app`() {
    assertEquals(
      expected = listOf(1.2F, 1.5F, 1.8F, 2F, 1F, 1.5F, 1F, 1F),
      actual = listOf(1F, 1.2F, 1.5F, 1.8F, 2F, 1.25F, 0.75F, 3F).map(::nextCarSpeed),
    )
  }

  @Test
  fun `a speed without its own icon gets the plain speed icon`() {
    assertEquals(expected = CommandButton.ICON_PLAYBACK_SPEED, actual = speedIcon(1.25F))
  }

  @Test
  fun `the bookmark button saves where the book plays right now`() = scope.runTest {
    val bookId = BookId("book")
    val book = mockk<Book>()
    every { player.currentMediaItem } returns
      MediaItem.Builder()
        .setMediaId(
          Json.encodeToString(MediaId.serializer(), MediaId.Chapter(bookId, ChapterId("chapter"))),
        )
        .build()
    coEvery { positionUpdater.flushPositionNow() } just Runs
    coEvery { bookRepository.get(bookId) } returns book
    coEvery { bookmarkRepo.addBookmarkAtBookPosition(book, null) } returns mockk()

    assertTrue(mediaButtons.onCustomCommand(MediaButtons.ADD_BOOKMARK))
    runCurrent()

    coVerifyOrder {
      positionUpdater.flushPositionNow()
      val _ = bookmarkRepo.addBookmarkAtBookPosition(book, null)
    }
  }
}
