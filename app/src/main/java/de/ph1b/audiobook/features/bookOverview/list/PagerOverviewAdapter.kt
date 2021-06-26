package de.ph1b.audiobook.features.bookOverview.list

import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.viewpager2.RouterStateAdapter
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.bookOverview.ScreenSlideController
import de.ph1b.audiobook.features.bookOverview.list.header.BookOverviewCategory
import timber.log.Timber


class PagerOverviewAdapter(private val host:Controller) : RouterStateAdapter(host) {
  private var listCategory: List<BookOverviewCategory> = listOf()

  override fun configureRouter(router: Router, position: Int) {
    if (!router.hasRootController()) {
      Timber.i("index at $position")
      if(position >= itemCount) {
        return
      }
      val bookCategory = listCategory[position]
      val page = ScreenSlideController(host,bookCategory)
      router.setRoot(RouterTransaction.with(page))
    }
  }
  override fun getItemCount() = listCategory.size
  fun updateItems(categorySet: Set<BookOverviewCategory>)
  {
    val newList = categorySet.toList()
    val oldSize = listCategory.size
    val newSize = newList.size
    listCategory = newList
    if(newSize > oldSize)
    {
      notifyItemRangeInserted(oldSize, newSize - oldSize)
    }
    else if(newSize < oldSize) {
      notifyItemRangeRemoved(oldSize, newSize - oldSize)
    }
  }
  fun notifyItems()
  {
    notifyItemRangeChanged(0, listCategory.size)
  }
  fun getItemName(position: Int):Int
  {
    if(position >= itemCount) {
      return R.string.book_header_unknown
    }
    return listCategory[position].nameRes
  }
}
