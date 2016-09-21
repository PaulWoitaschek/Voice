package de.ph1b.audiobook.mvp

import android.os.Bundle
import d
import i
import rx.subscriptions.CompositeSubscription


/**
 * Basic class for presenters that enables clients to control views
 * Offers handy ways for subscriptions
 * @author Paul Woitaschek
 */
abstract class Presenter<V> {

    protected var view: V? = null

    private var compositeSubscription: CompositeSubscription? = null

    open fun onRestore(savedState: Bundle?) {

    }

    fun bind(view: V) {
        if (this.view == null) {
            i { "binding $view" }
            this.view = view

            compositeSubscription = CompositeSubscription()
            onBind(view, compositeSubscription!!)
        } else {
            d { "$view already bound" }
        }
    }

    fun unbind() {
        i { "Unbinding $view" }
        this.view = null
        compositeSubscription?.unsubscribe()
    }

    open fun onSave(state: Bundle) {

    }

    abstract fun onBind(view: V, subscriptions: CompositeSubscription)
}
