package de.ph1b.audiobook.features.settings

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StringRes
import de.ph1b.audiobook.R
import de.ph1b.audiobook.misc.dpToPxRounded
import de.ph1b.audiobook.uitools.drawableFromAttr
import kotlinx.android.synthetic.main.merge_setting_row_double.view.*

class DoubleSettingView : LinearLayout {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

  init {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      foreground = context.drawableFromAttr(R.attr.selectableItemBackground)
    }
    gravity = Gravity.CENTER_VERTICAL
    minimumHeight = context.dpToPxRounded(60F)
    orientation = VERTICAL

    View.inflate(context, R.layout.merge_setting_row_double, this)
  }

  fun setTitle(@StringRes resId: Int) {
    title.setText(resId)
  }

  fun setDescription(@StringRes resId: Int) {
    description.setText(resId)
  }

  fun setDescription(text: String?) {
    description.text = text
  }
}
