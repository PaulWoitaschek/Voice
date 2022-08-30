package voice.common

import kotlin.coroutines.CoroutineContext

data class DispatcherProvider(
  val main: CoroutineContext,
  val io: CoroutineContext,
)
