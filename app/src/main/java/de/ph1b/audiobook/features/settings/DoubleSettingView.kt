package de.ph1b.audiobook.features.settings

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.res.use
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import de.ph1b.audiobook.R
import de.ph1b.audiobook.databinding.MergeSettingRowDoubleBinding
import de.ph1b.audiobook.misc.dpToPxRounded
import de.ph1b.audiobook.uitools.drawableFromAttr

class DoubleSettingView : LinearLayout {

  private val binding = MergeSettingRowDoubleBinding.inflate(LayoutInflater.from(context), this)

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    context.theme.obtainStyledAttributes(attrs, R.styleable.DoubleSettingView, 0, 0).use {
      binding.title.text = it.getText(R.styleable.DoubleSettingView_dsv_title)
      binding.description.text = it.getText(R.styleable.DoubleSettingView_dsv_description)
    }
  }

  init {
    foreground = context.drawableFromAttr(R.attr.selectableItemBackground)
    gravity = Gravity.CENTER_VERTICAL
    orientation = VERTICAL
    val padding = context.dpToPxRounded(8F)
    updatePadding(top = padding, bottom = padding)
  }

  fun setDescription(text: String?) {
    binding.description.isVisible = text != null
    binding.description.text = text
  }
}
