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

import de.ph1b.audiobook.activity.BaseActivity

/**
 * Base activity that provides a convenient way for binding a view to a presenter
 *
 * @author Paul Woitaschek
 */
abstract class RxBaseActivity<V, P> : BaseActivity() where P : Presenter<V> {

    abstract fun newPresenter(): P

    abstract fun provideView(): V

    public val presenter: P by lazy { newPresenter() }

    override fun onStart() {
        super.onStart()

        presenter.bind(provideView())
    }

    override fun onStop() {
        super.onStop()

        presenter.unbind()
    }
}