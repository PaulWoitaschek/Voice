package de.ph1b.audiobook.misc

import android.content.Context
import android.support.v7.widget.AppCompatSpinner
import android.util.AttributeSet

/**
 * A simple spinner that has the option to select an item without notifying the listeners
 *
 * @author Paul Woitaschek
 */
class SilentSpinner : AppCompatSpinner {

  constructor(context: Context) : super(context)

  constructor(context: Context, mode: Int) : super(context, mode)

  constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

  /** like [setSelection] but without notifying the listeners **/
  fun setSelectionSilently(position: Int, animate: Boolean) {
    val currentListener = onItemSelectedListener
    onItemSelectedListener = null
    setSelection(position, animate)
    onItemSelectedListener = currentListener
  }
}
