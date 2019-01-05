package de.ph1b.audiobook.features.bookCategory

import android.os.Bundle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.BaseController
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewClick
import de.ph1b.audiobook.features.bookOverview.list.header.BookOverviewCategory
import de.ph1b.audiobook.features.bookPlaying.BookPlayController
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.conductor.asTransaction
import de.ph1b.audiobook.misc.conductor.popOrBack
import de.ph1b.audiobook.misc.tint
import de.ph1b.audiobook.uitools.BookChangeHandler
import kotlinx.android.synthetic.main.book_category.*
import javax.inject.Inject

private const val NI_CATEGORY = "ni#category"

class BookCategoryController(bundle: Bundle) : BaseController(bundle) {

  @Inject
  lateinit var viewModel: BookCategoryViewModel

  init {
    appComponent.inject(this)
  }

  constructor(category: BookOverviewCategory) : this(Bundle().apply {
    putSerializable(NI_CATEGORY, category)
  })

  private val category = bundle.getSerializable(NI_CATEGORY) as BookOverviewCategory

  override val layoutRes = R.layout.book_category

  override fun onViewCreated() {
    toolbar.setTitle(category.nameRes)
    toolbar.tint()
    toolbar.setNavigationOnClickListener { popOrBack() }

    val adapter = BookCategoryAdapter { book, clickType ->
      when (clickType) {
        BookOverviewClick.REGULAR -> {
          val changeHandler = BookChangeHandler().apply {
            transitionName = book.coverTransitionName
          }
          router.replaceTopController(BookPlayController(book.id).asTransaction(changeHandler, changeHandler))
        }
        BookOverviewClick.MENU -> {
        }
      }
    }
    recyclerView.adapter = adapter
    val layoutManager = GridLayoutManager(activity, 1)
    recyclerView.layoutManager = layoutManager
    recyclerView.addItemDecoration(BookCategoryItemDecoration(activity, layoutManager))
    (recyclerView.itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false

    viewModel.get(category)
      .subscribe {
        layoutManager.spanCount = it.gridColumnCount
        adapter.submitList(it.models)
      }
      .disposeOnDestroyView()
  }
}
