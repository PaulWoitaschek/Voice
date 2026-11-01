package voice.core.audiobookshelf.login

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import voice.core.analytics.api.Analytics
import voice.core.audiobookshelf.AudiobookshelfServer
import voice.core.audiobookshelf.FindServerResult
import voice.core.audiobookshelf.LibrariesPreview
import voice.core.audiobookshelf.LoginResult
import voice.core.audiobookshelf.PendingLogin
import voice.core.audiobookshelf.ServerAccount
import voice.core.audiobookshelf.ServerLogin
import voice.core.audiobookshelf.account.Account
import voice.core.audiobookshelf.account.AccountStore
import voice.core.audiobookshelf.api.LoginRequest
import voice.core.audiobookshelf.http.AudiobookshelfHttp
import voice.core.audiobookshelf.sync.AudiobookshelfSync
import voice.core.logging.api.Logger

@ContributesBinding(AppScope::class)
@Inject
class AudiobookshelfLogin
internal constructor(
  private val serverFinder: ServerFinder,
  private val previews: LibrariesPreviews,
  private val serverAccount: ServerAccount,
  private val accountStore: AccountStore,
  private val http: AudiobookshelfHttp,
  private val sync: AudiobookshelfSync,
  private val analytics: Analytics,
) : ServerLogin {

  override suspend fun findServer(address: String): FindServerResult {
    return serverFinder.find(address).also { result ->
      analytics.event(
        "audiobookshelf_find_server",
        mapOf(
          "result" to
            when (result) {
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

  override suspend fun login(
    server: AudiobookshelfServer,
    username: String,
    password: String,
  ): LoginResult {
    return signIn(server, username, password).also { result ->
      analytics.event(
        "audiobookshelf_login",
        mapOf(
          "result" to
            when (result) {
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
        AccountLogin(
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
    } catch (_: IOException) {
      LoginResult.Unreachable
    } catch (e: SerializationException) {
      // something in front of the server, like a proxy with its own login page, answered instead
      Logger.w(e, "Could not log in")
      LoginResult.Failed
    }
  }

  override suspend fun preview(login: PendingLogin): LibrariesPreview? = previews.load(login.account, stored = false)

  override suspend fun connect(
    login: PendingLogin,
    libraryIds: List<String>,
  ) {
    analytics.event("audiobookshelf_connected", mapOf("libraries" to libraryIds.size.toString()))
    val current = accountStore.data.first()
    if (current != null && current.userId != login.account.userId) serverAccount.signOut()
    accountStore.updateData { login.account.copy(libraryIds = libraryIds) }
    sync.sync(force = true)
  }

  override suspend fun renewLogin(login: PendingLogin): Boolean {
    val current = accountStore.data.first()
    if (current != null && current.userId != login.account.userId) return false
    accountStore.updateData { stored ->
      login.account.copy(libraryIds = stored?.libraryIds.orEmpty())
    }
    sync.sync(force = true)
    return true
  }
}

internal class AccountLogin(val account: Account) : PendingLogin {
  override val serverName: String
    get() = serverName(account.serverUrl)
}

private val PendingLogin.account: Account
  get() = (this as AccountLogin).account
