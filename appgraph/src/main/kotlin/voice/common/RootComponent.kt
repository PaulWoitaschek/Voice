package voice.common

lateinit var rootComponent: Any

inline fun <reified T> rootComponentAs(): T = rootComponent as T
