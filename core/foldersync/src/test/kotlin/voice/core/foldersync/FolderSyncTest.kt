package voice.core.foldersync

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
import voice.core.data.repo.BookmarkRepo
import voice.core.data.store.CurrentBookStore
import voice.core.data.syncKey
import voice.core.playback.PlayerController
import voice.core.playback.playstate.PlayStateManager
import voice.core.scanner.MediaScanTrigger
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class FolderSyncTest {

  private val scope = TestScope()
  private val playerController = mockk<PlayerController>(relaxUnitFun = true)
  private val playStateManager = PlayStateManager()
  private val currentBookStore = MemoryDataStore<BookId?>(null)
  private val scannerActive = MutableStateFlow(false)
  private val mediaScanTrigger = mockk<MediaScanTrigger> {
    every { scannerActive } returns this@FolderSyncTest.scannerActive
  }
  private val written = mutableListOf<SyncFile>()
  private val syncFiles = mockk<SyncFiles> {
    every { canWrite(any()) } returns true
    every { read(any()) } returns emptyMap()
    every { write(folder(), any(), capture(written)) } returns Unit
  }
  private val audiobookFolders = mockk<AudiobookFolders> {
    every { all() } returns flowOf(
      mapOf(FolderType.Root to listOf(DocumentFileWithUri(documentFile = mockk(), uri = folder()))),
    )
  }

  @Test
  fun `a newer place from another device is taken over with its speed`() = scope.runTest {
    val book = book(currentFile = 0, position = 5000, lastPlayedAt = 1000, playbackSpeed = 1.25F)
    val repo = MemoryBookRepository(book)
    otherDevice(book, SyncedBook(place = place(file = 1, position = 3000, speed = 1.5F, playedAt = 2000)))

    folderSync(repo).sync()

    val content = repo.get(book.id)!!.content
    assertEquals(book.chapters[1].id, content.currentChapter)
    assertEquals(3000, content.positionInChapter)
    assertEquals(Instant.ofEpochMilli(2000), content.lastPlayedAt)
    assertEquals(1.5F, content.playbackSpeed)
    verify(exactly = 0) { playerController.setPosition(any(), any(), any(), any()) }
  }

  @Test
  fun `a speed the player can't play is not taken over`() = scope.runTest {
    val book = book(currentFile = 0, position = 5000, lastPlayedAt = 1000, playbackSpeed = 1.25F)
    val repo = MemoryBookRepository(book)

    folderSync(repo).takeOver(book.id, place(file = 1, position = 3000, speed = 0F, playedAt = 2000))

    val content = repo.get(book.id)!!.content
    assertEquals(3000, content.positionInChapter)
    assertEquals(1.25F, content.playbackSpeed)
  }

  @Test
  fun `the loaded book jumps to the new place and speed`() = scope.runTest {
    val book = book(lastPlayedAt = 1000)
    currentBookStore.updateData { book.id }
    coEvery { playerController.livePlaybackState(book.id) } returns mockk()

    folderSync(MemoryBookRepository(book)).takeOver(book.id, place(file = 1, position = 3000, speed = 1.5F))

    verify {
      playerController.setPosition(
        time = 3000,
        id = book.chapters[1].id,
        source = ListeningEvent.Source.OtherDevice,
      )
      playerController.setSpeed(1.5F)
    }
  }

  @Test
  fun `the book playing here stays where it is`() = scope.runTest {
    val book = book(lastPlayedAt = 1000)
    val repo = MemoryBookRepository(book)
    currentBookStore.updateData { book.id }
    playStateManager.playState = PlayStateManager.PlayState.Playing

    folderSync(repo).takeOver(book.id, place(playedAt = 2000))

    assertEquals(book, repo.get(book.id))
    verify(exactly = 0) { playerController.setPosition(any(), any(), any(), any()) }
  }

  @Test
  fun `the place played here is written into the folder`() = scope.runTest {
    val book = book(currentFile = 1, position = 3000, lastPlayedAt = 2000)

    folderSync(MemoryBookRepository(book)).start()
    testScheduler.advanceTimeBy(4.seconds)

    assertEquals(
      expected = listOf(SyncFile(mapOf(book.key() to SyncedBook(place = place(file = 1, position = 3000, playedAt = 2000))))),
      actual = written,
    )
  }

  @Test
  fun `a sync request takes over a newer place`() = scope.runTest {
    val book = book(lastPlayedAt = 1000)
    val repo = MemoryBookRepository(book)
    otherDevice(book, SyncedBook(place = place(position = 3000, playedAt = 2000)))

    folderSync(repo).requestSync()
    testScheduler.runCurrent()

    assertEquals(3000, repo.get(book.id)!!.content.positionInChapter)
  }

  @Test
  fun `without a current book, the book played last on another device becomes the current one`() = scope.runTest {
    val older = book(path = "Older", fileSizes = listOf(10, 20))
    val newer = book(path = "Newer", fileSizes = listOf(30, 40))
    every { syncFiles.read(folder()) } returns mapOf(
      "sync-other.json" to SyncFile(
        mapOf(
          older.key() to SyncedBook(place = place(playedAt = 1000)),
          newer.key() to SyncedBook(place = place(playedAt = 2000)),
        ),
      ),
    )

    folderSync(MemoryBookRepository(older, newer)).sync()

    assertEquals(newer.id, currentBookStore.data.first())
  }

  @Test
  fun `the current book stays when another book was played on another device`() = scope.runTest {
    val current = book(path = "Current", fileSizes = listOf(10, 20), lastPlayedAt = 1000)
    val other = book(path = "Other", fileSizes = listOf(30, 40))
    currentBookStore.updateData { current.id }
    otherDevice(other, SyncedBook(place = place(playedAt = 2000)))

    folderSync(MemoryBookRepository(current, other)).sync()

    assertEquals(current.id, currentBookStore.data.first())
  }

  @Test
  fun `no book becomes the current one while the library is scanned`() = scope.runTest {
    val book = book()
    otherDevice(book, SyncedBook(place = place(playedAt = 2000)))
    scannerActive.value = true

    folderSync(MemoryBookRepository(book)).sync()

    assertNull(currentBookStore.data.first())
  }

  @Test
  fun `no book becomes the current one when none was played`() = scope.runTest {
    folderSync(MemoryBookRepository(book())).sync()

    assertNull(currentBookStore.data.first())
  }

  @Test
  fun `a name from another device is taken, and not sent back`() = scope.runTest {
    val book = book()
    val repo = MemoryBookRepository(book)
    val name = SyncedText("Other", changedAt = 1000)
    otherDevice(book, SyncedBook(name = name))
    val folderSync = folderSync(repo)

    folderSync.sync()
    ownFile(folderSync, written.single())
    folderSync.sync()

    assertEquals("Other", repo.get(book.id)!!.content.customName)
    assertEquals(listOf(SyncFile(mapOf(book.key() to SyncedBook(name = name)))), written)
  }

  @Test
  fun `a name from another device that is the files' name here is not written`() = scope.runTest {
    val book = book(path = "Book")
    val repo = MemoryBookRepository(book)
    otherDevice(book, SyncedBook(name = SyncedText("Book", changedAt = 1000)))

    folderSync(repo).sync()

    assertNull(repo.get(book.id)!!.content.customName)
    assertEquals(emptyList(), written)
  }

  @Test
  fun `a custom name that is the files' name is not written`() = scope.runTest {
    // the update that kept names apart from the files copied every name into the custom one until the next scan
    val book = book(path = "Book", customName = "Book", currentFile = 1, position = 3000, lastPlayedAt = 2000)

    folderSync(MemoryBookRepository(book)).sync()

    assertEquals(listOf(SyncFile(mapOf(book.key() to SyncedBook(place = place())))), written)
  }

  @Test
  fun `a name written before that is the files' name goes away`() = scope.runTest {
    val book = book(path = "Book")
    val folderSync = folderSync(MemoryBookRepository(book))
    ownFile(folderSync, SyncFile(mapOf(book.key() to SyncedBook(name = SyncedText("Book", changedAt = 0)))))

    folderSync.sync()

    assertEquals(listOf(SyncFile(emptyMap())), written)
  }

  @Test
  fun `a name given here wins over an older one from another device`() = scope.runTest {
    val book = book(customName = "Mine")
    val repo = MemoryBookRepository(book)
    val folderSync = folderSync(repo)
    every { syncFiles.read(folder()) } returns mapOf(
      folderSync.ownFileName to SyncFile(mapOf(book.key() to SyncedBook(name = SyncedText("Earlier", changedAt = 1000)))),
      "sync-other.json" to SyncFile(mapOf(book.key() to SyncedBook(name = SyncedText("Other", changedAt = 2000)))),
    )

    folderSync.sync()

    assertEquals("Mine", repo.get(book.id)!!.content.customName)
    assertEquals(SyncedText("Mine", changedAt = NOW), written.single().books[book.key()]!!.name)
  }

  @Test
  fun `nothing is written when nothing changed`() = scope.runTest {
    val book = book(lastPlayedAt = 2000)
    val folderSync = folderSync(MemoryBookRepository(book))
    ownFile(folderSync, SyncFile(mapOf(book.key() to SyncedBook(place = place(file = 0, position = 0, playedAt = 2000)))))

    folderSync.sync()

    assertEquals(emptyList(), written)
  }

  @Test
  fun `a folder that can't be read is left alone`() = scope.runTest {
    val book = book(lastPlayedAt = 2000)
    every { syncFiles.read(folder()) } throws IOException()

    folderSync(MemoryBookRepository(book)).sync()

    assertEquals(emptyList(), written)
  }

  @Test
  fun `a folder Voice can only read is left alone`() = scope.runTest {
    val book = book(lastPlayedAt = 1000)
    val repo = MemoryBookRepository(book)
    otherDevice(book, SyncedBook(place = place(position = 3000, playedAt = 2000)))
    every { syncFiles.canWrite(folder()) } returns false

    folderSync(repo).sync()

    assertEquals(book, repo.get(book.id))
    assertEquals(emptyList(), written)
  }

  @Test
  fun `a book that is missing keeps its entry`() = scope.runTest {
    val book = book(lastPlayedAt = 2000)
    val folderSync = folderSync(MemoryBookRepository(book))
    val missing = SyncedBook(name = SyncedText("Missing", changedAt = 1000))
    ownFile(folderSync, SyncFile(mapOf("missing" to missing)))

    folderSync.sync()

    assertEquals(missing, written.single().books["missing"])
  }

  @Test
  fun `a bookmark from another device goes into the copy played last, and is not sent back`() = scope.runTest {
    val older = book(path = "Book", lastPlayedAt = 1000)
    val newer = book(path = "Copy of Book", lastPlayedAt = 2000)
    val bookmark = newer.bookmark(file = 1, time = 1000, addedAt = 100)
    val bookmarkRepo = spyk(MemoryBookmarkRepo())
    otherDevice(newer, SyncedBook(bookmarks = mapOf(bookmark.id.value to bookmark.synced(changedAt = 1500))))
    val folderSync = folderSync(MemoryBookRepository(older, newer), bookmarkRepo)

    folderSync.sync()
    ownFile(folderSync, written.single())
    folderSync.sync()

    assertEquals(listOf(bookmark), bookmarkRepo.bookmarks.value)
    coVerify(exactly = 1) { bookmarkRepo.addBookmark(any()) }
    assertEquals(bookmark.synced(changedAt = 1500), written.single().books[newer.key()]!!.bookmarks[bookmark.id.value])
  }

  @Test
  fun `a bookmark deleted on another device is deleted here`() = scope.runTest {
    val book = book()
    val bookmark = book.bookmark(addedAt = 100)
    val bookmarkRepo = MemoryBookmarkRepo(bookmark)
    otherDevice(book, SyncedBook(deletedBookmarks = mapOf(bookmark.id.value to 2000)))

    folderSync(MemoryBookRepository(book), bookmarkRepo).sync()

    assertEquals(emptyList(), bookmarkRepo.bookmarks.value)
    assertEquals(
      expected = SyncFile(mapOf(book.key() to SyncedBook(deletedBookmarks = mapOf(bookmark.id.value to 2000)))),
      actual = written.single(),
    )
  }

  private fun otherDevice(
    book: Book,
    synced: SyncedBook,
  ) {
    every { syncFiles.read(folder()) } returns mapOf("sync-other.json" to SyncFile(mapOf(book.key() to synced)))
  }

  private fun ownFile(
    folderSync: FolderSync,
    file: SyncFile,
  ) {
    val files = syncFiles.read(folder())
    every { syncFiles.read(folder()) } returns files + (folderSync.ownFileName to file)
  }

  private fun Book.key(): String = chapters.syncKey()!!

  private fun TestScope.folderSync(
    repo: MemoryBookRepository,
    bookmarkRepo: BookmarkRepo = MemoryBookmarkRepo(),
  ): FolderSync {
    val dispatcher = StandardTestDispatcher(testScheduler)
    return FolderSync(
      context = ApplicationProvider.getApplicationContext(),
      syncFiles = syncFiles,
      audiobookFolders = audiobookFolders,
      bookRepository = repo,
      bookmarkRepo = bookmarkRepo,
      playerController = playerController,
      playStateManager = playStateManager,
      mediaScanTrigger = mediaScanTrigger,
      currentBookStore = CurrentBookStore(currentBookStore),
      clock = Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC),
      dispatcherProvider = DispatcherProvider(main = dispatcher, io = dispatcher, mainImmediate = dispatcher),
    )
  }
}

private const val NOW = 5000L
