package voice.pref

interface PrefAdapter<T> {

  fun toString(value: T): String
  fun fromString(string: String): T
}
