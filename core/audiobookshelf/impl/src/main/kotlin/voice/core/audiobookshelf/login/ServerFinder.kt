package voice.core.audiobookshelf.login

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.HttpException
import voice.core.audiobookshelf.AudiobookshelfServer
import voice.core.audiobookshelf.FindServerResult
import voice.core.audiobookshelf.http.AudiobookshelfHttp
import voice.core.logging.api.Logger
import java.io.IOException
import java.security.cert.CertificateException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLPeerUnverifiedException

/**
 * The oldest server with the login that hands out refresh tokens.
 */
private val MINIMUM_SERVER_VERSION = listOf(2, 26, 0)

@Inject
internal class ServerFinder(private val http: AudiobookshelfHttp) {

  suspend fun find(address: String): FindServerResult = withContext(Dispatchers.IO) {
    var certificateNotTrusted = false
    // a host that can't be reached won't answer on another path either, which saves waiting for every timeout
    val unreachableOrigins = mutableSetOf<String>()
    for (url in serverUrlCandidates(address)) {
      val httpUrl = url.toHttpUrlOrNull() ?: continue
      val origin = "${httpUrl.scheme}://${httpUrl.host}:${httpUrl.port}"
      if (origin in unreachableOrigins) continue
      // a server out on the internet with a certificate problem doesn't get the password without encryption
      if (certificateNotTrusted && !httpUrl.isHttps && !isLocalHost(httpUrl.host)) continue
      val response = try {
        http.api(url).status()
      } catch (e: SSLException) {
        // https against a port that only speaks http fails too, that is no certificate problem
        if (e.isCertificateProblem()) certificateNotTrusted = true
        unreachableOrigins += origin
        continue
      } catch (_: IOException) {
        unreachableOrigins += origin
        continue
      } catch (_: HttpException) {
        continue
      } catch (_: SerializationException) {
        continue
      }
      val status = response.body()?.takeIf { response.isSuccessful } ?: continue
      if (status.app != "audiobookshelf") continue
      if (!status.isInit) return@withContext FindServerResult.NotSetUp
      val version = status.serverVersion.orEmpty()
      if (!version.isAtLeast(MINIMUM_SERVER_VERSION)) return@withContext FindServerResult.TooOld(version)
      // a server that redirects, like from http to https, lives where the redirect ends
      val finalUrl = response.raw().request.url
      if (httpUrl.isHttps && !finalUrl.isHttps && !isLocalHost(finalUrl.host)) {
        Logger.d("A server candidate redirects from https to http, skipping it")
        continue
      }
      val serverUrl = finalUrl.toString().let { if (it.endsWith("/status")) it.removeSuffix("status") else url }
      return@withContext FindServerResult.Found(
        AudiobookshelfServer(
          url = serverUrl,
          version = version,
          passwordLogin = "local" in status.authMethods,
        ),
      )
    }
    if (certificateNotTrusted) FindServerResult.CertificateNotTrusted else FindServerResult.NotFound
  }
}

private fun SSLException.isCertificateProblem(): Boolean {
  return this is SSLPeerUnverifiedException || generateSequence<Throwable>(cause) { it.cause }.any { it is CertificateException }
}

internal fun String.isAtLeast(minimum: List<Int>): Boolean {
  val parts = removePrefix("v").split('.', '-').mapNotNull { it.toIntOrNull() }
  minimum.forEachIndexed { index, required ->
    val part = parts.getOrNull(index) ?: 0
    if (part != required) return part > required
  }
  return true
}
