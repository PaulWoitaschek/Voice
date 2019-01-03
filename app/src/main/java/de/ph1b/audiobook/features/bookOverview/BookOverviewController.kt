package de.ph1b.audiobook.features.bookOverview

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bluelinelabs.conductor.RouterTransaction
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.features.BaseController
import de.ph1b.audiobook.features.bookCategory.BookCategoryController
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewAdapter
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewClick
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewHeaderModel
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewItem
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewItemDecoration
import de.ph1b.audiobook.features.bookPlaying.BookPlayController
import de.ph1b.audiobook.features.folderOverview.FolderOverviewController
import de.ph1b.audiobook.features.imagepicker.CoverFromInternetController
import de.ph1b.audiobook.features.settings.SettingsController
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.ElevateToolbarOnScroll
import de.ph1b.audiobook.misc.color
import de.ph1b.audiobook.misc.conductor.asTransaction
import de.ph1b.audiobook.misc.conductor.clearAfterDestroyView
import de.ph1b.audiobook.misc.conductor.clearAfterDestroyViewNullable
import de.ph1b.audiobook.misc.drawable
import de.ph1b.audiobook.misc.postedIfComputingLayout
import de.ph1b.audiobook.misc.tint
import de.ph1b.audiobook.misc.tinted
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.uitools.BookChangeHandler
import de.ph1b.audiobook.uitools.PlayPauseDrawableSetter
import kotlinx.android.synthetic.main.book_overview.*
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

private const val COVER_FROM_GALLERY = 1

/**
 * Showing the shelf of all the available books and provide a navigation to each book.
 */
class BookOverviewController : BaseController(),
  EditCoverDialogController.Callback, EditBookBottomSheetController.Callback,
  CoverFromInternetController.Callback {

  override val layoutRes = R.layout.book_overview

  init {
    appComponent.inject(this)
  }

  @field:[Inject Named(PrefKeys.CURRENT_BOOK)]
  lateinit var currentBookIdPref: Pref<UUID>

  @Inject
  lateinit var viewModel: BookOverviewViewModel

  private var playPauseDrawableSetter: PlayPauseDrawableSetter by clearAfterDestroyView()
  private var adapter: BookOverviewAdapter by clearAfterDestroyView()
  private var currentTapTarget by clearAfterDestroyViewNullable<TapTargetView>()
  private var menuBook: Book? = null
  private var useGrid = false
  private var columnCount = 1

  override fun onViewCreated() {
    val gridMenuItem = setupToolbar()
    setupFab()
    setupRecyclerView()

    viewModel.state()
      .subscribe { render(it, gridMenuItem) }
      .disposeOnDestroyView()
    viewModel.coverChanged
      .subscribe(::bookCoverChanged)
      .disposeOnDestroyView()
  }

  private fun setupFab() {
    fab.setOnClickListener { viewModel.playPause() }
    playPauseDrawableSetter = PlayPauseDrawableSetter(fab)
  }

  private fun setupRecyclerView() {
    recyclerView.setHasFixedSize(true)
    adapter = BookOverviewAdapter(
      bookClickListener = { book, clickType ->
        when (clickType) {
          BookOverviewClick.REGULAR -> invokeBookSelectionCallback(book)
          BookOverviewClick.MENU -> {
            EditBookBottomSheetController(this, book).showDialog(router)
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
          return if (isHeader) columnCount else 1
        }
      }
    }
    val listDecoration = BookOverviewItemDecoration(activity, layoutManager)
    recyclerView.addItemDecoration(listDecoration)
    recyclerView.layoutManager = layoutManager
    recyclerView.addOnScrollListener(ElevateToolbarOnScroll(toolbar))
  }

  private fun setupToolbar(): GridMenuItem {
    toolbar.inflateMenu(R.menu.book_overview)
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
    toolbar.tint()

    return GridMenuItem(toolbar.menu.findItem(R.id.toggleGrid))
  }

  private fun toFolderOverview() {
    val controller = FolderOverviewController()
    router.pushController(controller.asTransaction())
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      COVER_FROM_GALLERY -> {
        if (resultCode == Activity.RESULT_OK) {
          val imageUri = data?.data
          val book = menuBook
          if (imageUri == null || book == null) {
            return
          }

          EditCoverDialogController(this, book, imageUri).showDialog(router)
        }
      }
      else -> super.onActivityResult(requestCode, resultCode, data)
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

  private fun render(state: BookOverviewState, gridMenuItem: GridMenuItem) {
    Timber.i("render ${state.javaClass.simpleName}")
    when (state) {
      is BookOverviewState.Content -> {
        val content = ArrayList<BookOverviewItem>().apply {
          state.categoriesWithContents.forEach { (category, content) ->
            add(BookOverviewHeaderModel(category, content.hasMore))
            addAll(content.books)
          }
        }
        adapter.submitList(content)
        fab.isVisible = state.currentBookPresent
        showPlaying(state.playing)

        useGrid = state.useGrid
        columnCount = state.columnCount
        val lm = recyclerView.layoutManager as GridLayoutManager
        lm.spanCount = columnCount
        gridMenuItem.item.apply {
          val useGrid = state.useGrid
          setTitle(if (useGrid) R.string.layout_list else R.string.layout_grid)
          val drawableRes = if (useGrid) R.drawable.ic_view_list else R.drawable.ic_view_grid
          val tintColor = activity.color(R.color.toolbarIconColor)
          icon = activity.drawable(drawableRes).tinted(tintColor)
        }
      }
      is BookOverviewState.NoFolderSet -> {
        showNoFolderWarning()
      }
    }
    loadingProgress.isVisible = state == BookOverviewState.Loading
    gridMenuItem.item.isVisible = state != BookOverviewState.Loading
  }

  private fun showPlaying(playing: Boolean) {
    Timber.i("Called showPlaying $playing")
    val laidOut = fab.isLaidOut
    playPauseDrawableSetter.setPlaying(playing = playing, animated = laidOut)
  }

  /** Show a warning that no audiobook folder was chosen */
  private fun showNoFolderWarning() {
    if (currentTapTarget?.isVisible == true)
      return

    val target = TapTarget
      .forToolbarMenuItem(
        toolbar,
        R.id.library,
        getString(R.string.onboarding_title),
        getString(R.string.onboarding_content)
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

  private fun bookCoverChanged(bookId: UUID) {
    // there is an issue where notifyDataSetChanges throws:
    // java.lang.IllegalStateException: Cannot call this method while RecyclerView is computing a layout or scrolling
    recyclerView.postedIfComputingLayout {
      adapter.reloadBookCover(bookId)
    }
  }

  override fun onBookCoverChanged(bookId: UUID) {
    recyclerView.postedIfComputingLayout {
      adapter.reloadBookCover(bookId)
    }
  }

  override fun onInternetCoverRequested(book: Book) {
    router.pushController(CoverFromInternetController(book.id, this).asTransaction())
  }

  override fun onFileCoverRequested(book: Book) {
    menuBook = book
    val galleryPickerIntent = Intent(Intent.ACTION_PICK)
    galleryPickerIntent.type = "image/*"
    startActivityForResult(galleryPickerIntent, COVER_FROM_GALLERY)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    recyclerView.adapter = null
  }

  override fun onAttach(view: View) {
    super.onAttach(view)
    viewModel.attach()
  }
}

private inline class GridMenuItem(val item: MenuItem)
