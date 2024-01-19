package voice.common.conductor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.bluelinelabs.conductor.Controller

typealias InflateBinding<B> = (LayoutInflater, ViewGroup?, Boolean) -> B

abstract class ViewBindingController<B : ViewBinding>(
  args: Bundle = Bundle(),
  private val inflateBinding: InflateBinding<B>,
) : Controller(args) {

  private var _binding: B? = null
  val binding: B get() = _binding ?: error("No binding present.")

  final override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup,
    savedViewState: Bundle?,
  ): View {
    return inflateBinding(inflater, container, false)
      .also {
        _binding = it
        it.onBindingCreated()
      }
      .root
  }

  final override fun onDestroyView(view: View) {
    super.onDestroyView(view)
    onDestroyView()
    _binding = null
  }

  open fun onDestroyView() {}

  open fun B.onBindingCreated() {}
}
