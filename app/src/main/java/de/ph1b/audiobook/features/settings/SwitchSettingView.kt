package de.ph1b.audiobook.features.settings

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.use
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import de.ph1b.audiobook.R
import de.ph1b.audiobook.misc.dpToPxRounded
import de.ph1b.audiobook.uitools.drawableFromAttr
import kotlinx.android.synthetic.main.merge_setting_row_switch.view.*

class SwitchSettingView : ConstraintLayout {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    context.theme.obtainStyledAttributes(attrs, R.styleable.SwitchSettingView, 0, 0).use {
      switchTitle.text = it.getText(R.styleable.SwitchSettingView_ssv_title)
      switchDescription.text = it.getText(R.styleable.SwitchSettingView_ssv_description)
      switchDescription.isVisible = switchDescription.text?.isNotBlank() == true
    }
  }

  init {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      foreground = context.drawableFromAttr(R.attr.selectableItemBackground)
    }
    View.inflate(context, R.layout.merge_setting_row_switch, this)
    val padding = context.dpToPxRounded(8F)
    updatePadding(top = padding, bottom = padding)
  }

  fun onCheckedChanged(listener: () -> Unit) {
    setOnClickListener { listener() }
  }

  fun setChecked(checked: Boolean) {
    switchSetting.isChecked = checked
  }
}
