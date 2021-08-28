package voice.common.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.bluelinelabs.conductor.Controller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

abstract class ComposeController(args: Bundle = Bundle()) : Controller(args) {

  private lateinit var onCreateViewScope: CoroutineScope

  final override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
    onCreateViewScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    onCreateView(onCreateViewScope)
    return ComposeView(container.context).also {
      it.setContent {
        VoiceTheme {
          Content()
        }
      }
    }
  }

  open fun onCreateView(scope: CoroutineScope) {}

  @Composable
  abstract fun Content()

  @CallSuper
  override fun onDestroyView(view: View) {
    super.onDestroyView(view)
    onCreateViewScope.cancel()
  }
}
