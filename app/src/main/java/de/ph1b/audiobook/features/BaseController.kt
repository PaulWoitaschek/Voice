package de.ph1b.audiobook.features

import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.app.FragmentManager
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.rxlifecycle2.RxController
import io.reactivex.Observable

abstract class BaseController<B : ViewDataBinding>(args: Bundle = Bundle()) : RxController(args) {

  fun <T> Observable<T>.bindToLifeCycle(): Observable<T> = compose(bindToLifecycle<T>())

  val fragmentManager: FragmentManager
    get() = activity.supportFragmentManager

  fun getString(@StringRes resId: Int): String = activity.getString(resId)

  val activity: AppCompatActivity
    get() = getActivity() as AppCompatActivity

  abstract val layoutRes: Int

  private var internalBinding: B? = null

  val binding: B
    get() = internalBinding!!

  override final fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
    internalBinding = DataBindingUtil.inflate(inflater, layoutRes, container, false)
    onBindingCreated(internalBinding!!)
    return internalBinding!!.root
  }

  override final fun onDestroyView(view: View) {
    super.onDestroyView(view)
    onDestroyBinding(binding)
    internalBinding!!.unbind()
    internalBinding = null
  }

  open fun onBindingCreated(binding: B) {}

  open fun onDestroyBinding(binding: B) {}
}
