package voice.core.remoteconfig.noop

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import voice.core.remoteconfig.core.RemoteConfig

@ContributesBinding(AppScope::class)
@Inject
class NoopRemoteConfig : RemoteConfig {

  override fun boolean(
    key: String,
    defaultValue: Boolean,
  ): Boolean = defaultValue

  override fun string(
    key: String,
    defaultValue: String,
  ): String = defaultValue
}
