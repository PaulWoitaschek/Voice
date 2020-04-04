package de.ph1b.audiobook.features.bookOverview

import android.content.Intent
import android.graphics.Color
import android.view.MenuItem
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bluelinelabs.conductor.RouterTransaction
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import de.paulwoitaschek.flowpref.Pref
import de.ph1b.audiobook.R
import de.ph1b.audiobook.common.pref.PrefKeys
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.databinding.BookOverviewBinding
import de.ph1b.audiobook.features.GalleryPicker
import de.ph1b.audiobook.features.ViewBindingController
import de.ph1b.audiobook.features.bookCategory.BookCategoryController
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewAdapter
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewClick
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewHeaderModel
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewItemDecoration
import de.ph1b.audiobook.features.bookPlaying.BookPlayController
import de.ph1b.audiobook.features.folderOverview.FolderOverviewController
import de.ph1b.audiobook.features.imagepicker.CoverFromInternetController
import de.ph1b.audiobook.features.settings.SettingsController
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.conductor.asTransaction
import de.ph1b.audiobook.misc.conductor.clearAfterDestroyView
import de.ph1b.audiobook.misc.conductor.clearAfterDestroyViewNullable
import de.ph1b.audiobook.misc.postedIfComputingLayout
import de.ph1b.audiobook.uitools.BookChangeHandler
import de.ph1b.audiobook.uitools.PlayPauseDrawableSetter
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import kotlin.collections.component1
import kotlin.collections.component2

/**
 * Showing the shelf of all the available books and provide a navigation to each book.
 */
class BookOverviewController : ViewBindingController<BookOverviewBinding>(BookOverviewBinding::inflate),
  EditCoverDialogController.Callback, EditBookBottomSheetController.Callback,
  CoverFromInternetController.Callback {

  init {
    appComponent.inject(this)
  }

  @field:[Inject Named(PrefKeys.CURRENT_BOOK)]
  lateinit var currentBookIdPref: Pref<UUID>

  @Inject
  lateinit var viewModel: BookOverviewViewModel

  @Inject
  lateinit var galleryPicker: GalleryPicker

  private var playPauseDrawableSetter: PlayPauseDrawableSetter by clearAfterDestroyView()
  private var adapter: BookOverviewAdapter by clearAfterDestroyView()
  private var currentTapTarget by clearAfterDestroyViewNullable<TapTargetView>()
  private var useGrid = false

  override fun onBindingCreated(binding: BookOverviewBinding) {
    binding.apply {
      setupToolbar()
      setupFab()
      setupRecyclerView()
      lifecycleScope.launch {
        viewModel.coverChanged.collect {
          ensureActive()
          bookCoverChanged(it)
        }
      }
    }
  }

  private fun BookOverviewBinding.setupFab() {
    binding.fab.setOnClickListener { viewModel.playPause() }
    playPauseDrawableSetter = PlayPauseDrawableSetter(fab)
  }

  private fun BookOverviewBinding.setupRecyclerView() {
    recyclerView.setHasFixedSize(true)
    adapter = BookOverviewAdapter(
      bookClickListener = { book, clickType ->
        when (clickType) {
          BookOverviewClick.REGULAR -> invokeBookSelectionCallback(book)
          BookOverviewClick.MENU -> {
            EditBookBottomSheetController(this@BookOverviewController, book).showDialog(router)
          }
        }
      },
      openCategoryListener = { category ->
        Timber.i("open $category")
        router.pushController(BookCategoryController(category).asTransaction())
      }
    )
    recyclerView.adapter = adapter
    // without this the item would blink on every change
    val anim = recyclerView.itemAnimator as SimpleItemAnimator
    anim.supportsChangeAnimations = false
    val layoutManager = GridLayoutManager(activity, 1).apply {
      spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
          if (position == -1) {
            return 1
          }
          val isHeader = adapter.itemAtPositionIsHeader(position)
          return if (isHeader) spanCount else 1
        }
      }
    }
    val listDecoration = BookOverviewItemDecoration(activity!!, layoutManager)
    recyclerView.addItemDecoration(listDecoration)
    recyclerView.layoutManager = layoutManager
  }

  private fun BookOverviewBinding.setupToolbar() {
    toolbar.setOnMenuItemClickListener {
      when (it.itemId) {
        R.id.action_settings -> {
          val transaction = SettingsController().asTransaction()
          router.pushController(transaction)
          true
        }
        R.id.library -> {
          toFolderOverview()
          true
        }
        R.id.toggleGrid -> {
          viewModel.useGrid(!useGrid)
          true
        }
        else -> false
      }
    }
  }

  private fun BookOverviewBinding.gridMenuItem(): GridMenuItem = GridMenuItem(toolbar.menu.findItem(R.id.toggleGrid))

  private fun toFolderOverview() {
    val controller = FolderOverviewController()
    router.pushController(controller.asTransaction())
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    val arguments = galleryPicker.parse(requestCode, resultCode, data)
    if (arguments != null) {
      EditCoverDialogController(this, arguments).showDialog(router)
    }
  }

  private fun invokeBookSelectionCallback(book: Book) {
    currentBookIdPref.value = book.id
    val transaction = RouterTransaction.with(BookPlayController(book.id))
    val transition = BookChangeHandler()
    transition.transitionName = book.coverTransitionName
    transaction.pushChangeHandler(transition)
      .popChangeHandler(transition)
    router.pushController(transaction)
  }

  private fun BookOverviewBinding.render(state: BookOverviewState, gridMenuItem: GridMenuItem) {
    Timber.i("render ${state.javaClass.simpleName}")
    val adapterContent = when (state) {
      is BookOverviewState.Content -> buildList {
        state.categoriesWithContents.forEach { (category, content) ->
          add(BookOverviewHeaderModel(category, content.hasMore))
          addAll(content.books)
        }
      }
      BookOverviewState.Loading, BookOverviewState.NoFolderSet -> emptyList()
    }
    adapter.submitList(adapterContent)

    when (state) {
      is BookOverviewState.Content -> {
        hideNoFolderWarning()
        fab.isVisible = state.currentBookPresent

        useGrid = state.useGrid
        val lm = recyclerView.layoutManager as GridLayoutManager
        lm.spanCount = state.columnCount

        showPlaying(state.playing)
        gridMenuItem.item.apply {
          val useGrid = state.useGrid
          setTitle(if (useGrid) R.string.layout_list else R.string.layout_grid)
          val drawableRes = if (useGrid) R.drawable.ic_view_list else R.drawable.ic_view_grid
          setIcon(drawableRes)
        }
      }
      BookOverviewState.Loading -> {
        hideNoFolderWarning()
        fab.isVisible = false
      }
      BookOverviewState.NoFolderSet -> {
        showNoFolderWarning()
        fab.isVisible = false
      }
    }

    loadingProgress.isVisible = state == BookOverviewState.Loading
    gridMenuItem.item.isVisible = state != BookOverviewState.Loading
  }

  private fun showPlaying(playing: Boolean) {
    Timber.i("Called showPlaying $playing")
    playPauseDrawableSetter.setPlaying(playing = playing)
  }

  private fun hideNoFolderWarning() {
    val currentTapTarget = currentTapTarget ?: return
    if (currentTapTarget.isVisible) {
      currentTapTarget.dismiss(false)
    }
    this.currentTapTarget = null
  }

  /** Show a warning that no audiobook folder was chosen */
  private fun BookOverviewBinding.showNoFolderWarning() {
    if (currentTapTarget?.isVisible == true)
      return

    val target = TapTarget
      .forToolbarMenuItem(
        toolbar,
        R.id.library,
        activity!!.getString(R.string.onboarding_title),
        activity!!.getString(R.string.onboarding_content)
      )
      .cancelable(false)
      .tintTarget(false)
      .outerCircleColor(R.color.accentDark)
      .descriptionTextColorInt(Color.WHITE)
      .textColorInt(Color.WHITE)
      .targetCircleColorInt(Color.BLACK)
      .transparentTarget(true)
    currentTapTarget = TapTargetView.showFor(activity, target, object : TapTargetView.Listener() {
      override fun onTargetClick(view: TapTargetView?) {
        super.onTargetClick(view)
        toFolderOverview()
      }
    })
  }

  private fun BookOverviewBinding.bookCoverChanged(bookId: UUID) {
    // there is an issue where notifyDataSetChanges throws:
    // java.lang.IllegalStateException: Cannot call this method while RecyclerView is computing a layout or scrolling
    recyclerView.postedIfComputingLayout {
      adapter.reloadBookCover(bookId)
    }
  }

  override fun onBookCoverChanged(bookId: UUID) {
    binding.recyclerView.postedIfComputingLayout {
      adapter.reloadBookCover(bookId)
    }
  }

  override fun onInternetCoverRequested(book: Book) {
    router.pushController(CoverFromInternetController(book.id, this).asTransaction())
  }

  override fun onFileCoverRequested(book: Book) {
    galleryPicker.pick(book.id, this)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    binding.recyclerView.adapter = null
  }

  override fun onAttach(binding: BookOverviewBinding) {
    super.onAttach(binding)
    binding.apply {
      viewModel.attach()
      val gridMenuItem = gridMenuItem()
      lifecycleScope.launch {
        viewModel.state()
          .collect {
            render(it, gridMenuItem)
          }
      }
    }
  }
}

private inline class GridMenuItem(val item: MenuItem)
