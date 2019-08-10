package de.ph1b.audiobook.features.bookCategory

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.features.BaseController
import de.ph1b.audiobook.features.GalleryPicker
import de.ph1b.audiobook.features.bookOverview.EditBookBottomSheetController
import de.ph1b.audiobook.features.bookOverview.EditCoverDialogController
import de.ph1b.audiobook.features.bookOverview.list.BookComparator
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewClick
import de.ph1b.audiobook.features.bookOverview.list.header.BookOverviewCategory
import de.ph1b.audiobook.features.bookPlaying.BookPlayController
import de.ph1b.audiobook.features.imagepicker.CoverFromInternetController
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.conductor.asTransaction
import de.ph1b.audiobook.misc.conductor.clearAfterDestroyView
import de.ph1b.audiobook.misc.conductor.popOrBack
import de.ph1b.audiobook.misc.tint
import de.ph1b.audiobook.uitools.BookChangeHandler
import kotlinx.android.synthetic.main.book_category.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

private const val NI_CATEGORY = "ni#category"

class BookCategoryController(bundle: Bundle) : BaseController(bundle), EditBookBottomSheetController.Callback,
  CoverFromInternetController.Callback, EditCoverDialogController.Callback {

  @Inject
  lateinit var repo: BookRepository
  @Inject
  lateinit var viewModel: BookCategoryViewModel
  @Inject
  lateinit var galleryPicker: GalleryPicker

  constructor(category: BookOverviewCategory) : this(Bundle().apply {
    putSerializable(NI_CATEGORY, category)
  })

  private val category = bundle.getSerializable(NI_CATEGORY) as BookOverviewCategory
  private var adapter by clearAfterDestroyView<BookCategoryAdapter>()

  init {
    appComponent.inject(this)
    viewModel.category = category
  }

  override val layoutRes = R.layout.book_category

  override fun onViewCreated() {
    toolbar.setTitle(category.nameRes)
    toolbar.inflateMenu(R.menu.book_category)
    toolbar.setOnMenuItemClickListener {
      when (it.itemId) {
        R.id.sort -> {
          showSortingPopup()
          true
        }
        else -> false
      }
    }
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
          EditBookBottomSheetController(this, book).showDialog(router)
        }
      }
    }.also { adapter = it }
    recyclerView.adapter = adapter
    val layoutManager = GridLayoutManager(activity, 1)
    recyclerView.layoutManager = layoutManager
    recyclerView.addItemDecoration(BookCategoryItemDecoration(activity, layoutManager))
    (recyclerView.itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false

    viewModel.get()
      .subscribe {
        layoutManager.spanCount = it.gridColumnCount
        adapter.submitList(it.models)
      }
      .disposeOnDestroyView()
  }

  private fun showSortingPopup() {
    val anchor = toolbar.findViewById<View>(R.id.sort)
    PopupMenu(activity, anchor).apply {
      inflate(R.menu.sort_menu)
      val bookSorting = viewModel.bookSorting()
      menu.findItem(bookSorting.menuId).isChecked = true
      setOnMenuItemClickListener { menuItem ->
        val itemId = menuItem.itemId
        val comparator = BookComparator.values().find { it.menuId == itemId }
        if (comparator != null) {
          viewModel.sort(comparator)
          true
        } else {
          false
        }
      }
      show()
    }
  }

  override fun onInternetCoverRequested(book: Book) {
    router.pushController(CoverFromInternetController(book.id, this).asTransaction())
  }

  override fun onBookCoverChanged(bookId: UUID) {
    adapter.notifyCoverChanged(bookId)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    val arguments = galleryPicker.parse(requestCode, resultCode, data)
    if (arguments != null) {
      EditCoverDialogController(this, arguments).showDialog(router)
    }
  }

  override fun onFileCoverRequested(book: Book) {
    galleryPicker.pick(book.id, this)
  }

  override fun onFileDeletionRequested(book: Book) {
    GlobalScope.launch (Dispatchers.IO) {
      val bookContent = book.content
      val currentFile = bookContent.currentFile
      currentFile.delete()
      repo.hideBook(book.id)
    }
  }
}

private val BookComparator.menuId: Int
  get() = when (this) {
    BookComparator.BY_LAST_PLAYED -> R.id.byLastPlayed
    BookComparator.BY_NAME -> R.id.byName
    BookComparator.BY_DATE_ADDED -> R.id.byAdded
  }
