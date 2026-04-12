package voice.core.featureflag

class MemoryFeatureFlag<T>(var value: T) : FeatureFlag<T> {

  override val key: String get() = "key"

  override fun get(): T = value
}
