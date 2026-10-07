package voice.core.playback

import android.app.Application
import android.os.Looper
import androidx.datastore.core.DataStore
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.test.utils.FakeMediaSource
import androidx.media3.test.utils.FakeTimeline
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.repo.BookRepository
import voice.core.playback.player.VoicePlayer
import voice.core.playback.session.LibrarySessionCallback
import voice.core.playback.session.MediaItemProvider
import voice.core.playback.session.realChapterId
import voice.core.playback.session.search.book
import voice.core.playback.session.toMediaIdOrNull
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid

@RunWith(AndroidJUnit4::class)
class PlayerControllerTest {

  private val application = ApplicationProvider.getApplicationContext<Application>()
  private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
  private val bookId = BookId(Uuid.random().toString())
  private var book: Book = book(chapters = List(3) { chapter() }, id = bookId)
  private val currentBookStore = object : DataStore<BookId?> {
    private val delegate = MemoryDataStore<BookId?>(bookId)
    override val data: Flow<BookId?> get() = delegate.data
    override suspend fun updateData(transform: suspend (BookId?) -> BookId?): BookId? {
      // Like the real DataStore which writes to disk, this makes onSetMediaItems complete asynchronously.
      delay(100.milliseconds)
      return delegate.updateData(transform)
    }
  }

  private val bookRepository = mockk<BookRepository> {
    coEvery { get(bookId) } answers { book }
    coEvery { updateBook(any(), any()) } just Runs
  }

  private val mediaItemProvider = MediaItemProvider(
    bookRepository = bookRepository,
    application = application,
    chapterRepo = mockk(),
    contentRepo = mockk(),
    imageFileProvider = mockk(),
    currentBookStoreId = currentBookStore,
  )

  private val internalPlayer = TestExoPlayerBuilder(application)
    .setMediaSourceFactory(
      mockk {
        every { createMediaSource(any()) } answers {
          val mediaItem = arg<MediaItem>(0)
          val chapter = book.chapters.single {
            it.id == mediaItem.mediaId.toMediaIdOrNull()!!.realChapterId
          }
          FakeMediaSource(
            FakeTimeline(
              FakeTimeline.TimelineWindowDefinition.Builder()
                .setSeekable(true)
                .setDurationUs(TimeUnit.MILLISECONDS.toMicros(chapter.duration))
                .setMediaItem(mediaItem)
                .build(),
            ),
          )
        }
      },
    )
    .build()

  private val player = VoicePlayer(
    player = internalPlayer,
    repo = bookRepository,
    currentBookStoreId = currentBookStore,
    seekTimeStore = MemoryDataStore(20),
    autoRewindAmountStore = MemoryDataStore(0),
    mediaItemProvider = mediaItemProvider,
    scope = scope,
    volumeGain = mockk(relaxed = true),
    sleepTimer = mockk(),
    analytics = mockk(relaxed = true),
  )

  private val session = MediaSession.Builder(application, player)
    .setCallback(
      LibrarySessionCallback(
        mediaItemProvider = mediaItemProvider,
        scope = scope,
        player = player,
        bookSearchParser = mockk(),
        bookSearchHandler = mockk(),
        currentBookStoreId = currentBookStore,
        bookRepository = bookRepository,
      ),
    )
    .build()

  private val playerController = PlayerController(
    context = application,
    currentBookStoreId = currentBookStore,
    bookRepository = bookRepository,
    mediaItemProvider = mediaItemProvider,
    sessionToken = session.token,
  )

  @After
  fun tearDown() {
    session.release()
    player.release()
    scope.cancel()
  }

  @Test
  fun `next right after a cold start skips to the next chapter`() {
    playerController.next()

    awaitReadyAndIdle()
    assertEquals(expected = 1, actual = internalPlayer.currentMediaItemIndex)
  }

  @Test
  fun `previous right after a cold start skips to the previous chapter`() {
    book = book.update { it.copy(currentChapter = book.chapters[2].id) }

    playerController.previous()

    awaitReadyAndIdle()
    assertEquals(expected = 1, actual = internalPlayer.currentMediaItemIndex)
  }

  @Test
  fun `setPosition right after a cold start seeks to the chapter`() {
    playerController.setPosition(time = 1_000, id = book.chapters[2].id)

    awaitReadyAndIdle()
    assertEquals(expected = 2, actual = internalPlayer.currentMediaItemIndex)
    assertEquals(expected = 1_000, actual = internalPlayer.currentPosition)
  }

  private fun awaitReadyAndIdle() {
    TestPlayerRunHelper.runUntilPlaybackState(internalPlayer, Player.STATE_READY)
    shadowOf(Looper.getMainLooper()).idle()
  }

  private fun chapter(): Chapter {
    return Chapter(
      id = ChapterId(Uuid.random().toString()),
      name = "chapter",
      duration = 10_000,
      fileLastModified = Instant.EPOCH,
      markData = emptyList(),
      fileSize = 0,
    )
  }
}
