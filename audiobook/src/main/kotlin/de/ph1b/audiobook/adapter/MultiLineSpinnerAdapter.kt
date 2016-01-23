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

import android.content.Context
import android.graphics.Color
import android.support.annotation.ColorInt
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Spinner
import android.widget.SpinnerAdapter
import android.widget.TextView
import de.ph1b.audiobook.R
import de.ph1b.audiobook.uitools.ThemeUtil
import timber.log.Timber
import java.util.*

/**
 * Adapter fror [Spinner] that highlights the current selection and shows multiple lines of text.
 *
 * @author Paul Woitaschek
 */
class MultiLineSpinnerAdapter<Type>(private val spinner: Spinner, private val context: Context, @ColorInt private val unselectedTextColor: Int) : BaseAdapter(), SpinnerAdapter {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        // no need for view holder pattern, we can just reuse the view as its a single TextView
        val textView =
                if (convertView == null) {
                    LayoutInflater.from(context).inflate(R.layout.fragment_book_play_spinner, parent, false) as TextView
                } else {
                    convertView as TextView
                }

        Timber.i("parent is $parent")
        val selected = position == spinner.selectedItemPosition
        textView.text = getItem(position).shown

        if (parent == spinner) {
            textView.setBackgroundResource(0)
            textView.setTextColor(unselectedTextColor)
        } else if (selected) {
            textView.setBackgroundResource(R.drawable.spinner_selected_background)
            textView.setTextColor(Color.WHITE)
        } else {
            textView.setBackgroundResource(ThemeUtil.getResourceId(context, android.R.attr.windowBackground))
            textView.setTextColor(ContextCompat.getColor(context, ThemeUtil.getResourceId(context, android.R.attr.textColorPrimary)))
        }

        return textView
    }

    override fun getItem(position: Int) = data[position]

    override fun getItemId(position: Int) = position.toLong()

    override fun getCount() = data.size

    private val data = ArrayList<Data<Type>>()

    fun setData(data: List<Data<Type>>) {
        if (this.data != data) {
            this.data.clear()
            this.data.addAll(data)
            notifyDataSetChanged()
        }
    }

    data class Data<E>(val data: E, val shown: String)
}