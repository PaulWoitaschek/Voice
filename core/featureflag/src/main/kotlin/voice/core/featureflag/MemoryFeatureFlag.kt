package voice.core.featureflag

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.reflect.KClass

inline fun <reified T : Any> MemoryFeatureFlag(initialValue: T): MemoryFeatureFlag<T> {
  return MemoryFeatureFlag(initialValue, T::class)
}

class MemoryFeatureFlag<T : Any>(
  initialValue: T,
  override val type: KClass<T>,
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
  override val key: String
    get() = error("Unsupported")

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
