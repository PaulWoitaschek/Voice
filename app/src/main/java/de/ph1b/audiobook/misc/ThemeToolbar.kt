package de.ph1b.audiobook.misc

import androidx.appcompat.widget.Toolbar
import androidx.core.view.forEach
import de.ph1b.audiobook.R

fun Toolbar.tint() {
  val color = context.color(R.color.toolbarIconColor)
  menu.forEach {
    it.icon?.setTint(color)
  }
  navigationIcon?.setTint(color)
  overflowIcon?.setTint(color)
}
