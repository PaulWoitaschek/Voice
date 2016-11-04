package de.ph1b.audiobook.mvp

import android.os.Bundle
import d
import i
import io.reactivex.disposables.CompositeDisposable


/**
 * Basic class for presenters that enables clients to control views offers handy ways for subscriptions.
 *
 * @author Paul Woitaschek
 */
abstract class Presenter<V> {

    protected var view: V? = null

    private var compositeDisposable: CompositeDisposable? = null

    open fun onRestore(savedState: Bundle?) {

    }

    fun bind(view: V) {
        if (this.view == null) {
            i { "binding $view" }
            this.view = view

            compositeDisposable = CompositeDisposable()
            onBind(view, compositeDisposable!!)
        } else {
            d { "$view already bound" }
        }
    }

    fun unbind() {
        i { "Unbinding $view" }
        this.view = null
        compositeDisposable?.dispose()
    }

    open fun onSave(state: Bundle) {

    }

    abstract fun onBind(view: V, disposables: CompositeDisposable)
}
