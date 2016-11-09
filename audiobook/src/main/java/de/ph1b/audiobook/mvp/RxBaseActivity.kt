package de.ph1b.audiobook.mvp

import android.os.Bundle
import de.ph1b.audiobook.features.BaseActivity
import i


/**
 * Base activity that provides a convenient way for binding a view to a presenter
 *
 * @author Paul Woitaschek
 */
abstract class RxBaseActivity<V, out P> : BaseActivity() where P : Presenter<V> {

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
        i { "onResume" }

        presenterDelegate.onStart()
    }

    override fun onStop() {
        super.onStop()
        i { "onPause" }

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