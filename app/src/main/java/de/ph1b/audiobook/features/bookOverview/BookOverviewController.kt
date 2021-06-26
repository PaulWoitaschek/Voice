package de.ph1b.audiobook.features.bookOverview

import android.content.Intent
import android.graphics.Color
import android.view.MenuItem
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.RouterTransaction
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import com.google.android.material.tabs.TabLayoutMediator
import de.paulwoitaschek.flowpref.Pref
import de.ph1b.audiobook.R
import de.ph1b.audiobook.common.pref.PrefKeys
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.databinding.BookOverviewBinding
import de.ph1b.audiobook.features.GalleryPicker
import de.ph1b.audiobook.features.ViewBindingController
import de.ph1b.audiobook.features.bookOverview.list.PagerOverviewAdapter
import de.ph1b.audiobook.features.bookPlaying.BookPlayController
import de.ph1b.audiobook.features.folderOverview.FolderOverviewController
import de.ph1b.audiobook.features.imagepicker.CoverFromInternetController
import de.ph1b.audiobook.features.settings.SettingsController
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.conductor.asTransaction
import de.ph1b.audiobook.misc.conductor.clearAfterDestroyView
import de.ph1b.audiobook.misc.conductor.clearAfterDestroyViewNullable
import de.ph1b.audiobook.uitools.BookChangeHandler
import de.ph1b.audiobook.uitools.PlayPauseDrawableSetter
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

/**
 * Showing the shelf of all the available books and provide a navigation to each book.
 */
class BookOverviewController : ViewBindingController<BookOverviewBinding>(BookOverviewBinding::inflate),
  EditCoverDialogController.Callback, EditBookBottomSheetController.Callback, ScreenSlideController.Callback,
  CoverFromInternetController.Callback
{
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
  private var adapter: PagerOverviewAdapter by clearAfterDestroyView()
  private var currentTapTarget by clearAfterDestroyViewNullable<TapTargetView>()
  private var useGrid = false

  private var tabLayoutMediator: TabLayoutMediator by clearAfterDestroyView()

  override fun BookOverviewBinding.onBindingCreated() {
    setupToolbar()
    setupFab()
    setupViewPager()
    setupTab()
    lifecycleScope.launch {
      viewModel.coverChanged.collect {
        ensureActive()
        onBookCoverChanged(it)
      }
    }
  }
  private fun BookOverviewBinding.setupTab() {
    tabLayoutMediator = TabLayoutMediator(tabLayout, viewPage) { tab, position ->
      if(adapter.itemCount>position)
      {
        viewPage.setCurrentItem(
          tab.position,
          true
        )
        tab.setText(adapter.getItemName(position))
      }
    }
    tabLayoutMediator.attach()
  }

  private fun setupViewPager() {
    adapter = PagerOverviewAdapter(this)
    binding.viewPage.adapter = adapter
  }

  private fun BookOverviewBinding.setupFab() {
    binding.fab.setOnClickListener { viewModel.playPause() }
    playPauseDrawableSetter = PlayPauseDrawableSetter(fab)
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

  override fun invokeBookSelectionCallback(book: Book) {
    currentBookIdPref.value = book.id
    val transaction = RouterTransaction.with(BookPlayController(book.id))
    val transition = BookChangeHandler()
    transition.transitionName = book.coverTransitionName
    transaction.pushChangeHandler(transition)
      .popChangeHandler(transition)
    router.pushController(transaction)
  }

  override fun invokeEditBookBottomSheetController(book: Book) {
    Timber.i("invokeEditBookBottomSheetController ${book.javaClass.simpleName}")
    EditBookBottomSheetController(this, book).showDialog(router)
  }

  private fun BookOverviewBinding.render(state: BookOverviewState, gridMenuItem: GridMenuItem) {
    Timber.i("render ${state.javaClass.simpleName}")

    when (state) {
      is BookOverviewState.Content -> {

        if(state.categoriesWithContents.size != adapter.itemCount) {
          adapter.updateItems(state.categoriesWithContents.keys)
        }

        hideNoFolderWarning()
        fab.isVisible = state.currentBookPresent

        useGrid = state.useGrid

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

  override fun onBookCoverChanged(bookId: UUID) {
    adapter.notifyItems()
  }

  override fun onInternetCoverRequested(book: Book) {
    router.pushController(CoverFromInternetController(book.id, this).asTransaction())
  }

  override fun onFileCoverRequested(book: Book) {
    galleryPicker.pick(book.id, this)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    binding.viewPage.adapter = null
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
}

private inline class GridMenuItem(val item: MenuItem)
