package de.ph1b.audiobook.features

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.bluelinelabs.conductor.RestoreViewOnCreateController
import com.bluelinelabs.conductor.archlifecycle.ControllerLifecycleOwner
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.*

abstract class BaseController(args: Bundle = Bundle()) : RestoreViewOnCreateController(args),
  LayoutContainer, LifecycleOwner {

  @Suppress("LeakingThis")
  private val lifecycleOwner = ControllerLifecycleOwner(this)

  override fun getLifecycle(): Lifecycle = lifecycleOwner.lifecycle

  private val onCreateViewDisposables = CompositeDisposable()

  fun Disposable.disposeOnDestroyView() {
    onCreateViewDisposables.add(this)
  }

  val activity: AppCompatActivity get() = getActivity() as AppCompatActivity

  val fragmentManager: FragmentManager get() = activity.supportFragmentManager

  fun getString(@StringRes resId: Int): String = activity.getString(resId)

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
    onCreateViewDisposables.clear()
    clearFindViewByIdCache()
    _containerView = null
  }

  open fun onViewCreated() {}

  open fun onDestroyView() {}
}
