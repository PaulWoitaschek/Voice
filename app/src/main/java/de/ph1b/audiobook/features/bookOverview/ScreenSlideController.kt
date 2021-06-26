package de.ph1b.audiobook.features.bookOverview

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import com.bluelinelabs.conductor.Controller
import de.paulwoitaschek.flowpref.Pref
import de.ph1b.audiobook.R
import de.ph1b.audiobook.common.pref.PrefKeys
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.BookComparator
import de.ph1b.audiobook.databinding.FragmentScreenSlideBinding
import de.ph1b.audiobook.features.ViewBindingController
import de.ph1b.audiobook.features.bookCategory.BookCategoryAdapter
import de.ph1b.audiobook.features.bookCategory.BookCategoryItemDecoration
import de.ph1b.audiobook.features.bookCategory.BookCategoryViewModel
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewClick
import de.ph1b.audiobook.features.bookOverview.list.header.BookOverviewCategory
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.conductor.clearAfterDestroyView
import de.ph1b.audiobook.misc.conductor.popOrBack
import de.ph1b.audiobook.misc.postedIfComputingLayout
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

class ScreenSlideController(bundle: Bundle) : ViewBindingController<FragmentScreenSlideBinding>(bundle, FragmentScreenSlideBinding::inflate)
{
  private val bookOverviewCategory = bundle.getSerializable(NI_CATEGORY) as BookOverviewCategory
  init {
    appComponent.inject(this)
    viewModel.category = bookOverviewCategory
  }
  private var adapter: BookCategoryAdapter by clearAfterDestroyView()

  @field:[Inject Named(PrefKeys.CURRENT_BOOK)]
  lateinit var currentBookIdPref: Pref<UUID>

  @Inject
  lateinit var viewModel: BookCategoryViewModel

  override fun FragmentScreenSlideBinding.onBindingCreated() {
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
    toolbar.setNavigationOnClickListener { popOrBack() }

    val adapter = BookCategoryAdapter { book, clickType ->
      when (clickType) {
        BookOverviewClick.REGULAR -> callback().invokeBookSelectionCallback(book)
        BookOverviewClick.MENU -> callback().invokeEditBookBottomSheetController(book)
      }
    }.also { adapter = it }
    recyclerView.adapter = adapter
    val layoutManager = GridLayoutManager(activity, 1)
    recyclerView.layoutManager = layoutManager
    recyclerView.addItemDecoration(BookCategoryItemDecoration(activity!!, layoutManager))
    (recyclerView.itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false
    lifecycleScope.launch {
      viewModel.get()
        .collect {
          layoutManager.spanCount = it.gridColumnCount
          adapter.submitList(it.models)
        }
    }
  }

  private fun FragmentScreenSlideBinding.showSortingPopup() {
    val anchor = toolbar.findViewById<View>(R.id.sort)
    PopupMenu(activity!!, anchor).apply {
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

  private fun FragmentScreenSlideBinding.bookCoverChanged(bookId: UUID) {
    // there is an issue where notifyDataSetChanges throws:
    // java.lang.IllegalStateException: Cannot call this method while RecyclerView is computing a layout or scrolling
    recyclerView.postedIfComputingLayout {
      adapter.notifyCoverChanged(bookId)
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    binding.recyclerView.adapter = null
  }

  override fun FragmentScreenSlideBinding.onAttach() {

  }

  private fun callback() = targetController as Callback

  companion object {
    private const val NI_CATEGORY = "ni#bookcategory"
    operator fun <T> invoke(
      target: T,
      category: BookOverviewCategory
    ): ScreenSlideController where T : Controller {
      val bundle = Bundle().apply { putSerializable(NI_CATEGORY, category)}
      return ScreenSlideController(bundle).apply {
        targetController = target
      }
    }
  }

  interface Callback {
    fun invokeBookSelectionCallback(book: Book)
    fun invokeEditBookBottomSheetController(book: Book)
  }
}
private val BookComparator.menuId: Int
  get() = when (this) {
    BookComparator.BY_LAST_PLAYED -> R.id.byLastPlayed
    BookComparator.BY_NAME -> R.id.byName
    BookComparator.BY_DATE_ADDED -> R.id.byAdded
  }
