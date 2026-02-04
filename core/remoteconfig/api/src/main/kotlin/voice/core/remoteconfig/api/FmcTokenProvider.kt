package voice.core.remoteconfig.api

interface FmcTokenProvider {

  suspend fun token(): String?
}
