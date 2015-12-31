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

import android.os.Bundle

/**
 * Delegates lifecycle methods to the presenter.
 *
 * @author Paul Woitaschek
 */
class PresenterDelegate <V, P>(private val newPresenter: () -> P, private val getView: () -> V)  where P : Presenter<V> {

    public var presenter: P? = null
        private set

    fun onCreate(savedInstanceState: Bundle?) {
        presenter = newPresenter.invoke()
        presenter!!.onRestore(savedInstanceState)
    }

    fun onResume() {
        presenter!!.bind(getView.invoke())
    }

    fun onPause() {
        presenter!!.unbind()
    }

    fun onSaveInstanceState(outState: Bundle) {
        presenter!!.onSave(outState)
    }

    fun onDestroy() {
        presenter = null
    }
}