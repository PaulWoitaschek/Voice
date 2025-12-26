package voice.core.featureflag

import voice.core.remoteconfig.api.RemoteConfig

internal class RemoteConfigFeatureFlag<T>(
  private val remoteConfig: RemoteConfig,
  private val fromRemoteConfig: (RemoteConfig) -> T,
) : FeatureFlag<T> {

  override fun get(): T {
    return fromRemoteConfig(remoteConfig)
  }
}
