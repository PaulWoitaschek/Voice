package voice.core.remoteconfig.firebase

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import com.google.firebase.remoteconfig.get
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import voice.core.remoteconfig.core.RemoteConfig

@ContributesBinding(AppScope::class)
@Inject
class FirebaseRemoteConfigImpl : RemoteConfig {

  private val firebaseRemoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

  override fun boolean(
    key: String,
    defaultValue: Boolean,
  ): Boolean {
    return valueIfRemote(key = key, default = defaultValue, get = FirebaseRemoteConfigValue::asBoolean)
  }

  override fun string(
    key: String,
    defaultValue: String,
  ): String {
    return valueIfRemote(key = key, default = defaultValue, get = FirebaseRemoteConfigValue::asString)
  }

  private fun <T> valueIfRemote(
    key: String,
    default: T,
    get: (FirebaseRemoteConfigValue) -> T,
  ): T {
    val value = firebaseRemoteConfig[key]
    return if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) {
      get(value)
    } else {
      default
    }
  }
}
