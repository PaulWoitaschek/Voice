package voice.core.audiobookshelf

import android.content.Context
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import retrofit2.HttpException
import voice.core.analytics.api.Analytics
import voice.core.audiobookshelf.account.Account
import voice.core.audiobookshelf.account.AccountStore
import voice.core.audiobookshelf.api.AudiobookshelfApi
import voice.core.audiobookshelf.api.LoginRequest
import voice.core.audiobookshelf.download.AudiobookshelfDownloads
import voice.core.audiobookshelf.http.AudiobookshelfHttp
import voice.core.audiobookshelf.login.serverUrlCandidates
import voice.core.audiobookshelf.sync.AudiobookshelfSync
import voice.core.audiobookshelf.sync.ProgressSync
import voice.core.data.BookId
import voice.core.data.isRemote
import voice.core.data.repo.BookContentRepo
import voice.core.data.store.CurrentBookStore
import voice.core.logging.api.Logger
import voice.core.playback.PlayerController
import java.io.File
import java.io.IOException
import javax.net.ssl.SSLException

/**
 * The oldest server with the login that hands out refresh tokens.
 */
private val MINIMUM_SERVER_VERSION = listOf(2, 26, 0)

public data class AudiobookshelfConnection(
  val serverUrl: String,
  val username: String,
  val libraryIds: List<String>,
  val needsLogin: Boolean,
) {
  /** The server as people know it, without the scheme and the trailing slash. */
  val serverName: String
    get() {
      val url = serverUrl.toHttpUrlOrNull() ?: return serverUrl
      val port = if (url.port == if (url.isHttps) 443 else 80) "" else ":${url.port}"
      val path = url.encodedPath.trimEnd('/')
      return url.host + port + path
    }
}

public class AudiobookshelfServer internal constructor(
  internal val url: String,
  val version: String,
  val passwordLogin: Boolean,
)

public sealed interface FindServerResult {
  public data class Found(val server: AudiobookshelfServer) : FindServerResult
  public data object NotFound : FindServerResult
  public data object CertificateNotTrusted : FindServerResult
  public data object NotSetUp : FindServerResult
  public data class TooOld(val version: String) : FindServerResult
}

/**
 * A successful login that isn't stored yet, so the listener can still pick the libraries.
 */
public class PendingLogin internal constructor(internal val account: Account) {
  val serverName: String get() = account.toConnection().serverName
}

public sealed interface LoginResult {
  public data class Success(val login: PendingLogin) : LoginResult
  public data object WrongCredentials : LoginResult
  public data object Unreachable : LoginResult
  public data object Failed : LoginResult
}

public data class AudiobookshelfLibrary(
  val id: String,
  val name: String,
  val bookCount: Int,
)

public data class LibrariesPreview(
  val libraries: List<AudiobookshelfLibrary>,
  val covers: List<File>,
)

@SingleIn(AppScope::class)
@Inject
public class Audiobookshelf internal constructor(
  @AccountStore
  private val accountStore: DataStore<Account?>,
  private val http: AudiobookshelfHttp,
  private val sync: AudiobookshelfSync,
  private val progressSync: ProgressSync,
  private val downloads: AudiobookshelfDownloads,
  private val contentRepo: BookContentRepo,
  @CurrentBookStore
  private val currentBookStore: DataStore<BookId?>,
  private val playerController: PlayerController,
  private val context: Context,
  private val analytics: Analytics,
) {

  public val connection: Flow<AudiobookshelfConnection?> = accountStore.data
    .map { it?.toConnection() }
    .distinctUntilChanged()

  public suspend fun isConnected(): Boolean = accountStore.data.first() != null

  public suspend fun findServer(address: String): FindServerResult {
    return probe(address).also { result ->
      analytics.event(
        "audiobookshelf_find_server",
        mapOf(
          "result" to when (result) {
            is FindServerResult.Found -> "found"
            FindServerResult.NotFound -> "not_found"
            FindServerResult.CertificateNotTrusted -> "certificate"
            FindServerResult.NotSetUp -> "not_set_up"
            is FindServerResult.TooOld -> "too_old"
          },
        ),
      )
    }
  }

  private suspend fun probe(address: String): FindServerResult = withContext(Dispatchers.IO) {
    var certificateNotTrusted = false
    // a host that can't be reached won't answer on another path either, which saves waiting for every timeout
    val unreachableOrigins = mutableSetOf<String>()
    for (url in serverUrlCandidates(address)) {
      val origin = url.toHttpUrlOrNull()?.let { "${it.scheme}://${it.host}:${it.port}" } ?: continue
      if (origin in unreachableOrigins) continue
      val status = try {
        http.api(url).status()
      } catch (e: SSLException) {
        Logger.d("$url: $e")
        certificateNotTrusted = true
        unreachableOrigins += origin
        continue
      } catch (e: IOException) {
        Logger.d("$url: $e")
        unreachableOrigins += origin
        continue
      } catch (e: HttpException) {
        Logger.d("$url: $e")
        continue
      } catch (e: SerializationException) {
        Logger.d("$url: $e")
        continue
      }
      if (status.app != "audiobookshelf") continue
      if (!status.isInit) return@withContext FindServerResult.NotSetUp
      val version = status.serverVersion.orEmpty()
      if (!version.isAtLeast(MINIMUM_SERVER_VERSION)) return@withContext FindServerResult.TooOld(version)
      return@withContext FindServerResult.Found(
        AudiobookshelfServer(
          url = url,
          version = version,
          passwordLogin = "local" in status.authMethods,
        ),
      )
    }
    if (certificateNotTrusted) FindServerResult.CertificateNotTrusted else FindServerResult.NotFound
  }

  public suspend fun login(
    server: AudiobookshelfServer,
    username: String,
    password: String,
  ): LoginResult {
    return signIn(server, username, password).also { result ->
      analytics.event(
        "audiobookshelf_login",
        mapOf(
          "result" to when (result) {
            is LoginResult.Success -> "success"
            LoginResult.WrongCredentials -> "wrong_credentials"
            LoginResult.Unreachable -> "unreachable"
            LoginResult.Failed -> "failed"
          },
        ),
      )
    }
  }

  private suspend fun signIn(
    server: AudiobookshelfServer,
    username: String,
    password: String,
  ): LoginResult {
    return try {
      val user = http.api(server.url).login(LoginRequest(username = username.trim(), password = password)).user
      val accessToken = user.accessToken
      val refreshToken = user.refreshToken
      if (accessToken == null || refreshToken == null) return LoginResult.Failed
      LoginResult.Success(
        PendingLogin(
          Account(
            serverUrl = server.url,
            userId = user.id,
            username = user.username,
            accessToken = accessToken,
            refreshToken = refreshToken,
            libraryIds = emptyList(),
            serverVersion = server.version,
          ),
        ),
      )
    } catch (e: HttpException) {
      if (e.code() == 401) LoginResult.WrongCredentials else LoginResult.Failed
    } catch (e: IOException) {
      Logger.d("Could not log in: $e")
      LoginResult.Unreachable
    } catch (e: SerializationException) {
      // something in front of the server, like a proxy with its own login page, answered instead
      Logger.w(e, "Could not log in")
      LoginResult.Failed
    }
  }

  public suspend fun preview(login: PendingLogin): LibrariesPreview? {
    return preview(login.account, http.withToken(login.account.serverUrl, login.account.accessToken), stored = false)
  }

  public suspend fun libraries(): LibrariesPreview? {
    val account = accountStore.data.first() ?: return null
    return preview(account, http.authenticatedApi(account.serverUrl), stored = true)
  }

  private suspend fun preview(
    account: Account,
    api: AudiobookshelfApi,
    stored: Boolean,
  ): LibrariesPreview? = withContext(Dispatchers.IO) {
    try {
      val libraries = api.libraries().libraries.filter { it.mediaType == "book" }
      val firstItems = libraries.associate { library ->
        library.id to api.libraryItems(libraryId = library.id, limit = 8, page = 0)
      }
      val covers = firstItems.values
        .flatMap { it.results }
        .filter { it.media.coverPath != null }
        .take(5)
        .mapNotNull { item -> downloadPreviewCover(account, item.id, stored) }
      LibrariesPreview(
        libraries = libraries.map { library ->
          AudiobookshelfLibrary(
            id = library.id,
            name = library.name,
            bookCount = firstItems[library.id]?.total ?: 0,
          )
        },
        covers = covers,
      )
    } catch (e: IOException) {
      Logger.d("Could not load the libraries: $e")
      null
    } catch (e: HttpException) {
      Logger.w(e, "Could not load the libraries")
      null
    } catch (e: SerializationException) {
      Logger.w(e, "Could not load the libraries")
      null
    }
  }

  private fun downloadPreviewCover(
    account: Account,
    itemId: String,
    stored: Boolean,
  ): File? {
    val folder = File(context.cacheDir, "audiobookshelfPreview").apply { mkdirs() }
    val file = File(folder, "$itemId.jpg")
    if (file.exists()) return file
    val url = "${account.serverUrl}api/items/$itemId/cover?width=400&format=jpeg"
    val request = if (stored) {
      Request.Builder().url(url).build()
    } else {
      Request.Builder().url(url).header("Authorization", "Bearer ${account.accessToken}").build()
    }
    val client = if (stored) http.authenticatedClient else http.client
    return try {
      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return null
        file.outputStream().use { output -> response.body.byteStream().copyTo(output) }
        file
      }
    } catch (e: IOException) {
      Logger.d("Could not load a cover: $e")
      null
    }
  }

  /**
   * Stores the login, which brings the books of [libraryIds] into the library.
   */
  public suspend fun connect(
    login: PendingLogin,
    libraryIds: List<String>,
  ) {
    analytics.event("audiobookshelf_connected", mapOf("libraries" to libraryIds.size.toString()))
    accountStore.updateData { login.account.copy(libraryIds = libraryIds) }
    sync.sync(force = true)
  }

  /**
   * Signs in again after the server declined the stored login.
   */
  public suspend fun renewLogin(login: PendingLogin) {
    accountStore.updateData { current ->
      login.account.copy(libraryIds = current?.libraryIds.orEmpty())
    }
    sync.sync(force = true)
  }

  public suspend fun setLibraries(libraryIds: List<String>) {
    accountStore.updateData { it?.copy(libraryIds = libraryIds) }
    sync.sync(force = true)
  }

  /**
   * Removes the server books and their downloads from Voice. Their progress lives on on the server.
   */
  public suspend fun signOut() {
    val account = accountStore.data.first() ?: return
    analytics.event("audiobookshelf_signed_out")
    // a sync that is still running would bring the books back
    sync.cancel()
    if (currentBookStore.data.first()?.isRemote == true) {
      playerController.pause()
      currentBookStore.updateData { null }
    }
    try {
      http.api(account.serverUrl).logout(account.refreshToken).let { response ->
        if (!response.isSuccessful) Logger.d("Logging out failed with ${response.code()}")
      }
    } catch (e: IOException) {
      Logger.d("Could not log out: $e")
    }
    accountStore.updateData { null }
    downloads.removeAll()
    val remoteBooks = contentRepo.all().filter { it.id.isRemote }
    remoteBooks.forEach { contentRepo.put(it.copy(isActive = false)) }
    progressSync.forget(remoteBooks.map { it.id }.toSet())
    sync.onSignedOut()
  }
}

private fun Account.toConnection() = AudiobookshelfConnection(
  serverUrl = serverUrl,
  username = username,
  libraryIds = libraryIds,
  needsLogin = needsLogin,
)

internal fun String.isAtLeast(minimum: List<Int>): Boolean {
  val parts = removePrefix("v").split('.', '-').mapNotNull { it.toIntOrNull() }
  minimum.forEachIndexed { index, required ->
    val part = parts.getOrNull(index) ?: 0
    if (part != required) return part > required
  }
  return true
}
