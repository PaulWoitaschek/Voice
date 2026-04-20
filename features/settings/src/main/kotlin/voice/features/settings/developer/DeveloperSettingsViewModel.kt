package voice.features.settings.developer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.core.featureflag.FeatureFlag
import voice.core.remoteconfig.api.FmcTokenProvider
import voice.core.remoteconfig.api.RemoteConfig
import voice.navigation.Navigator

@Inject
class DeveloperSettingsViewModel(
  private val navigator: Navigator,
  private val fmcTokenProvider: FmcTokenProvider,
  private val remoteConfig: RemoteConfig,
  dispatcherProvider: DispatcherProvider,
  private val featureFlags: Set<FeatureFlag<*>>,
) {

  private val scope = MainScope(dispatcherProvider)

  @Composable
  fun viewState(): DeveloperSettingsViewState {
    val fcmToken: String? by produceState(null) {
      value = fmcTokenProvider.token()
    }

    val flags = remember {
      val flags = featureFlags.toList()
      combine(flags.map { it.flow }) {
        it.withIndex().map { (index, value) ->
          flags[index] to value
        }
      }
    }.collectAsState(emptyList()).value

    return DeveloperSettingsViewState(
      fcmToken = fcmToken,
      featureFlags = flags
        .sortedBy { it.first.key }
        .map { (featureFlag, value) ->
          when (featureFlag.type) {
            Boolean::class -> DeveloperSettingsViewState.FeatureFlagViewState.BooleanFlag(
              key = featureFlag.key,
              description = featureFlag.description,
              value = value.value as Boolean,
              isOverridden = value.isOverridden,
            )
            String::class -> DeveloperSettingsViewState.FeatureFlagViewState.StringFlag(
              key = featureFlag.key,
              description = featureFlag.description,
              value = value.value as String,
              isOverridden = value.isOverridden,
            )
            else -> error("Invalid feature flag type: ${featureFlag.type}")
          }
        },
    )
  }

  private var refreshJob: Job? = null
  fun refreshRemoteConfig() {
    if (refreshJob?.isActive == true) return
    refreshJob = scope.launch {
      remoteConfig.refresh()
    }
  }

  fun close() {
    navigator.goBack()
  }

  fun setBooleanOverride(
    key: String,
    value: Boolean,
  ) {
    featureFlags.forEach {
      if (it.key == key) {
        @Suppress("UNCHECKED_CAST")
        (it as FeatureFlag<Boolean>).overwrite(value)
      }
    }
  }

  fun setStringOverride(
    key: String,
    value: String,
  ) {
    featureFlags.forEach {
      if (it.key == key) {
        @Suppress("UNCHECKED_CAST")
        (it as FeatureFlag<String>).overwrite(value)
      }
    }
  }

  fun clearOverride(key: String) {
    featureFlags.forEach {
      if (it.key == key) {
        it.clearOverwrite()
      }
    }
  }
}
