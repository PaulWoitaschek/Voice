package voice.core.foldersync

import android.content.Context
import android.net.Uri
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import voice.core.common.DispatcherProvider
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.Bookmark
import voice.core.data.ListeningEvent
import voice.core.data.folders.AudiobookFolders
import voice.core.data.folders.DocumentFileWithUri
import voice.core.data.folders.FolderType
import voice.core.data.repo.BookRepository
import voice.core.data.repo.BookmarkRepo
import voice.core.data.store.CurrentBookStore
import voice.core.logging.api.Logger
import voice.core.playback.PlayerController
import voice.core.playback.playstate.PlayStateManager
import voice.core.scanner.MediaScanTrigger
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/**
 * Takes the listener's place, speed, names and bookmarks to their other devices without a server. Each device writes
 * what it knows into a file in the library folders, which the listener syncs with a tool of their own, and takes over
 * what changed later in the files of the other devices.
 */
@SingleIn(AppScope::class)
@Inject
public class FolderSync internal constructor(
  private val context: Context,
  private val syncFiles: SyncFiles,
  private val audiobookFolders: AudiobookFolders,
  private val bookRepository: BookRepository,
  private val bookmarkRepo: BookmarkRepo,
  private val playerController: PlayerController,
  private val playStateManager: PlayStateManager,
  private val mediaScanTrigger: MediaScanTrigger,
  private val currentBookStore: CurrentBookStore,
  private val clock: Clock,
  dispatcherProvider: DispatcherProvider,
) {

  private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)

  // two syncs at once could both move the same book and add the jump to its history twice
  private val lock = Mutex()
  private var started = false

  internal val ownFileName by lazy { "sync-${deviceId()}.json" }

  internal fun start() {
    if (started) return
    started = true
    scope.launch { syncChanges() }
  }

  internal fun requestSync() {
    scope.launch { sync() }
  }

  @OptIn(FlowPreview::class)
  private suspend fun syncChanges() {
    // while playing, the position changes every few hundred ms, so this syncs once playback stops
    combine(
      audiobookFolders.all(),
      bookRepository.flow(),
      bookmarkRepo.flow(),
      // so a book becomes the current one once the scan is done
      mediaScanTrigger.scannerActive,
    ) { _, _, _, _ -> }
      .debounce(3.seconds)
      .collect { sync() }
  }

  internal suspend fun sync() {
    lock.withLock {
      audiobookFolders.all().first().uris().forEach { syncFolder(it) }
      // mid-scan, the book played last may not be in the library yet
      if (!mediaScanTrigger.scannerActive.first()) continueBookPlayedLast()
    }
  }

  // the current book isn't synced, so a new phone, or one whose Voice data was cleared, would have nothing to continue
  private suspend fun continueBookPlayedLast() {
    if (currentBookStore.data.first() != null) return
    val book = bookRepository.all()
      .filter { it.content.lastPlayedAt != Instant.EPOCH }
      .maxByOrNull { it.content.lastPlayedAt }
      ?: return
    currentBookStore.updateData { it ?: book.id }
  }

  private suspend fun syncFolder(folder: Uri) {
    // without a file of its own, the files of the other devices would undo the renames and deletions made here
    if (!syncFiles.canWrite(folder)) return
    val books = bookRepository.all().keyedIn(folder)
    if (books.isEmpty()) return
    val bookmarks = bookmarkRepo.flow().first().groupBy { it.chapterId }
    // writing without its own file would lose the times in it, so the folder waits for the next sync
    val files = inFolder { syncFiles.read(folder) } ?: return
    val ownFile = files[ownFileName]
    val own = ownFile?.books.orEmpty()
    val others = (files - ownFileName).values
    val now = clock.millis()
    val synced = books.groupBy(keySelector = { it.key }, valueTransform = { it.book })
      .mapValues { (key, copies) ->
        val mine = copies.toSyncedBook(bookmarks, own[key], ownFileExists = ownFile != null, now = now)
        val newest = others.mapNotNull { it.books[key] }.fold(mine, SyncedBook::with)
        // before writing, so if Voice stops in between, the next sync sends back what it took, not what it replaced
        takeOver(copies, mine, newest)
        newest.withoutFilesName(copies)
      }
    // books that are missing for now, like on an SD card that's out, keep their entry
    val file = SyncFile((own + synced).filterValues { it != SyncedBook() })
    // a folder without a book that was played, renamed or bookmarked gets no .voice folder
    if (file != (ownFile ?: SyncFile(emptyMap()))) {
      val _ = inFolder { syncFiles.write(folder, ownFileName, file) }
    }
  }

  private suspend fun takeOver(
    copies: List<Book>,
    mine: SyncedBook,
    newest: SyncedBook,
  ) {
    val place = newest.place
    if (place != null && place != mine.place) {
      copies.forEach { takeOver(it.id, place) }
    }
    val name = newest.name
    if (name != null && name != mine.name) {
      copies.forEach { copy ->
        bookRepository.updateBook(copy.id) { it.withCustomName(name.value) }
      }
    }
    (mine.bookmarks.keys - newest.bookmarks.keys).forEach { bookmarkRepo.deleteBookmark(Bookmark.Id(it)) }
    val book = copies.maxBy { it.content.lastPlayedAt }
    newest.bookmarks.forEach { (id, bookmark) ->
      // saving a bookmark that didn't change would start another sync, and if writing fails, another after that
      if (mine.bookmarks[id]?.copy(changedAt = bookmark.changedAt) == bookmark) return@forEach
      val chapter = book.chapters.getOrNull(bookmark.file) ?: return@forEach
      bookmarkRepo.addBookmark(
        Bookmark(
          bookId = book.id,
          chapterId = chapter.id,
          title = bookmark.title,
          time = bookmark.time.coerceIn(0, chapter.duration),
          addedAt = Instant.ofEpochMilli(bookmark.addedAt),
          id = Bookmark.Id(id),
          kind = bookmark.kind,
        ),
      )
    }
  }

  internal suspend fun takeOver(
    bookId: BookId,
    remote: SyncedPlace,
  ) {
    val book = bookRepository.get(bookId) ?: return
    val position = book.positionFrom(remote) ?: return
    val isCurrent = currentBookStore.data.first() == bookId
    if (isCurrent && playStateManager.playState == PlayStateManager.PlayState.Playing) {
      // what plays here is newer than anything another device wrote
      return
    }
    Logger.d("Taking over a position from another device (current=$isCurrent)")
    // the player fails on a speed of 0 or less, and would again every time it loads the book
    val speed = remote.speed.takeIf { it > 0 } ?: book.content.playbackSpeed
    if (isCurrent && playerController.livePlaybackState(bookId) != null) {
      playerController.setPosition(
        time = position.positionInChapter,
        id = position.chapterId,
        source = ListeningEvent.Source.OtherDevice,
      )
      playerController.setSpeed(speed)
    }
    bookRepository.updateBook(bookId) {
      it.copy(
        currentChapter = position.chapterId,
        positionInChapter = position.positionInChapter,
        lastPlayedAt = position.playedAt,
        playbackSpeed = speed,
      )
    }
  }

  private inline fun <T> inFolder(action: () -> T): T? = try {
    action()
  } catch (e: IOException) {
    Logger.w(e, "Could not sync through a folder")
    null
  } catch (e: SecurityException) {
    // the listener took away the access to the folder
    Logger.w(e, "Could not sync through a folder")
    null
  } catch (e: IllegalArgumentException) {
    // how the storage provider reports a file or storage that went away since the last query
    Logger.w(e, "Could not sync through a folder")
    null
  }

  // not backed up, so a phone restored from a backup counts as another device
  private fun deviceId(): String {
    val file = File(context.noBackupFilesDir, "folderSyncDeviceId")
    return file.takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }
      ?: UUID.randomUUID().toString().also { file.writeText(it) }
  }
}

private fun Map<FolderType, List<DocumentFileWithUri>>.uris(): List<Uri> = values.flatten().map { it.uri }
