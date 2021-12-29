package de.ph1b.audiobook.mvp

import android.os.Bundle
import de.ph1b.audiobook.features.BaseActivity
import timber.log.Timber

/**
 * Base activity that provides a convenient way for binding a view to a presenter
 */
abstract class RxBaseActivity<V : Any, out P> : BaseActivity() where P : Presenter<V> {

  private val presenterDelegate = PresenterDelegate({ newPresenter() }, { provideView() })

  abstract fun newPresenter(): P

  abstract fun provideView(): V

  fun presenter() = presenterDelegate.presenter()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    presenterDelegate.onCreate(savedInstanceState)
  }

  override fun onStart() {
    super.onStart()
    Timber.i("onResume")

    presenterDelegate.onStart()
  }

  override fun onStop() {
    super.onStop()
    Timber.i("onPause")

    presenterDelegate.onStop()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)

    presenterDelegate.onSaveInstanceState(outState)
  }

  override fun onDestroy() {
    super.onDestroy()

    presenterDelegate.onDestroy()
  }
}
