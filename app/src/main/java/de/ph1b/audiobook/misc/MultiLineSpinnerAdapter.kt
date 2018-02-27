package de.ph1b.audiobook.misc

import android.content.Context
import android.graphics.Color
import android.support.annotation.ColorInt
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Spinner
import android.widget.SpinnerAdapter
import android.widget.TextView
import de.ph1b.audiobook.R
import de.ph1b.audiobook.uitools.ThemeUtil
import java.util.*

/**
 * Adapter for [Spinner] that highlights the current selection and shows multiple lines of text.
 */
class MultiLineSpinnerAdapter<Type>(
  private val spinner: Spinner,
  private val context: Context,
  @ColorInt private val unselectedTextColor: Int,
  private val resolveName: (type: Type, position: Int) -> String = { type, _ -> type.toString() }
) : BaseAdapter(), SpinnerAdapter {

  private val data = ArrayList<Type>()

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
    // no need for view holder pattern, we can just reuse the view as its a single TextView
    val textView =
      if (convertView == null) {
        context.layoutInflater().inflate(R.layout.book_play_spinner, parent, false) as TextView
      } else {
        convertView as TextView
      }

    val selected = position == spinner.selectedItemPosition
    textView.text = resolveName(getItem(position), position)

    when {
      parent == spinner -> {
        textView.setBackgroundResource(0)
        textView.setTextColor(unselectedTextColor)
      }
      selected -> {
        textView.setBackgroundResource(R.drawable.selected_spinner_background)
        textView.setTextColor(Color.WHITE)
      }
      else -> {
        textView.setBackgroundResource(
          ThemeUtil.getResourceId(
            context,
            android.R.attr.windowBackground
          )
        )
        textView.setTextColor(
          context.color(
            ThemeUtil.getResourceId(
              context,
              android.R.attr.textColorPrimary
            )
          )
        )
      }
    }

    return textView
  }

  override fun getItem(position: Int) = data[position]

  override fun getItemId(position: Int) = position.toLong()

  override fun getCount() = data.size

  fun setData(data: List<Type>) {
    if (this.data != data) {
      this.data.clear()
      this.data.addAll(data)
      notifyDataSetChanged()
    }
  }
}
