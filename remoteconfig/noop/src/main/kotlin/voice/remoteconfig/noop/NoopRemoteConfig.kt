package voice.remoteconfig.noop

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import voice.common.AppScope
import voice.remoteconfig.core.RemoteConfig
import javax.inject.Inject

@ContributesBinding(AppScope::class)
class NoopRemoteConfig
@Inject constructor() : RemoteConfig {

  override fun boolean(
    key: String,
    defaultValue: Boolean,
  ): Boolean = defaultValue

  override fun string(
    key: String,
    defaultValue: String,
  ): String = defaultValue
}
