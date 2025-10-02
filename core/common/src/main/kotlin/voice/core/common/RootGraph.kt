package voice.core.common

lateinit var rootGraph: Any

inline fun <reified T> rootGraphAs(): T = rootGraph as T
