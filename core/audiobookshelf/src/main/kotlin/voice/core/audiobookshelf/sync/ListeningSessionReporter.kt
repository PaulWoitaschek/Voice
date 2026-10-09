package voice.core.audiobookshelf.sync

import android.os.Build
import android.os.SystemClock
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import voice.core.audiobookshelf.account.Account
import voice.core.audiobookshelf.account.AccountStore
import voice.core.audiobookshelf.account.Preferences
import voice.core.audiobookshelf.account.PreferencesStore
import voice.core.audiobookshelf.api.DeviceInfo
import voice.core.audiobookshelf.api.SessionSyncRequest
import voice.core.audiobookshelf.api.StartSessionRequest
import voice.core.audiobookshelf.http.AudiobookshelfHttp
import voice.core.audiobookshelf.itemId
import voice.core.common.AppInfoProvider
import voice.core.data.BookId
import voice.core.data.isRemote
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.logging.api.Logger
import voice.core.playback.playstate.PlayStateManager
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

private val REPORT_INTERVAL = 30.seconds
private val CLOSE_AFTER_PAUSE = 10.minutes

/**
 * Tells the server what is played, the way its own apps do: a listening session per stretch of listening, synced
 * while playing. That keeps the progress current on the other devices and fills the listening stats of the
 * server. Without a connection the progress is sent later by the [ProgressSync].
 */
@SingleIn(AppScope::class)
@Inject
internal class ListeningSessionReporter(
  private val http: AudiobookshelfHttp,
  @AccountStore
  private val accountStore: DataStore<Account?>,
  @PreferencesStore
  private val preferencesStore: DataStore<Preferences>,
  @CurrentBookStore
  private val currentBookStore: DataStore<BookId?>,
  private val bookRepository: BookRepository,
  private val playStateManager: PlayStateManager,
  private val progressSync: ProgressSync,
  private val appInfoProvider: AppInfoProvider,
) {

  private data class Session(
    val id: String,
    val bookId: BookId,
  )

  private val mutex = Mutex()
  private var session: Session? = null

  fun start(scope: CoroutineScope) {
    scope.launch {
      combine(playStateManager.playStateFlow, currentBookStore.data) { playState, bookId -> playState to bookId }
        .distinctUntilChanged()
        .collectLatest { (playState, bookId) ->
          if (bookId == null || !bookId.isRemote) {
            close()
            return@collectLatest
          }
          if (session?.bookId != bookId) close()

          if (playState == PlayStateManager.PlayState.Playing) {
            var reportedAt = SystemClock.elapsedRealtime()
            try {
              while (true) {
                delay(REPORT_INTERVAL)
                val now = SystemClock.elapsedRealtime()
                report(bookId, listenedMs = now - reportedAt)
                reportedAt = now
              }
            } finally {
              withContext(NonCancellable) {
                val listenedMs = SystemClock.elapsedRealtime() - reportedAt
                // the position of the pause is written a moment after the pause itself
                delay(500.milliseconds)
                report(bookId, listenedMs)
              }
            }
          } else {
            delay(CLOSE_AFTER_PAUSE)
            close()
          }
        }
    }
  }

  private suspend fun report(
    bookId: BookId,
    listenedMs: Long,
  ) = mutex.withLock {
    val account = accountStore.data.first()?.takeUnless { it.needsLogin } ?: return@withLock
    val book = bookRepository.get(bookId) ?: return@withLock
    val itemId = bookId.itemId ?: return@withLock
    val api = http.authenticatedApi(account.serverUrl)
    val request = SessionSyncRequest(
      currentTime = book.position / 1000.0,
      timeListened = listenedMs / 1000.0,
      duration = book.duration / 1000.0,
    )
    try {
      val sessionId = session?.takeIf { it.bookId == bookId }?.id
        ?: api.startSession(itemId, startSessionRequest()).id.also { session = Session(it, bookId) }
      val response = api.syncSession(sessionId, request)
      if (response.isSuccessful) {
        progressSync.markSent(book)
        return@withLock
      }
      // the server forgets sessions when it restarts
      if (response.code() == 404) session = null
      Logger.w("Could not sync the listening session: ${response.code()}")
    } catch (e: IOException) {
      Logger.d("Could not sync the listening session: $e")
    } catch (e: HttpException) {
      Logger.w(e, "Could not start a listening session")
    } catch (e: SerializationException) {
      Logger.w(e, "Could not start a listening session")
    }
    @Suppress("RETURN_VALUE_NOT_USED")
    progressSync.push(account, bookId)
  }

  private suspend fun close() = mutex.withLock {
    val closing = session ?: return@withLock
    session = null
    val account = accountStore.data.first() ?: return@withLock
    val book = bookRepository.get(closing.bookId) ?: return@withLock
    try {
      val response = http.authenticatedApi(account.serverUrl).closeSession(
        closing.id,
        SessionSyncRequest(
          currentTime = book.position / 1000.0,
          timeListened = 0.0,
          duration = book.duration / 1000.0,
        ),
      )
      if (!response.isSuccessful) Logger.d("Could not close the listening session: ${response.code()}")
    } catch (e: IOException) {
      Logger.d("Could not close the listening session: $e")
    }
  }

  private suspend fun startSessionRequest(): StartSessionRequest {
    return StartSessionRequest(
      deviceInfo = DeviceInfo(
        clientName = "Voice",
        clientVersion = appInfoProvider.versionName,
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        sdkVersion = Build.VERSION.SDK_INT,
        deviceId = deviceId(),
      ),
      mediaPlayer = "Voice",
      forceDirectPlay = true,
      forceTranscode = false,
      supportedMimeTypes = SUPPORTED_MIME_TYPES,
    )
  }

  private suspend fun deviceId(): String {
    val stored = preferencesStore.data.first().deviceId
    if (stored != null) return stored
    return preferencesStore.updateData { preferences ->
      if (preferences.deviceId != null) preferences else preferences.copy(deviceId = Uuid.random().toString())
    }.deviceId!!
  }
}

private val SUPPORTED_MIME_TYPES = listOf(
  "audio/flac",
  "audio/mpeg",
  "audio/mp4",
  "audio/ogg",
  "audio/aac",
  "audio/webm",
  "audio/x-matroska",
)
