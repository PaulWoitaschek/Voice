package voice.core.featureflag

interface FeatureFlag<T> {
  val key: String
  fun get(): T
}
