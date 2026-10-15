package voice.core.foldersync

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import voice.core.common.DispatcherProvider
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.ListeningEvent
import voice.core.data.folders.AudiobookFolders
import voice.core.data.folders.DocumentFileWithUri
import voice.core.data.folders.FolderType
import voice.core.data.syncKey
import voice.core.playback.PlayerController
import voice.core.playback.playstate.PlayStateManager
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class FolderSyncTest {

  private val scope = TestScope()
  private val playerController = mockk<PlayerController>(relaxUnitFun = true)
  private val playStateManager = PlayStateManager()
  private val currentBookStore = MemoryDataStore<BookId?>(null)
  private val syncFiles = mockk<SyncFiles>(relaxUnitFun = true) {
    every { read(any(), any()) } returns emptyList()
  }
  private val audiobookFolders = mockk<AudiobookFolders> {
    every { all() } returns flowOf(
      mapOf(FolderType.Root to listOf(DocumentFileWithUri(documentFile = mockk(), uri = folder()))),
    )
  }

  @Test
  fun `a newer place from another device is taken over`() = scope.runTest {
    val book = book(currentFile = 0, position = 5000, lastPlayedAt = 1000, playbackSpeed = 1.25F)
    val repo = MemoryBookRepository(book)
    every { syncFiles.read(folder(), any()) } returns listOf(
      SyncFile(mapOf(book.chapters.syncKey()!! to SyncedPosition(file = 1, position = 3000, updatedAt = 2000))),
    )

    folderSync(repo).read()

    val content = repo.get(book.id)!!.content
    assertEquals(book.chapters[1].id, content.currentChapter)
    assertEquals(3000, content.positionInChapter)
    assertEquals(Instant.ofEpochMilli(2000), content.lastPlayedAt)
    assertEquals(1.25F, content.playbackSpeed)
    verify(exactly = 0) { playerController.setPosition(any(), any(), any(), any()) }
  }

  @Test
  fun `the loaded book jumps to the new place`() = scope.runTest {
    val book = book(lastPlayedAt = 1000)
    currentBookStore.updateData { book.id }
    coEvery { playerController.livePlaybackState(book.id) } returns mockk()

    folderSync(MemoryBookRepository(book)).takeOver(book.id, SyncedPosition(file = 1, position = 3000, updatedAt = 2000))

    verify {
      playerController.setPosition(
        time = 3000,
        id = book.chapters[1].id,
        source = ListeningEvent.Source.OtherDevice,
      )
    }
  }

  @Test
  fun `the book playing here stays where it is`() = scope.runTest {
    val book = book(lastPlayedAt = 1000)
    val repo = MemoryBookRepository(book)
    currentBookStore.updateData { book.id }
    playStateManager.playState = PlayStateManager.PlayState.Playing

    folderSync(repo).takeOver(book.id, SyncedPosition(file = 1, position = 3000, updatedAt = 2000))

    assertEquals(book, repo.get(book.id))
    verify(exactly = 0) { playerController.setPosition(any(), any(), any(), any()) }
  }

  @Test
  fun `a book never played here starts at the last speed`() = scope.runTest {
    val book = book()
    val repo = MemoryBookRepository(book)

    folderSync(repo).takeOver(book.id, SyncedPosition(file = 1, position = 3000, updatedAt = 2000))

    assertEquals(1.75F, repo.get(book.id)!!.content.playbackSpeed)
  }

  @Test
  fun `the place played here is written into the folder`() = scope.runTest {
    val book = book(currentFile = 1, position = 3000, lastPlayedAt = 2000)
    val written = slot<SyncFile>()
    every { syncFiles.write(folder(), any(), capture(written)) } returns Unit

    folderSync(MemoryBookRepository(book)).start()
    testScheduler.advanceTimeBy(4.seconds)

    assertEquals(
      expected = SyncFile(mapOf(book.chapters.syncKey()!! to SyncedPosition(file = 1, position = 3000, updatedAt = 2000))),
      actual = written.captured,
    )
  }

  @Test
  fun `a read request takes over a newer place`() = scope.runTest {
    val book = book(lastPlayedAt = 1000)
    val repo = MemoryBookRepository(book)
    every { syncFiles.read(folder(), any()) } returns listOf(
      SyncFile(mapOf(book.chapters.syncKey()!! to SyncedPosition(file = 1, position = 3000, updatedAt = 2000))),
    )

    folderSync(repo).requestRead()
    testScheduler.runCurrent()

    assertEquals(3000, repo.get(book.id)!!.content.positionInChapter)
  }

  private fun TestScope.folderSync(repo: MemoryBookRepository): FolderSync {
    val dispatcher = StandardTestDispatcher(testScheduler)
    return FolderSync(
      context = ApplicationProvider.getApplicationContext(),
      syncFiles = syncFiles,
      audiobookFolders = audiobookFolders,
      bookRepository = repo,
      playerController = playerController,
      playStateManager = playStateManager,
      currentBookStore = currentBookStore,
      lastPlaybackSpeedStore = MemoryDataStore(1.75F),
      dispatcherProvider = DispatcherProvider(main = dispatcher, io = dispatcher, mainImmediate = dispatcher),
    )
  }
}
