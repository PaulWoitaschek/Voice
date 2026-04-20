package voice.core.featureflag

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.reflect.KClass

inline fun <reified T : Any> MemoryFeatureFlag(
  initialValue: T,
  key: String = "memory_feature_flag",
  description: String = "",
): MemoryFeatureFlag<T> {
  return MemoryFeatureFlag(initialValue, T::class, key, description)
}

class MemoryFeatureFlag<T : Any>(
  initialValue: T,
  override val type: KClass<T>,
  override val key: String = "memory_feature_flag",
  override val description: String = "",
) : FeatureFlag<T> {

  private var defaultValue: T = initialValue
  private val state = MutableStateFlow(
    FeatureFlagValue(
      value = initialValue,
      isOverridden = false,
    ),
  )

  var value: T
    get() = state.value.value
    set(value) {
      defaultValue = value
      if (!state.value.isOverridden) {
        state.value = FeatureFlagValue(
          value = value,
          isOverridden = false,
        )
      }
    }
  override fun get(): T = state.value.value

  override fun overwrite(value: T) {
    state.value = FeatureFlagValue(
      value = value,
      isOverridden = true,
    )
  }

  override fun clearOverwrite() {
    state.value = FeatureFlagValue(
      value = defaultValue,
      isOverridden = false,
    )
  }

  override val flow: Flow<FeatureFlagValue<T>> = state
}
