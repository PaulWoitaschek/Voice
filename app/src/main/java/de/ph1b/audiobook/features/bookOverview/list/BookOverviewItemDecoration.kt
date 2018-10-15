package de.ph1b.audiobook.features.bookOverview.list

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import de.ph1b.audiobook.misc.dpToPxRounded

class BookOverviewItemDecoration(context: Context) : ItemDecoration() {

  private val margin = context.dpToPxRounded(8F)

  override fun getItemOffsets(
    outRect: Rect,
    view: View,
    parent: RecyclerView,
    state: RecyclerView.State
  ) {
    outRect.set(margin, margin / 2, margin, margin / 2)
  }
}
