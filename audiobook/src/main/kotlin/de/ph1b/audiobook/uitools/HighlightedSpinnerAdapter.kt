package de.ph1b.audiobook.uitools

import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import de.ph1b.audiobook.R

/**
 * TODO: Class description
 *
 * @author Paul Woitaschek
 */
class HighlightedSpinnerAdapter(context: Context, private val spinner: Spinner) : ArrayAdapter<String>(context, R.layout.fragment_book_play_spinner, R.id.spinnerTextItem) {

    fun setContent(content: List<String>) {
        clear()
        addAll(content)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val dropDownView = super.getDropDownView(position, convertView, parent)
        val textView = dropDownView.findViewById(R.id.spinnerTextItem) as TextView

        // highlights the selected item and un-highlights an item if it is not selected.
        // default implementation uses a ViewHolder, so this is necessary.
        if (position == spinner.selectedItemPosition) {
            textView.setBackgroundResource(R.drawable.spinner_selected_background)
            textView.setTextColor(ContextCompat.getColor(context, R.color.copy_abc_primary_text_material_dark))
        } else {
            textView.setBackgroundResource(ThemeUtil.getResourceId(context,
                    R.attr.selectableItemBackground))
            textView.setTextColor(ContextCompat.getColor(context, ThemeUtil.getResourceId(
                    context, android.R.attr.textColorPrimary)))
        }

        return dropDownView
    }
}