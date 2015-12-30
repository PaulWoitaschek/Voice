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

package de.ph1b.audiobook.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Spinner
import android.widget.SpinnerAdapter
import android.widget.TextView
import de.ph1b.audiobook.R
import timber.log.Timber
import java.util.*

/**
 * Spinneradapter that highlights the current selection and shows multiple lines of text.
 *
 * @author Paul Woitaschek
 */
class MultiLineSpinnerAdapter<Type>(private val spinner: Spinner) : BaseAdapter(), SpinnerAdapter {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        val textView = LayoutInflater.from(parent.context).inflate(R.layout.fragment_book_play_spinner, parent, false) as TextView
        Timber.i("parent is $parent")
        val selected = position == spinner.selectedItemPosition
        textView.text = getItem(position).shown
        if (selected && parent != spinner) {
            textView.setBackgroundResource(R.drawable.spinner_selected_background)
        }
        return textView
    }

    override fun getItem(position: Int) = data[position]

    override fun getItemId(position: Int) = position.toLong()

    override fun getCount() = data.size

    private val data = ArrayList<Data<Type>>()

    fun setData(data: List<Data<Type>>) {
        this.data.clear()
        this.data.addAll(data)
        notifyDataSetChanged()
    }

    data class Data<E>(val data: E, val shown: String)
}