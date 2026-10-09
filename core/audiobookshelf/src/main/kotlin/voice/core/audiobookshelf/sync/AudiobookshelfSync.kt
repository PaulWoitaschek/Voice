package voice.core.audiobookshelf.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.SystemClock
import androidx.core.content.getSystemService
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import voice.core.audiobookshelf.account.Account
import voice.core.audiobookshelf.account.AccountStore
import voice.core.audiobookshelf.download.AudiobookshelfDownloads
import voice.core.audiobookshelf.http.AudiobookshelfHttp
import voice.core.data.BookId
import voice.core.data.isRemote
import voice.core.data.repo.BookContentRepo
import voice.core.data.repo.BookRepository
import voice.core.logging.api.Logger
import voice.core.playback.playstate.PlayStateManager
import java.io.IOException
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val MIN_SYNC_INTERVAL = 1.minutes

/**
 * Whether the server can be reached right now, as far as Voice knows.
 */
public enum class ServerReachability {
  Unknown,
  Reachable,
  Unreachable,
}

/**
 * Runs the syncs with the server: the whole library when the app starts, when the library opens and when the
 * device gets back online, and the position of a book shortly after it changed here.
 */
@SingleIn(AppScope::class)
@Inject
public class AudiobookshelfSync internal constructor(
  @AccountStore
  private val accountStore: DataStore<Account?>,
  private val http: AudiobookshelfHttp,
  private val librarySync: LibrarySync,
  private val progressSync: ProgressSync,
  private val bookmarkSync: BookmarkSync,
  private val downloads: AudiobookshelfDownloads,
  private val listeningSessionReporter: ListeningSessionReporter,
  private val bookRepository: BookRepository,
  private val contentRepo: BookContentRepo,
  private val playStateManager: PlayStateManager,
  private val context: Context,
) {

  // a sync must never take the app down, whatever the server answers
  private val scope = CoroutineScope(
    SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
      Logger.w(throwable, "Syncing with the server failed")
    },
  )
  private val mutex = Mutex()
  private var syncJob: Job? = null
  private var started = false
  private var lastSyncAt: Long? = null

  @Volatile
  private var resyncRequested = false
  private var validatedNetwork: Network? = null

  public val syncing: StateFlow<Boolean>
    field = MutableStateFlow(false)

  public val reachability: StateFlow<ServerReachability>
    field = MutableStateFlow(ServerReachability.Unknown)

  internal fun start() {
    if (started) return
    started = true
    scope.launch { hideBooksWithoutAccount() }
    listeningSessionReporter.start(scope)
    pushLocalChanges()
    syncWhenOnline()
    sync(force = true)
  }

  /**
   * Syncs the library with the server, unless a sync is already running. Without [force], a sync that finished
   * less than a minute ago is enough.
   */
  // the network callback calls this from another thread
  @Synchronized
  public fun sync(force: Boolean = false) {
    if (syncJob?.isActive == true) {
      // the running sync may have read the account before it changed
      if (force) resyncRequested = true
      return
    }
    val last = lastSyncAt
    if (!force && last != null && SystemClock.elapsedRealtime() - last < MIN_SYNC_INTERVAL.inWholeMilliseconds) return
    syncJob = scope.launch {
      mutex.withLock {
        syncing.value = true
        try {
          do {
            resyncRequested = false
            val account = accountStore.data.first()?.takeUnless { it.needsLogin } ?: break
            syncNow(account)
          } while (resyncRequested)
        } finally {
          syncing.value = false
        }
      }
    }
  }

  internal suspend fun cancel() {
    resyncRequested = false
    syncJob?.cancelAndJoin()
  }

  // the books of a server stay hidden while signed out, for example after a restore from a backup without the login
  private suspend fun hideBooksWithoutAccount() {
    if (accountStore.data.first() != null) return
    contentRepo.all()
      .filter { it.isActive && it.id.isRemote }
      .forEach { contentRepo.put(it.copy(isActive = false)) }
  }

  private suspend fun syncNow(account: Account) {
    try {
      val api = http.authenticatedApi(account.serverUrl)
      @Suppress("RETURN_VALUE_NOT_USED")
      api.me()
      reachability.value = ServerReachability.Reachable
      syncLibrary(account)
      // the library sync can take a while, the progress fetched after it is as fresh as it gets
      val user = api.me()
      val books = bookRepository.all()
      progressSync.syncAll(account, user.mediaProgress)
      bookmarkSync.sync(account, user.bookmarks, books)
      lastSyncAt = SystemClock.elapsedRealtime()
    } catch (e: IOException) {
      Logger.d("Could not sync with the server: $e")
      reachability.value = ServerReachability.Unreachable
    } catch (e: HttpException) {
      Logger.w(e, "Could not sync with the server")
    } catch (e: SerializationException) {
      Logger.w(e, "The server answered in an unexpected way")
    }
  }

  // a book that moved since Voice started, like after a seek. What moved before goes through the sync, which first
  // checks whether another device listened since.
  private fun pushLocalChanges() {
    scope.launch {
      var previous: Map<BookId, Long>? = null
      bookRepository.flow()
        .map { books ->
          books.filter { it.id.isRemote }.associate { it.id to it.position }
        }
        .distinctUntilChanged()
        // while playing the position changes all the time and the listening session sends it
        .debounce(3.seconds)
        .collect { positions ->
          val before = previous
          previous = positions
          if (before == null) return@collect
          if (playStateManager.playState == PlayStateManager.PlayState.Playing) return@collect
          val account = accountStore.data.first()?.takeUnless { it.needsLogin } ?: return@collect
          positions
            .filter { (bookId, position) -> bookId in before && before[bookId] != position }
            .keys
            .forEach { bookId ->
              if (!progressSync.push(account, bookId)) return@collect
            }
        }
    }
  }

  private suspend fun syncLibrary(account: Account) {
    try {
      librarySync.sync(account)
      downloads.reconcile()
    } catch (e: HttpException) {
      // the progress and the bookmarks still go both ways
      Logger.w(e, "Could not sync the library")
    } catch (e: SerializationException) {
      Logger.w(e, "The server answered in an unexpected way")
    }
  }

  private fun syncWhenOnline() {
    val connectivityManager = context.getSystemService<ConnectivityManager>() ?: return
    connectivityManager.registerDefaultNetworkCallback(
      object : ConnectivityManager.NetworkCallback() {
        // capabilities change all the time, only a network that just got online is worth a sync
        override fun onCapabilitiesChanged(
          network: Network,
          networkCapabilities: NetworkCapabilities,
        ) {
          val validated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
          if (validated && network != validatedNetwork) {
            validatedNetwork = network
            sync(force = true)
          } else if (!validated && network == validatedNetwork) {
            validatedNetwork = null
          }
        }

        override fun onLost(network: Network) {
          if (network == validatedNetwork) validatedNetwork = null
          reachability.value = ServerReachability.Unreachable
        }
      },
    )
  }

  internal fun onSignedOut() {
    reachability.value = ServerReachability.Unknown
  }
}
