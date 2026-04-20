package voice.core.featureflag

import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass

data class FeatureFlagValue<T : Any>(
  val value: T,
  val isOverridden: Boolean,
)

interface FeatureFlag<T : Any> {
  val key: String
  val description: String
  fun get(): T

  val flow: Flow<FeatureFlagValue<T>>

  val type: KClass<T>

  fun overwrite(value: T)

  fun clearOverwrite()
}
