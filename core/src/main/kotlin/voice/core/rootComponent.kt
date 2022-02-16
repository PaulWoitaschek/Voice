package voice.core

lateinit var rootComponent: Any

inline fun <reified T> rootComponentAs(): T = rootComponent as T
