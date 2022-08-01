package voice.common.conductor

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.INITIALIZED
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class LifecycleScopeProperty(private val lifecycle: Lifecycle) : ReadOnlyProperty<Any?, CoroutineScope> {

  private val scopes = mutableMapOf<Lifecycle.State, CoroutineScope>()

  init {
    initScope(CREATED)
    scopes[INITIALIZED] = scopes.getValue(CREATED)
    lifecycle.addObserver(
      object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
          when (event) {
            ON_START -> initScope(STARTED)
            ON_RESUME -> initScope(RESUMED)
            ON_PAUSE -> cancelScope(RESUMED)
            ON_STOP -> cancelScope(STARTED)
            ON_DESTROY -> cancelScope(CREATED)
            Lifecycle.Event.ON_CREATE,
            Lifecycle.Event.ON_ANY,
            -> {
              // no-op
            }
          }
        }
      },
    )
  }

  private fun initScope(state: Lifecycle.State) {
    scopes[state] = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  }

  private fun cancelScope(state: Lifecycle.State) {
    scopes.getValue(state).cancel()
  }

  override fun getValue(thisRef: Any?, property: KProperty<*>): CoroutineScope {
    return scopes.getValue(lifecycle.currentState)
  }
}
