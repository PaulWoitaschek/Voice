package de.ph1b.audiobook.features.settings

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.res.use
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import de.ph1b.audiobook.R
import de.ph1b.audiobook.misc.dpToPxRounded
import de.ph1b.audiobook.uitools.drawableFromAttr
import kotlinx.android.synthetic.main.merge_setting_row_double.view.*

class DoubleSettingView : LinearLayout {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    context.theme.obtainStyledAttributes(attrs, R.styleable.DoubleSettingView, 0, 0).use {
      title.text = it.getText(R.styleable.DoubleSettingView_dsv_title)
      description.text = it.getText(R.styleable.DoubleSettingView_dsv_description)
    }
  }

  init {
    foreground = context.drawableFromAttr(R.attr.selectableItemBackground)
    gravity = Gravity.CENTER_VERTICAL
    orientation = VERTICAL
    val padding = context.dpToPxRounded(8F)
    updatePadding(top = padding, bottom = padding)

    View.inflate(context, R.layout.merge_setting_row_double, this)
  }

  fun setDescription(text: String?) {
    description.isVisible = text != null
    description.text = text
  }
}
