package voice.core.audiobookshelf.http

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import voice.core.audiobookshelf.account.Account
import voice.core.audiobookshelf.account.AccountStore
import voice.core.audiobookshelf.api.AudiobookshelfApi
import voice.core.audiobookshelf.audiobookshelfJson
import voice.core.common.AppInfoProvider
import voice.core.logging.api.Logger
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Talks to the server of the signed in account. Requests to it carry the access token, and an expired token is
 * swapped for a fresh one on the fly, so neither the api calls nor the player have to care.
 */
@SingleIn(AppScope::class)
@Inject
internal class AudiobookshelfHttp(
  @AccountStore
  private val accountStore: DataStore<Account?>,
  appInfoProvider: AppInfoProvider,
) {

  private val userAgent = "Voice/${appInfoProvider.versionName}"

  val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .addInterceptor { chain ->
      chain.proceed(
        chain.request().newBuilder()
          .header("User-Agent", userAgent)
          .build(),
      )
    }
    .build()

  val authenticatedClient: OkHttpClient = client.newBuilder()
    .addInterceptor(AccessTokenInterceptor())
    .authenticator(TokenRefresher())
    .build()

  private val apis = ConcurrentHashMap<String, AudiobookshelfApi>()
  private val authenticatedApis = ConcurrentHashMap<String, AudiobookshelfApi>()

  fun api(serverUrl: String): AudiobookshelfApi = apis.getOrPut(serverUrl) { retrofit(serverUrl, client) }

  fun authenticatedApi(serverUrl: String): AudiobookshelfApi {
    return authenticatedApis.getOrPut(serverUrl) { retrofit(serverUrl, authenticatedClient) }
  }

  /**
   * For a login that isn't stored yet.
   */
  fun withToken(
    serverUrl: String,
    token: String,
  ): AudiobookshelfApi {
    val tokenClient = client.newBuilder()
      .addInterceptor { chain -> chain.proceed(chain.request().withToken(token)) }
      .build()
    return retrofit(serverUrl, tokenClient)
  }

  private fun retrofit(
    serverUrl: String,
    client: OkHttpClient,
  ): AudiobookshelfApi {
    return Retrofit.Builder()
      .baseUrl(serverUrl)
      .client(client)
      .addConverterFactory(audiobookshelfJson.asConverterFactory("application/json".toMediaType()))
      .build()
      .create()
  }

  private fun currentAccount(): Account? = runBlocking { accountStore.data.first() }

  private inner class AccessTokenInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
      val request = chain.request()
      val account = currentAccount()
      if (account == null || !request.url.toString().startsWith(account.serverUrl)) {
        return chain.proceed(request)
      }
      return chain.proceed(request.withToken(account.accessToken))
    }
  }

  private inner class TokenRefresher : Authenticator {

    override fun authenticate(
      route: Route?,
      response: Response,
    ): Request? {
      val request = response.request
      synchronized(this) {
        val account = currentAccount() ?: return null
        if (!request.url.toString().startsWith(account.serverUrl) || account.needsLogin) return null
        if (response.priorResponse != null) return null

        val usedToken = request.header("Authorization")?.removePrefix("Bearer ")
        if (usedToken != account.accessToken) {
          // another request refreshed the tokens in the meantime
          return request.withToken(account.accessToken)
        }

        val refreshed = try {
          api(account.serverUrl).refresh(account.refreshToken).execute()
        } catch (e: IOException) {
          Logger.w(e, "Could not refresh the access token")
          return null
        }
        val user = refreshed.body()?.user
        val accessToken = user?.accessToken
        val refreshToken = user?.refreshToken
        if (!refreshed.isSuccessful || accessToken == null || refreshToken == null) {
          if (refreshed.code() == 401 || refreshed.code() == 403) {
            Logger.w("The server declined the refresh token, signing in again is needed")
            runBlocking {
              accountStore.updateData { it?.copy(needsLogin = true) }
            }
          }
          return null
        }
        runBlocking {
          accountStore.updateData {
            it?.copy(accessToken = accessToken, refreshToken = refreshToken, needsLogin = false)
          }
        }
        return request.withToken(accessToken)
      }
    }
  }
}

private fun Request.withToken(token: String): Request {
  return newBuilder()
    .header("Authorization", "Bearer $token")
    .build()
}
