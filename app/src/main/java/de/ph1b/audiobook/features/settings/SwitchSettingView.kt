package de.ph1b.audiobook.features.settings

import android.content.Context
import android.os.Build
import android.support.annotation.StringRes
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import de.ph1b.audiobook.R
import de.ph1b.audiobook.uitools.drawableFromAttr
import kotlinx.android.synthetic.main.merge_setting_row_switch.view.*

class SwitchSettingView : ConstraintLayout {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

  init {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      foreground = context.drawableFromAttr(R.attr.selectableItemBackground)
    }
    View.inflate(context, R.layout.merge_setting_row_switch, this)
  }


  fun setTitle(@StringRes resId: Int) {
    switchTitle.setText(resId)
  }

  fun setDescription(@StringRes resId: Int) {
    switchDescription.setText(resId)
  }

  inline fun onCheckedChanged(crossinline listener: (checked: Boolean) -> Unit) {
    switchSetting.setOnCheckedChangeListener { _, isChecked ->
      listener(isChecked)
    }
  }

  fun setChecked(checked: Boolean) {
    switchSetting.isChecked = checked
  }

  fun toggle() {
    switchSetting.toggle()
  }
}
