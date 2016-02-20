/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Material Audiobook Player. If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.mvp

import Slimber
import android.os.Bundle
import rx.subscriptions.CompositeSubscription


/**
 * Basic class for presenters that enables clients to control views offers handy ways for subscriptions.
 *
 * @author Paul Woitaschek
 */
abstract class Presenter<V> {

    protected var view: V? = null

    private var compositeSubscription: CompositeSubscription? = null

    open fun onRestore(savedState: Bundle?) {

    }

    fun bind(view: V) {
        if (this.view == null) {
            Slimber.i { "binding $view" }
            this.view = view

            compositeSubscription = CompositeSubscription()
            onBind(view, compositeSubscription!!)
        } else {
            Slimber.d { "$view already bound" }
        }
    }

    fun unbind() {
        Slimber.i { "Unbinding $view" }
        this.view = null
        compositeSubscription?.unsubscribe()
    }

    open fun onSave(state: Bundle) {

    }

    abstract fun onBind(view: V, subscriptions: CompositeSubscription)
}
