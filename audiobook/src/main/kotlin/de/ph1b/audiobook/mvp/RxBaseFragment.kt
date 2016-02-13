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
import android.support.v4.app.Fragment

/**
 * Base fragment that provides a convenient way for binding a view to a presenter
 *
 * @author Paul Woitaschek
 */
abstract class RxBaseFragment <V, P> : Fragment() where P : Presenter<V> {

    private val presenterDelegate = PresenterDelegate({ newPresenter() }, { provideView() })

    abstract fun newPresenter(): P

    abstract fun provideView(): V

    fun presenter() = presenterDelegate.presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        presenterDelegate.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()

        presenterDelegate.onStart()
    }

    override fun onStop() {
        super.onStop()

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