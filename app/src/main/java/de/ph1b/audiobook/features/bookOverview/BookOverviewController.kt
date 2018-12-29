package de.ph1b.audiobook.features.bookOverview

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bluelinelabs.conductor.RouterTransaction
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.features.BaseController
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewAdapter
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewClick
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewHeaderModel
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewItem
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewItemDecoration
import de.ph1b.audiobook.features.bookPlaying.BookPlayController
import de.ph1b.audiobook.features.folderOverview.FolderOverviewController
import de.ph1b.audiobook.features.imagepicker.CoverFromInternetController
import de.ph1b.audiobook.features.settings.SettingsController
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.misc.ElevateToolbarOnScroll
import de.ph1b.audiobook.misc.conductor.asTransaction
import de.ph1b.audiobook.misc.conductor.clearAfterDestroyView
import de.ph1b.audiobook.misc.conductor.clearAfterDestroyViewNullable
import de.ph1b.audiobook.misc.postedIfComputingLayout
import de.ph1b.audiobook.misc.tint
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
    App.component.inject(this)
  }

  @field:[Inject Named(PrefKeys.CURRENT_BOOK)]
  lateinit var currentBookIdPref: Pref<UUID>

  @Inject
  lateinit var viewModel: BookOverviewViewModel

  private var playPauseDrawableSetter: PlayPauseDrawableSetter by clearAfterDestroyView()
  private var adapter: BookOverviewAdapter by clearAfterDestroyView()
  private var currentTapTarget by clearAfterDestroyViewNullable<TapTargetView>()
  private var menuBook: Book? = null

  override fun onViewCreated() {
    setupBottomAppBar()
    setupFab()
    setupRecyclerView()

    viewModel.state
      .subscribe(::render)
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
    adapter = BookOverviewAdapter { book, clickType ->
      when (clickType) {
        BookOverviewClick.REGULAR -> invokeBookSelectionCallback(book)
        BookOverviewClick.MENU -> {
          EditBookBottomSheetController(this, book).showDialog(router)
        }
      }
    }
    recyclerView.adapter = adapter
    // without this the item would blink on every change
    val anim = recyclerView.itemAnimator as SimpleItemAnimator
    anim.supportsChangeAnimations = false
    val listDecoration = BookOverviewItemDecoration(activity)
    recyclerView.addItemDecoration(listDecoration)
    recyclerView.layoutManager = LinearLayoutManager(activity)
    recyclerView.addOnScrollListener(ElevateToolbarOnScroll(toolbar))
  }

  private fun setupBottomAppBar() {
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
        else -> false
      }
    }
    toolbar.tint()
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

  private fun render(state: BookOverviewState) {
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
      }
      is BookOverviewState.NoFolderSet -> {
        showNoFolderWarning()
      }
    }
    loadingProgress.isVisible = state == BookOverviewState.Loading
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
