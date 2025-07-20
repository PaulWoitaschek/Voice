package voice.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

data class DispatcherProvider(
  val main: CoroutineContext,
  val io: CoroutineContext,
)

@Suppress("FunctionName")
fun MainScope(dispatcherProvider: DispatcherProvider): CoroutineScope {
  return CoroutineScope(SupervisorJob() + dispatcherProvider.main)
}
