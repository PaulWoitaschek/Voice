package de.ph1b.audiobook.features.bookOverview

import android.content.Intent
import android.graphics.Color
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.datastore.core.DataStore
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bluelinelabs.conductor.RouterTransaction
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import de.ph1b.audiobook.R
import de.ph1b.audiobook.common.pref.CurrentBook
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.databinding.BookOverviewBinding
import de.ph1b.audiobook.features.GalleryPicker
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewAdapter
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewClick
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewHeaderModel
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewItemDecoration
import de.ph1b.audiobook.features.imagepicker.CoverFromInternetController
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.conductor.asTransaction
import de.ph1b.audiobook.misc.conductor.clearAfterDestroyViewNullable
import de.ph1b.audiobook.uitools.BookChangeHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import voice.common.PlayPauseDrawableSetter
import voice.common.conductor.ViewBindingController
import voice.common.conductor.clearAfterDestroyView
import voice.folderPicker.FolderPickerController
import voice.playbackScreen.BookPlayController
import voice.settings.SettingsController
import javax.inject.Inject
import kotlin.collections.component1
import kotlin.collections.component2

/**
 * Showing the shelf of all the available books and provide a navigation to each book.
 */
class BookOverviewController :
  ViewBindingController<BookOverviewBinding>(BookOverviewBinding::inflate),
  EditBookBottomSheetController.Callback {

  init {
    appComponent.inject(this)
  }

  @field:[Inject CurrentBook]
  lateinit var currentBookIdPref: DataStore<Book.Id?>

  @Inject
  lateinit var viewModel: BookOverviewViewModel

  @Inject
  lateinit var galleryPicker: GalleryPicker

  private var playPauseDrawableSetter: PlayPauseDrawableSetter by clearAfterDestroyView()
  private var adapter: BookOverviewAdapter by clearAfterDestroyView()
  private var currentTapTarget by clearAfterDestroyViewNullable<TapTargetView>()
  private var useGrid = false

  override fun BookOverviewBinding.onBindingCreated() {
    setupToolbar()
    setupFab()
    setupRecyclerView()
  }

  private fun BookOverviewBinding.setupFab() {
    binding.fab.setOnClickListener { viewModel.playPause() }
    playPauseDrawableSetter = PlayPauseDrawableSetter(fab)
  }

  private fun BookOverviewBinding.setupRecyclerView() {
    adapter = BookOverviewAdapter(
      bookClickListener = { bookId, clickType ->
        when (clickType) {
          BookOverviewClick.REGULAR -> invokeBookSelectionCallback(bookId)
          BookOverviewClick.MENU -> {
            EditBookBottomSheetController(this@BookOverviewController, bookId).showDialog(router)
          }
        }
      },
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
          router.pushController(SettingsController().asTransaction())
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

  private fun BookOverviewBinding.gridMenuItem(): MenuItem = toolbar.menu.findItem(R.id.toggleGrid)

  private fun toFolderOverview() {
    val controller = FolderPickerController()
    router.pushController(controller.asTransaction())
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    val arguments = galleryPicker.parse(requestCode, resultCode, data)
    if (arguments != null) {
      EditCoverDialogController(arguments).showDialog(router)
    }
  }

  private fun invokeBookSelectionCallback(bookId: Book.Id) {
    runBlocking {
      currentBookIdPref.updateData { bookId }
    }
    val transaction = RouterTransaction.with(BookPlayController(bookId))
    val transition = BookChangeHandler()
    transition.transitionName = bookId.toString()
    transaction.pushChangeHandler(transition)
      .popChangeHandler(transition)
    router.pushController(transaction)
  }

  private fun BookOverviewBinding.render(state: BookOverviewState, gridMenuItem: MenuItem) {
    Timber.i("render ${state.javaClass.simpleName}")
    val adapterContent = when (state) {
      is BookOverviewState.Content -> buildList {
        state.categoriesWithContents.forEach { (category, content) ->
          add(BookOverviewHeaderModel(category))
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
        gridMenuItem.apply {
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
    gridMenuItem.isVisible = state != BookOverviewState.Loading
  }

  private fun showPlaying(playing: Boolean) {
    Timber.i("Called showPlaying $playing")
    playPauseDrawableSetter.setPlaying(playing = playing)
  }

  private fun hideNoFolderWarning() {
    currentTapTarget?.dismiss(false)
    currentTapTarget = null
  }

  /** Show a warning that no audiobook folder was chosen */
  private fun BookOverviewBinding.showNoFolderWarning() {
    if (currentTapTarget != null) {
      return
    }

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
    currentTapTarget = TapTargetView.showFor(
      activity, target,
      object : TapTargetView.Listener() {
        override fun onTargetClick(view: TapTargetView?) {
          super.onTargetClick(view)
          toFolderOverview()
        }
      }
    )
  }

  override fun onInternetCoverRequested(book: Book.Id) {
    router.pushController(
      CoverFromInternetController(book)
        .asTransaction()
    )
  }

  override fun onFileCoverRequested(book: Book.Id) {
    galleryPicker.pick(book, this)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    binding.recyclerView.adapter = null
  }

  override fun BookOverviewBinding.onAttach() {
    viewModel.attach()
    val gridMenuItem = gridMenuItem()
    lifecycleScope.launch {
      viewModel.state()
        .collect {
          render(it, gridMenuItem)
        }
    }
  }

  override fun onDetach(view: View) {
    super.onDetach(view)
    currentTapTarget?.dismiss(false)
    currentTapTarget = null
  }
}
