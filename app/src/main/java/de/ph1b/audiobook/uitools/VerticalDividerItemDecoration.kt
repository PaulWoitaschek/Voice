package de.ph1b.audiobook.uitools

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.support.annotation.Px
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ItemDecoration
import android.support.v7.widget.RecyclerView.State
import android.view.View

/**
 * A item decoration with a left offset.
 */
class VerticalDividerItemDecoration(context: Context, @Px private val leftMargin: Int = 0) :
  ItemDecoration() {

  private val divider: Drawable
  private val bounds = Rect()

  init {
    val a = context.obtainStyledAttributes(ATTRS)
    divider = a.getDrawable(0)!!
    a.recycle()
  }

  override fun onDraw(c: Canvas, parent: RecyclerView, state: State) {
    val right = parent.width - parent.paddingRight
    val childCount = parent.childCount

    // don't draw the bottom-most divider
    for (i in 0 until childCount - 1) {
      val child = parent.getChildAt(i)
      parent.getDecoratedBoundsWithMargins(child, bounds)
      val bottom = bounds.bottom + Math.round(child.translationY)
      val top = bottom - divider.intrinsicHeight
      divider.setBounds(leftMargin, top, right, bottom)
      divider.draw(c)
    }
  }

  override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) {
    outRect.set(0, 0, 0, divider.intrinsicHeight)
  }

  companion object {
    private val ATTRS = intArrayOf(android.R.attr.listDivider)
  }
}
