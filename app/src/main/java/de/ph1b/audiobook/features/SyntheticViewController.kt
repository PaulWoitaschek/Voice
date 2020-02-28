package de.ph1b.audiobook.features

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.*

abstract class SyntheticViewController(args: Bundle = Bundle()) : BaseController(args), LayoutContainer {

  abstract val layoutRes: Int

  private var _containerView: View? = null
  override val containerView: View? get() = _containerView

  final override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup,
    savedViewState: Bundle?
  ): View {
    val view = inflater.inflate(layoutRes, container, false).also {
      _containerView = it
    }
    onViewCreated()
    return view
  }

  final override fun onDestroyView(view: View) {
    super.onDestroyView(view)
    onDestroyView()
    clearFindViewByIdCache()
    _containerView = null
  }

  open fun onViewCreated() {}

  open fun onDestroyView() {}
}
