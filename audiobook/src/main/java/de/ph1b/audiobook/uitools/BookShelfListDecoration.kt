package de.ph1b.audiobook.uitools

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ItemDecoration
import android.support.v7.widget.RecyclerView.State
import android.view.View
import de.ph1b.audiobook.misc.dpToPxRounded

/**
 * A item decoration with a left offset for the book shelf list
 *
 * @author Paul Woitaschek
 */
class BookShelfListDecoration(context: Context) : ItemDecoration() {

  private val divider: Drawable
  private val bounds = Rect()
  private val marginLeft = context.dpToPxRounded(72F)

  init {
    val a = context.obtainStyledAttributes(ATTRS)
    divider = a.getDrawable(0)
    a.recycle()
  }

  override fun onDraw(c: Canvas, parent: RecyclerView, state: State?) {
    c.save()
    val right = parent.width - parent.paddingRight

    val childCount = parent.childCount

    for (i in 0 until childCount - 1) {
      val child = parent.getChildAt(i)
      parent.getDecoratedBoundsWithMargins(child, bounds)
      val bottom = bounds.bottom + Math.round(ViewCompat.getTranslationY(child))
      val top = bottom - divider.intrinsicHeight
      divider.setBounds(marginLeft, top, right, bottom)
      divider.draw(c)
    }

    c.restore()
  }


  override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State?) {
    outRect.set(0, 0, 0, divider.intrinsicHeight)
  }

  companion object {
    private val ATTRS = intArrayOf(android.R.attr.listDivider)
  }
}
