package de.ph1b.audiobook.features.bookOverview.list

import android.content.Context
import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ItemDecoration
import android.view.View
import de.ph1b.audiobook.misc.dpToPxRounded


class BookShelfItemDecoration(context: Context) : ItemDecoration() {

  private val margin = context.dpToPxRounded(8F)

  override fun getItemOffsets(outRect: Rect, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
    outRect.set(margin, margin / 2, margin, margin / 2)
  }
}
