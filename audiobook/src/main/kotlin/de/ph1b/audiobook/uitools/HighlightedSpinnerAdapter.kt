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
 * An extension of [ArrayAdapter] that forces the consumer to decide what it will show by wrapping
 * the type inside [SpinnerData].
 *
 * It also highlights the current selection.
 *
 * @author Paul Woitaschek
 */
class HighlightedSpinnerAdapter<SpinnerType>(context: Context, private val spinner: Spinner) : ArrayAdapter<HighlightedSpinnerAdapter.SpinnerData<SpinnerType>>(context, R.layout.fragment_book_play_spinner, R.id.spinnerTextItem) {


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

    /**
     * As ArrayAdapter uses [toString] on its Type, we use this wrapper class that forces consumers
     * to explicitly define a name while at the same time still being able to retrieve the
     * underlying data.
     */
    abstract class SpinnerData<E>(public val data: E) {

        abstract fun getStringRepresentation(toRepresent: E): String;

        final override fun toString(): String {
            return getStringRepresentation(data)
        }
    }
}