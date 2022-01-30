package voice.common.conductor

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.bluelinelabs.conductor.Controller
import de.ph1b.audiobook.common.conductor.ControllerLifecycleOwner
import de.ph1b.audiobook.common.conductor.LifecycleScopeProperty

abstract class BaseController(args: Bundle = Bundle()) : Controller(args), LifecycleOwner {

  @Suppress("LeakingThis")
  private val lifecycleOwner = ControllerLifecycleOwner(this)

  final override fun getLifecycle(): Lifecycle = lifecycleOwner.lifecycle

  val lifecycleScope by LifecycleScopeProperty(lifecycle)
}
