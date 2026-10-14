package voice.core.foldersync

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import voice.core.common.DispatcherProvider
import voice.core.data.BookId
import voice.core.data.ListeningEvent
import voice.core.data.folders.AudiobookFolders
import voice.core.data.folders.DocumentFileWithUri
import voice.core.data.folders.FolderType
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.data.store.LastPlaybackSpeedStore
import voice.core.logging.api.Logger
import voice.core.playback.PlayerController
import voice.core.playback.playstate.PlayStateManager
import voice.core.playback.withDefaultSpeed
import java.io.File
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/**
 * Takes the listener's place to their other devices without a server. Each device writes its places into a file in
 * the library folders, which the listener syncs with a tool of their own, and takes over newer places from the files
 * of the other devices.
 */
@SingleIn(AppScope::class)
@Inject
public class FolderSync internal constructor(
  private val context: Context,
  private val syncFolder: SyncFolder,
  private val audiobookFolders: AudiobookFolders,
  private val bookRepository: BookRepository,
  private val playerController: PlayerController,
  private val playStateManager: PlayStateManager,
  @CurrentBookStore
  private val currentBookStore: DataStore<BookId?>,
  @LastPlaybackSpeedStore
  private val lastPlaybackSpeedStore: DataStore<Float>,
  dispatcherProvider: DispatcherProvider,
) {

  private val scope = CoroutineScope(
    SupervisorJob() + dispatcherProvider.io + CoroutineExceptionHandler { _, throwable ->
      Logger.e(throwable, "Syncing through the library folders failed")
    },
  )
  private val readRequests = Channel<Unit>(Channel.CONFLATED)
  private var started = false

  private val ownFileName by lazy { "sync-${deviceId()}.json" }

  internal fun start() {
    if (started) return
    started = true
    scope.launch { writeChanges() }
    scope.launch { readOnRequest() }
  }

  internal fun requestRead() {
    readRequests.trySend(Unit)
  }

  @OptIn(FlowPreview::class)
  private suspend fun writeChanges() {
    val written = mutableMapOf<Uri, SyncFile>()
    // while playing, the position changes every few hundred ms, so this writes once playback stops
    combine(audiobookFolders.all(), bookRepository.flow(), ::Pair)
      .debounce(3.seconds)
      .collect { (folders, books) ->
        folders.uris().forEach { folder ->
          val file = books.keyedIn(folder).toSyncFile()
          // a folder without a book that was played here gets no .voice folder
          if (file.books.isEmpty() || written[folder] == file) return@forEach
          inFolder(folder) { syncFolder.write(folder, ownFileName, file) }
            ?.let { written[folder] = file }
        }
      }
  }

  @OptIn(FlowPreview::class)
  private suspend fun readOnRequest() {
    // a book started on another device may only show up here with a scan
    val booksChanged = bookRepository.flow()
      .map { books -> books.map { it.id }.toSet() }
      .distinctUntilChanged()
      .debounce(2.seconds)
      .map {}
    merge(readRequests.receiveAsFlow(), booksChanged)
      .conflate()
      .collect {
        // a read that fails would otherwise end all reads until the app restarts
        try {
          read()
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Logger.e(e, "Reading the places from the library folders failed")
        }
      }
  }

  internal suspend fun read() {
    val books = bookRepository.all()
    val keyedByFolder = audiobookFolders.all().first().uris()
      .associateWith { books.keyedIn(it) }
      .filterValues { it.isNotEmpty() }
    val remote = keyedByFolder.keys
      .flatMap { folder -> inFolder(folder) { syncFolder.read(folder, exceptName = ownFileName) }.orEmpty() }
      .newest()
    keyedByFolder.values.flatten().forEach { (key, book) ->
      remote[key]?.let { takeOver(book.id, it) }
    }
  }

  internal suspend fun takeOver(
    bookId: BookId,
    remote: SyncedPosition,
  ) {
    val book = bookRepository.get(bookId) ?: return
    val position = book.positionFrom(remote) ?: return
    val isCurrent = currentBookStore.data.first() == bookId
    if (isCurrent && playStateManager.playState == PlayStateManager.PlayState.Playing) {
      // what plays here is newer than anything another device wrote
      return
    }
    Logger.d("Taking over the position of ${book.content.name} from another device")
    if (isCurrent && playerController.livePlaybackState(bookId) != null) {
      playerController.setPosition(
        time = position.positionInChapter,
        id = position.chapterId,
        source = ListeningEvent.Source.OtherDevice,
      )
    }
    // the book now counts as played, so it would no longer start at the speed the listener last picked
    val speed = book.withDefaultSpeed { lastPlaybackSpeedStore.data.first() }.content.playbackSpeed
    bookRepository.updateBook(bookId) {
      it.copy(
        currentChapter = position.chapterId,
        positionInChapter = position.positionInChapter,
        lastPlayedAt = position.playedAt,
        playbackSpeed = speed,
      )
    }
  }

  private inline fun <T> inFolder(
    folder: Uri,
    action: () -> T,
  ): T? = try {
    action()
  } catch (e: Exception) {
    Logger.w(e, "Could not sync through $folder")
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
