package voice.core.audiobookshelf.sync

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.IOException
import kotlin.math.abs
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import voice.core.audiobookshelf.account.Account
import voice.core.audiobookshelf.account.SyncedProgress
import voice.core.audiobookshelf.account.SyncedProgressStore
import voice.core.audiobookshelf.api.AbsMediaProgress
import voice.core.audiobookshelf.api.ProgressUpdate
import voice.core.audiobookshelf.http.AudiobookshelfHttp
import voice.core.audiobookshelf.itemId
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.ListeningEvent
import voice.core.data.isRemote
import voice.core.data.repo.BookRepository
import voice.core.logging.api.Logger
import voice.core.playback.PlayerController
import voice.core.playback.playstate.PlayStateManager

/**
 * Keeps the position of server books in step with the server. The newest position wins: what was
 * played on this device goes up, what was played on another device comes down. A position that
 * couldn't be sent stays different from the [SyncedProgress] and goes up with the next sync.
 */
@SingleIn(AppScope::class)
@Inject
internal class ProgressSync(
  private val http: AudiobookshelfHttp,
  private val bookRepository: BookRepository,
  private val syncedProgressStore: SyncedProgressStore,
  private val playStateManager: PlayStateManager,
  private val playerController: PlayerController,
) {

  private val mutex = Mutex()

  suspend fun syncAll(
    account: Account,
    serverProgress: List<AbsMediaProgress>,
  ) = mutex.withLock {
    val progressByItem =
      serverProgress
        .filter { it.episodeId == null && it.libraryItemId != null }
        .associateBy { it.libraryItemId!! }
    val syncedProgress = syncedProgressStore.data.first()
    val changes = Changes()
    bookRepository
      .all()
      .filter { it.id.isRemote }
      .forEach { stale ->
        val itemId = stale.id.itemId ?: return@forEach
        // read right before deciding, as sending the books before can take a while
        val book = bookRepository.get(stale.id) ?: return@forEach
        sync(account, book, syncedProgress[book.id.value], progressByItem[itemId], changes)
      }
    // one write each, as each write of a book updates the whole library
    bookRepository.updateBooks(changes.positions.keys) { changes.positions.getValue(it.id)(it) }
    if (changes.synced.isNotEmpty()) syncedProgressStore.updateData { it + changes.synced }
  }

  /**
   * Sends the position of [bookId] if it moved since it was last synced. Returns whether the server
   * is up to date.
   */
  suspend fun push(
    account: Account,
    bookId: BookId,
  ): Boolean = mutex.withLock {
    val book = bookRepository.get(bookId) ?: return@withLock true
    val synced = syncedProgressStore.data.first()[bookId.value]
    if (!book.movedAwayFrom(synced) || book.id.itemId == null) return@withLock true
    val sent = send(account, book, serverLastUpdate = synced?.serverLastUpdate ?: 0)
    if (sent != null) remember(bookId, sent)
    sent != null
  }

  /**
   * Remembers that the server got the position of [book] in another way, like a listening session.
   */
  suspend fun markSent(book: Book) = mutex.withLock {
    val previous = syncedProgressStore.data.first()[book.id.value]
    remember(book.id, book.syncedProgress(serverLastUpdate = previous?.serverLastUpdate ?: 0))
  }

  private suspend fun sync(
    account: Account,
    book: Book,
    synced: SyncedProgress?,
    server: AbsMediaProgress?,
    changes: Changes,
  ) {
    val localChanged = book.movedAwayFrom(synced)
    val serverProgress = server?.let(book::progressOnServer)
    // what Voice sent itself comes back with a newer time, which isn't a change of another device
    val serverChanged =
      server != null &&
        serverProgress != null &&
        server.lastUpdate > (synced?.serverLastUpdate ?: -1) &&
        (synced == null || serverProgress.differsFrom(synced))
    if (
      server != null &&
        synced != null &&
        !serverChanged &&
        server.lastUpdate > synced.serverLastUpdate
    ) {
      changes.synced[book.id.value] = synced.copy(serverLastUpdate = server.lastUpdate)
    }

    when {
      serverChanged && !localChanged -> applyServer(book, server, changes)
      serverChanged && localChanged -> {
        if (
          isPlaying(book.id) || book.content.lastPlayedAt.toEpochMilliseconds() > server.lastUpdate
        ) {
          send(account, book, server.lastUpdate)?.let { changes.synced[book.id.value] = it }
        } else {
          applyServer(book, server, changes)
        }
      }
      localChanged -> {
        send(account, book, synced?.serverLastUpdate ?: 0)?.let {
          changes.synced[book.id.value] = it
        }
      }
    }
  }

  private suspend fun applyServer(
    book: Book,
    server: AbsMediaProgress,
    changes: Changes,
  ) {
    val serverProgress = book.progressOnServer(server)
    if (isPlaying(book.id)) {
      // what plays here is newer than anything on the server
      return
    }
    if (book.movedAwayFrom(serverProgress)) {
      val position = book.positionAt(serverProgress.positionMs)
      Logger.d("Taking over the position ${serverProgress.positionMs} from the server")
      val live = playerController.livePlaybackState(book.id)
      if (live != null) {
        playerController.setPosition(
          time = position.positionInChapter,
          id = position.chapterId,
          source = ListeningEvent.Source.OtherDevice,
        )
      }
      changes.positions[book.id] = { content ->
        // a pause or a seek since then is newer
        if (
          content.currentChapter != book.content.currentChapter ||
            content.positionInChapter != book.content.positionInChapter
        ) {
          content
        } else {
          content.copy(
            currentChapter = position.chapterId,
            positionInChapter = position.positionInChapter,
            lastPlayedAt =
              maxOf(content.lastPlayedAt, Instant.fromEpochMilliseconds(server.lastUpdate)),
          )
        }
      }
    }
    changes.synced[book.id.value] = serverProgress
  }

  private suspend fun send(
    account: Account,
    book: Book,
    serverLastUpdate: Long,
  ): SyncedProgress? {
    val itemId = book.id.itemId ?: return null
    val durationSeconds = book.duration / 1000.0
    val finished = book.isFinished
    val update =
      ProgressUpdate(
        currentTime = book.position / 1000.0,
        duration = durationSeconds,
        progress =
          if (finished) 1.0 else (book.position.toDouble() / book.duration).coerceIn(0.0, 1.0),
        isFinished =
          when {
            finished -> true
            // told that a finished book isn't, the server starts it over, which is right for a book
            // at its start
            book.position < BOOK_START_MS -> false
            else -> null
          },
      )
    return try {
      val response = http.authenticatedApi(account.serverUrl).updateProgress(itemId, update)
      if (response.isSuccessful) {
        book.syncedProgress(serverLastUpdate)
      } else {
        Logger.w("Could not send the progress: HTTP ${response.code()}")
        null
      }
    } catch (_: IOException) {
      null
    }
  }

  private fun isPlaying(bookId: BookId): Boolean {
    return playStateManager.playState == PlayStateManager.PlayState.Playing(bookId)
  }

  private suspend fun remember(
    bookId: BookId,
    progress: SyncedProgress,
  ) {
    syncedProgressStore.updateData { it + (bookId.value to progress) }
  }

  suspend fun forget(bookIds: Set<BookId>) {
    val keys = bookIds.map { it.value }.toSet()
    syncedProgressStore.updateData { it - keys }
  }
}

private fun Book.syncedProgress(serverLastUpdate: Long) =
  SyncedProgress(
    positionMs = position,
    finished = isFinished,
    serverLastUpdate = serverLastUpdate,
  )

private fun Book.progressOnServer(server: AbsMediaProgress): SyncedProgress {
  val serverPosition = (server.currentTime * 1000).toLong().coerceIn(0, duration)
  return SyncedProgress(
    // a book finished elsewhere ends up at its end here, unless it is already in its last seconds
    positionMs =
      if (server.isFinished && duration - serverPosition >= FINISHED_REMAINING_MS) {
        duration
      } else {
        serverPosition
      },
    finished = server.isFinished,
    serverLastUpdate = server.lastUpdate,
  )
}

/** Small differences come from rounding the position to the seconds the server stores. */
private fun Book.movedAwayFrom(progress: SyncedProgress?): Boolean {
  if (progress == null) return position > 0
  return isFinished != progress.finished ||
    abs(position - progress.positionMs) > POSITION_TOLERANCE_MS
}

private fun SyncedProgress.differsFrom(other: SyncedProgress): Boolean {
  return finished != other.finished || abs(positionMs - other.positionMs) > POSITION_TOLERANCE_MS
}

private const val POSITION_TOLERANCE_MS = 1_500

private const val BOOK_START_MS = 1_000

private class Changes {
  val synced = mutableMapOf<String, SyncedProgress>()
  val positions = mutableMapOf<BookId, (BookContent) -> BookContent>()
}
