package voice.core.common

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

data class DispatcherProvider(
  val main: CoroutineContext = Dispatchers.Main,
  val io: CoroutineContext = Dispatchers.IO,
  val mainImmediate: CoroutineContext = Dispatchers.Main.immediate,
  val default: CoroutineContext = Dispatchers.Default,
)

@Suppress("FunctionName")
fun MainScope(dispatcherProvider: DispatcherProvider): CoroutineScope {
  return CoroutineScope(SupervisorJob() + dispatcherProvider.mainImmediate)
}
