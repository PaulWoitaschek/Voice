package voice.core.featureflag

interface FeatureFlag<T> {
  fun get(): T
}
