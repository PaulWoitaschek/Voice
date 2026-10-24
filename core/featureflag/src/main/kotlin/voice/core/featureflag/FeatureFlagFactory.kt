package voice.core.featureflag

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
import voice.core.remoteconfig.api.RemoteConfig
import kotlin.reflect.KClass

@Inject
@SingleIn(AppScope::class)
class FeatureFlagFactory(
  private val remoteConfig: RemoteConfig,
  private val overridesStore: FeatureFlagOverridesStore,
  private val scope: CoroutineScope,
) {

  // Read before anything collects the store on the main thread. A first read started there holds
  // DataStore's lock until main is free again, so get() blocking main would wait for it forever.
  private var overrides: Map<String, FeatureFlagOverride> = runBlocking {
    overridesStore.data.first()
  }

  fun boolean(
    key: String,
    description: String,
    defaultValue: Boolean = false,
  ): FeatureFlag<Boolean> {
    return create(
      key = key,
      description = description,
      type = Boolean::class,
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
    description: String,
    defaultValue: String,
  ): FeatureFlag<String> {
    return create(
      key = key,
      description = description,
      type = String::class,
      readRemoteConfig = {
        remoteConfig.string(key = key, defaultValue = defaultValue)
      },
      createOverride = FeatureFlagOverride::StringValue,
      getOverrideValue = { override ->
        (override as? FeatureFlagOverride.StringValue)?.value
      },
    )
  }

  fun <E : Enum<E>> enum(
    key: String,
    description: String,
    defaultValue: E,
    entries: List<E>,
  ): FeatureFlag<E> {
    fun entry(name: String): E? = entries.firstOrNull { it.name == name }
    val flag = create(
      key = key,
      description = description,
      type = defaultValue.declaringJavaClass.kotlin,
      readRemoteConfig = {
        entry(remoteConfig.string(key = key, defaultValue = defaultValue.name)) ?: defaultValue
      },
      createOverride = { FeatureFlagOverride.StringValue(it.name) },
      getOverrideValue = { override ->
        (override as? FeatureFlagOverride.StringValue)?.value?.let(::entry)
      },
    )
    return object : EnumFeatureFlag<E>, FeatureFlag<E> by flag {
      override val entries: List<E> = entries
    }
  }

  private fun <T : Any> create(
    key: String,
    description: String,
    type: KClass<T>,
    readRemoteConfig: () -> T,
    createOverride: (T) -> FeatureFlagOverride,
    getOverrideValue: (FeatureFlagOverride) -> T?,
  ): FeatureFlag<T> {
    return object : FeatureFlag<T> {

      override val key: String get() = key
      override val description: String get() = description

      override fun get(): T {
        return overrides[key]?.let(getOverrideValue) ?: readRemoteConfig()
      }

      override val type: KClass<T>
        get() = type

      override fun overwrite(value: T) {
        scope.launch {
          overrides = overridesStore.updateData {
            it + (key to createOverride(value))
          }
        }
      }

      override fun clearOverwrite() {
        scope.launch {
          overrides = overridesStore.updateData { overrides ->
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
            val overrideValue = override?.let(getOverrideValue)
            FeatureFlagValue(
              value = overrideValue ?: readRemoteConfig(),
              isOverridden = overrideValue != null,
            )
          }
          .distinctUntilChanged()
    }
  }
}
