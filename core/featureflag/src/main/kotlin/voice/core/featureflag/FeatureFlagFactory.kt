package voice.core.featureflag

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import voice.core.data.store.FeatureFlagOverridesStore
import voice.core.remoteconfig.api.RemoteConfig
import kotlin.reflect.KClass

@Inject
@SingleIn(AppScope::class)
class FeatureFlagFactory(
  private val remoteConfig: RemoteConfig,
  @FeatureFlagOverridesStore
  private val overridesStore: DataStore<Map<String, FeatureFlagOverride>>,
  private val scope: CoroutineScope,
) {

  private var overrides: Map<String, FeatureFlagOverride>? = null

  init {
    scope.launch {
      overridesStore.data.collect {
        overrides = it
      }
    }
  }

  private fun overrides(): Map<String, FeatureFlagOverride> {
    return overrides ?: runBlocking {
      overridesStore.data.first()
    }
  }

  fun boolean(
    key: String,
    defaultValue: Boolean = false,
  ): FeatureFlag<Boolean> {
    return create(
      key = key,
      readRemoteConfig = {
        remoteConfig.boolean(key = key, defaultValue = defaultValue)
      },
      createOverride = FeatureFlagOverride::BooleanValue,
      getOverrideValue = { override ->
        (override as? FeatureFlagOverride.BooleanValue)?.value
      },
    )
  }

  fun string(
    key: String,
    defaultValue: String,
  ): FeatureFlag<String> {
    return create(
      key = key,
      readRemoteConfig = {
        remoteConfig.string(key = key, defaultValue = defaultValue)
      },
      createOverride = FeatureFlagOverride::StringValue,
      getOverrideValue = { override ->
        (override as? FeatureFlagOverride.StringValue)?.value
      },
    )
  }

  private inline fun <reified T : Any> create(
    key: String,
    noinline readRemoteConfig: () -> T,
    noinline createOverride: (T) -> FeatureFlagOverride,
    noinline getOverrideValue: (FeatureFlagOverride) -> T?,
  ): FeatureFlag<T> {
    return object : FeatureFlag<T> {

      override val key: String get() = key

      override fun get(): T {
        return overrides()[key]?.let(getOverrideValue) ?: readRemoteConfig()
      }

      override val type: KClass<T>
        get() = T::class

      override fun overwrite(value: T) {
        scope.launch {
          overridesStore.updateData {
            it + (key to createOverride(value))
          }
        }
      }

      override fun clearOverwrite() {
        scope.launch {
          overridesStore.updateData { overrides ->
            overrides.toMutableMap().also {
              it.remove(key)
            }
          }
        }
      }

      override val flow: Flow<FeatureFlagValue<T>>
        get() = overridesStore.data
          .map { overrides ->
            val override = overrides[key]
            FeatureFlagValue(
              value = override?.let(getOverrideValue) ?: readRemoteConfig(),
              isOverridden = override != null,
            )
          }
          .distinctUntilChanged()
    }
  }
}
