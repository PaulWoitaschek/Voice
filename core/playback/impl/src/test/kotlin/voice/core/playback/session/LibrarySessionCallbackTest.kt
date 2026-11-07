package voice.core.playback.session

import android.content.Context
import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import java.util.concurrent.ExecutionException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.runner.RunWith
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.logging.api.Logger
import voice.core.playback.LockScreenSliderStore
import voice.core.playback.MemoryDataStore
import voice.core.playback.player.VoicePlayer
import voice.core.playback.session.search.BookSearchParser
import voice.core.search.BookSearch
import voice.core.search.BookSearchResult

@RunWith(AndroidJUnit4::class)
class LibrarySessionCallbackTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val player = TestExoPlayerBuilder(context).build()
  private val session = MediaSession.Builder(context, player).build()
  private val scope = TestScope()
  private val lockScreenSliderStore = MemoryDataStore(true)
  private val notificationSession =
    mockk<MediaSession>(relaxed = true) {
      every { isMediaNotificationController(any()) } returns true
    }
  private val notification = controller(context.packageName, context.applicationInfo.uid)
  private val mediaItemProvider = mockk<MediaItemProvider>()
  private val bookSearch = mockk<BookSearch>()
  private val voicePlayer = mockk<VoicePlayer>(relaxed = true)
  private val currentBookStore = MemoryDataStore<BookId?>(null)
  private val bookRepository = mockk<BookRepository>()
  private val chapterMarks =
    MediaItemsWithStartPosition(
      List(2) { markIndex ->
        MediaItem(
          title = "Mark $markIndex",
          mediaId =
            MediaId.ChapterMark(
              bookId = BOOK_ID,
              chapterId = ChapterId("chapter"),
              markIndex = markIndex,
              startMs = markIndex * 10_000L,
              endMs = (markIndex + 1) * 10_000L,
            ),
          isPlayable = true,
          browsable = false,
          mediaType = MediaType.AudioBookChapter,
        )
      },
      1,
      3_000,
    )
  private val callback = callback(scope.backgroundScope)
  private val customCommand = SessionCommand(CustomCommand.CUSTOM_COMMAND_ACTION, Bundle.EMPTY)

  @After
  fun tearDown() {
    session.release()
    player.release()
    unmockkAll()
  }

  @Test
  fun `the app's own controllers can send custom commands`() {
    val result =
      callback.onConnect(session, controller(context.packageName, context.applicationInfo.uid))

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
    val args =
      Bundle().apply {
        putString(CustomCommand.CUSTOM_COMMAND_EXTRA, "{")
      }

    val result =
      callback.onCustomCommand(
        session,
        controller(context.packageName, context.applicationInfo.uid),
        customCommand,
        args,
      )

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

    val result =
      callback.onConnect(session, controller(context.packageName, context.applicationInfo.uid))

    assertTrue(result.availablePlayerCommands.contains(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM))
  }

  @Test
  fun `without a book to resume, Android offers no recent book`() {
    every { mediaItemProvider.recent() } returns null

    val result =
      callback.onGetLibraryRoot(
        mockk(),
        controller(OTHER_PACKAGE, OTHER_UID),
        LibraryParams.Builder().setRecent(true).build(),
      )

    assertEquals(SessionError.ERROR_NOT_SUPPORTED, result.get().resultCode)
  }

  @Test
  fun `searching in Android Auto lists the matching books`() = scope.runTest {
    val book = mockk<Book>()
    val item =
      MediaItem(
        title = "The Hobbit",
        mediaId = MediaId.Book(BookId("book")),
        isPlayable = true,
        browsable = false,
        mediaType = MediaType.AudioBook,
      )
    every { mediaItemProvider.mediaItem(book) } returns item
    coEvery { bookSearch.search("tolkien") } returns
      listOf(BookSearchResult(book, emptyMap(), emptySet(), score = 1))
    val librarySession = mockk<MediaLibrarySession>(relaxed = true)
    val androidAuto = controller(ANDROID_AUTO_PACKAGE, OTHER_UID)

    val _ = callback.onSearch(librarySession, androidAuto, "tolkien", null)
    val result =
      callback.onGetSearchResult(librarySession, androidAuto, "tolkien", 0, Int.MAX_VALUE, null)
    runCurrent()

    verify { librarySession.notifySearchResultChanged(androidAuto, "tolkien", 1, null) }
    assertEquals(expected = listOf(item), actual = result.get().value?.toList())
  }

  @Test
  fun `a failing library lookup is reported`() {
    mockkObject(Logger)
    coEvery { mediaItemProvider.item("book") } throws IllegalStateException()

    val result =
      callback(serviceLikeScope())
        .onGetItem(mockk(), controller(ANDROID_AUTO_PACKAGE, OTHER_UID), "book")

    assertFailsWith<ExecutionException> { result.get() }
    verify { Logger.nonFatal(ofType<IllegalStateException>()) }
  }

  @Test
  fun `without a current book, there is nothing to resume`() {
    mockkObject(Logger)

    val result = callback(serviceLikeScope()).onPlaybackResumption(session, notification, true)

    val exception = assertFailsWith<ExecutionException> { result.get() }
    assertIs<UnsupportedOperationException>(exception.cause)
    verify(exactly = 0) { Logger.nonFatal(any()) }
  }

  @Test
  fun `when Android Auto connects, the current book starts at its stored position`() =
    scope.runTest {
      val book = mockk<Book>()
      currentBookStore.updateData { BOOK_ID }
      coEvery { bookRepository.get(BOOK_ID) } returns book
      every { mediaItemProvider.mediaItemsWithStartPosition(book) } returns chapterMarks
      every { voicePlayer.playbackState } returns Player.STATE_IDLE

      val _ = callback.onConnect(session, controller(ANDROID_AUTO_PACKAGE, OTHER_UID))
      runCurrent()

      verifyOrder {
        voicePlayer.setMediaItems(
          chapterMarks.mediaItems,
          chapterMarks.startIndex,
          chapterMarks.startPositionMs,
        )
        voicePlayer.prepare()
      }
    }

  @Test
  fun `a book item with a start index starts where the book stopped`() = scope.runTest {
    val bookItem = bookItem(BOOK_ID)
    coEvery { mediaItemProvider.mediaItemsWithStartPosition(bookItem.mediaId) } returns chapterMarks

    val result =
      callback.onSetMediaItems(
        session,
        controller(OTHER_PACKAGE, OTHER_UID),
        mutableListOf(bookItem(BookId("other")), bookItem),
        1,
        0,
      )
    runCurrent()

    assertEquals(expected = chapterMarks, actual = result.get())
    assertEquals(expected = BOOK_ID, actual = currentBookStore.data.first())
  }

  private fun bookItem(id: BookId) =
    MediaItem(
      title = "The Hobbit",
      mediaId = MediaId.Book(id),
      isPlayable = true,
      browsable = false,
      mediaType = MediaType.AudioBook,
    )

  // Unlike TestScope.backgroundScope, it doesn't fail the test when a future fails.
  private fun serviceLikeScope() = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())

  private fun callback(coroutineScope: CoroutineScope) =
    LibrarySessionCallback(
      mediaItemProvider = mediaItemProvider,
      scope = coroutineScope,
      player = voicePlayer,
      bookSearchParser = BookSearchParser(),
      bookSearchHandler = mockk(),
      currentBookStoreId = CurrentBookStore(currentBookStore),
      bookRepository = bookRepository,
      commandSourceResolver = mockk(relaxed = true),
      context = context,
      lockScreenSliderStore = LockScreenSliderStore(lockScreenSliderStore),
      bookSearch = bookSearch,
    )

  private fun notificationCommands(): List<Player.Commands> {
    val commands = mutableListOf<Player.Commands>()
    verify { notificationSession.setAvailableCommands(notification, any(), capture(commands)) }
    return commands
  }

  private fun controller(
    packageName: String,
    uid: Int,
  ) =
    MediaSession.ControllerInfo.createTestOnlyControllerInfo(
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
    const val ANDROID_AUTO_PACKAGE = "com.google.android.projection.gearhead"
    const val OTHER_UID = 10_123
    const val MEDIA3_CONTROLLER_VERSION = 1
    val BOOK_ID = BookId("book")
  }
}
