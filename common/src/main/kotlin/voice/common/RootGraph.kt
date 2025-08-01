package voice.common

lateinit var rootGraph: Any

inline fun <reified T> rootGraphAs(): T = rootGraph as T
