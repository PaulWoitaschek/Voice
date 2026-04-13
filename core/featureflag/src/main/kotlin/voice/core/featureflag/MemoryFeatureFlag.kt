package voice.core.featureflag

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.reflect.KClass

class MemoryFeatureFlag<T : Any>(
  var value: T,
  override val type: KClass<T>,
) : FeatureFlag<T> {

  override val key: String get() = "key"

  override fun get(): T = value

  override fun overwrite(value: T) {
    error("Unsupported")
  }

  override fun clearOverwrite() {
    error("Unsupported")
  }

  override val flow: Flow<FeatureFlagValue<T>>
    get() = flowOf(
      FeatureFlagValue(
        value = value,
        isOverridden = false,
      ),
    )
}
