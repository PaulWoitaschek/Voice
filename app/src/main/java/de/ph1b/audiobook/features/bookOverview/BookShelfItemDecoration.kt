package de.ph1b.audiobook.features.bookOverview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ItemDecoration
import android.support.v7.widget.RecyclerView.State
import kotlinx.android.synthetic.main.book_shelf_row.*


class BookShelfItemDecoration(context: Context) : ItemDecoration() {

  private val divider: Drawable
  private val bounds = Rect()

  init {
    val a = context.obtainStyledAttributes(ATTRS)
    divider = a.getDrawable(0)
    a.recycle()
  }

  override fun onDrawOver(c: Canvas, parent: RecyclerView, state: State?) {
    val right = parent.width - parent.paddingRight
    val childCount = parent.childCount

    // don't draw the bottom-most divider
    for (i in 0 until childCount - 1) {
      val child = parent.getChildAt(i)
      parent.getDecoratedBoundsWithMargins(child, bounds)
      val bottom = bounds.bottom + Math.round(child.translationY)
      val top = bottom - divider.intrinsicHeight

      val viewHolder = parent.getChildViewHolder(child) as BookShelfAdapter.ViewHolder
      val progressBar = viewHolder.progress
      val left = progressBar.left + progressBar.progress * progressBar.width
      divider.setBounds(Math.round(left), top, right, bottom)
      divider.draw(c)
    }
  }

  companion object {
    private val ATTRS = intArrayOf(android.R.attr.listDivider)
  }
}
