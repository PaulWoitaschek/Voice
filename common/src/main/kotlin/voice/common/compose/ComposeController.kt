package voice.common.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.bluelinelabs.conductor.Controller

abstract class ComposeController(args: Bundle = Bundle()) : Controller(args) {

  final override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup,
    savedViewState: Bundle?,
  ): View {
    onCreateView()
    return ComposeView(container.context).also {
      it.setContent {
        VoiceTheme {
          Content()
        }
      }
    }
  }

  open fun onCreateView() {}

  @Composable
  abstract fun Content()
}
