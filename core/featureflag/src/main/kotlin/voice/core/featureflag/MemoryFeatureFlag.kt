package voice.core.featureflag

class MemoryFeatureFlag<T>(var value: T) : FeatureFlag<T> {

  override fun get(): T = value
}
