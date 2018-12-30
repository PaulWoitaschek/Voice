package de.ph1b.audiobook.features.bookCategory

import android.os.Bundle
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.BaseController
import de.ph1b.audiobook.features.bookOverview.list.header.BookOverviewCategory
import de.ph1b.audiobook.misc.conductor.popOrBack
import de.ph1b.audiobook.misc.tint
import kotlinx.android.synthetic.main.book_category.*

private const val NI_CATEGORY = "ni#category"

class BookCategoryController(bundle: Bundle) : BaseController(bundle) {

  constructor(category: BookOverviewCategory) : this(Bundle().apply {
    putSerializable(NI_CATEGORY, category)
  })

  private val category = bundle.getSerializable(NI_CATEGORY) as BookOverviewCategory

  override val layoutRes = R.layout.book_category

  override fun onViewCreated() {
    toolbar.setTitle(category.nameRes)
    toolbar.tint()
    toolbar.setNavigationOnClickListener { popOrBack() }
  }
}
