package voice.core.remoteconfig.firebase

import com.google.firebase.installations.FirebaseInstallations
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.tasks.await
import voice.core.logging.api.Logger
import voice.core.remoteconfig.api.FmcTokenProvider

@ContributesBinding(AppScope::class)
class FcmTokenProviderImpl : FmcTokenProvider {

  override suspend fun token(): String? {
    val tokenResult = try {
      FirebaseInstallations.getInstance().getToken(true)
        .await()
    } catch (e: Exception) {
      Logger.w(e)
      return null
    }
    return tokenResult.token
  }
}
