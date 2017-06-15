package de.ph1b.audiobook.features.bookOverview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.support.annotation.DrawableRes
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentTransaction
import android.support.v7.widget.*
import android.view.MenuItem
import android.view.View
import com.bluelinelabs.conductor.RouterTransaction
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.R
import de.ph1b.audiobook.databinding.BookShelfBinding
import de.ph1b.audiobook.features.bookPlaying.BookPlayController
import de.ph1b.audiobook.features.imagepicker.ImagePickerController
import de.ph1b.audiobook.features.settings.SettingsController
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.*
import de.ph1b.audiobook.mvp.MvpController
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.uitools.BookTransition
import de.ph1b.audiobook.uitools.PlayPauseDrawable
import de.ph1b.audiobook.uitools.visible
import i
import w
import javax.inject.Inject

/**
 * Showing the shelf of all the available books and provide a navigation to each book.
 *
 * @author Paul Woitaschek
 */
class BookShelfController : MvpController<BookShelfController, BookShelfPresenter, BookShelfBinding>(), EditCoverDialogFragment.Callback, EditBookBottomSheet.Callback {

  override fun createPresenter() = App.component.bookShelfPresenter
  override val layoutRes = R.layout.book_shelf

  private val COVER_FROM_GALLERY = 1

  override fun provideView() = this

  init {
    App.component.inject(this)
  }

  @Inject lateinit var prefs: PrefsManager

  private val playPauseDrawable = PlayPauseDrawable()
  private lateinit var adapter: BookShelfAdapter
  private lateinit var listDecoration: RecyclerView.ItemDecoration
  private lateinit var gridLayoutManager: GridLayoutManager
  private lateinit var linearLayoutManager: RecyclerView.LayoutManager
  private var menuBook: Book? = null
  private var pendingTransaction: FragmentTransaction? = null
  private var firstPlayStateUpdate = true
  private var currentBook: Book? = null
  private val positionResolver = GridPositionResolver()

  private lateinit var currentPlaying: MenuItem

  override fun onBindingCreated(binding: BookShelfBinding) {
    setupFab()
    setupRecyclerView()
    setupToolbar()
  }

  private fun setupFab() {
    binding.fab.setIconDrawable(playPauseDrawable)
    binding.fab.setOnClickListener { presenter.playPauseRequested() }
  }

  private fun setupRecyclerView() {
    binding.recyclerView.setHasFixedSize(true)
    adapter = BookShelfAdapter(activity) { book, clickType ->
      if (clickType == BookShelfAdapter.ClickType.REGULAR) {
        invokeBookSelectionCallback(book.id)
      } else {
        EditBookBottomSheet.newInstance(this, book).show(fragmentManager, "editBottomSheet")
      }
    }
    binding.recyclerView.adapter = adapter
    // without this the item would blink on every change
    val anim = binding.recyclerView.itemAnimator as SimpleItemAnimator
    anim.supportsChangeAnimations = false
    listDecoration = DividerItemDecoration(activity, DividerItemDecoration.VERTICAL)
    gridLayoutManager = GridLayoutManager(activity, amountOfColumns())
    linearLayoutManager = LinearLayoutManager(activity)

    binding.recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
      val twoDp = activity.dpToPx(2F)
      override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        val count = state.itemCount
        val isGrid = prefs.displayMode.value == DisplayMode.GRID
        if (position >= count) {
          w { "position=$position is >= count=$count. Skipping decoration" }
          return
        }
        positionResolver.prepare(position = position, count = count, columnCount = if (isGrid) amountOfColumns() else 1)

        if (isGrid) {
          outRect.left = if (positionResolver.isLeft()) 0 else Math.round(twoDp)
          outRect.right = if (positionResolver.isRight()) 0 else Math.round(twoDp)
          outRect.top = Math.round(if (positionResolver.isTop()) 2 * twoDp else twoDp)
          outRect.bottom = Math.round(if (positionResolver.isBottom()) activity.dpToPx(104F) else twoDp)
        } else {
          outRect.bottom = if (positionResolver.isBottom()) Math.round(activity.dpToPx(104F)) else 0
        }
      }
    })

    binding.scroller.attachTo(binding.recyclerView)

    updateDisplayMode()
  }

  private fun setupToolbar() {
    binding.toolbar.inflateMenu(R.menu.book_shelf)
    val menu = binding.toolbar.menu

    currentPlaying = menu.findItem(R.id.action_current)

    // sets the grid / list toggle icon
    val displayModeItem = menu.findItem(R.id.action_change_layout)
    displayModeItem.setIcon((!prefs.displayMode.value).icon)

    binding.toolbar.title = getString(R.string.app_name)

    binding.toolbar.setOnMenuItemClickListener {
      when (it.itemId) {
        R.id.action_settings -> {
          val transaction = SettingsController().asTransaction()
          router.pushController(transaction)
          true
        }
        R.id.action_current -> {
          invokeBookSelectionCallback(prefs.currentBookId.value)
          true
        }
        R.id.action_change_layout -> {
          prefs.displayMode.value = !prefs.displayMode.value
          updateDisplayMode()
          true
        }
        else -> false
      }
    }
  }

  override fun onActivityResumed(activity: Activity) {
    super.onActivityResumed(activity)

    pendingTransaction?.commit()
    pendingTransaction = null
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

          @SuppressLint("CommitTransaction")
          pendingTransaction = fragmentManager.beginTransaction()
              .add(EditCoverDialogFragment.newInstance(this, book, imageUri),
                  EditCoverDialogFragment.TAG)
        }
      }
      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  // Returns the amount of columns the main-grid will need
  private fun amountOfColumns(): Int {
    val r = binding.recyclerView.resources
    val displayMetrics = r.displayMetrics
    val widthPx = displayMetrics.widthPixels.toFloat()
    val desiredPx = r.getDimensionPixelSize(R.dimen.desired_medium_cover).toFloat()
    val columns = Math.round(widthPx / desiredPx)
    return Math.max(columns, 2)
  }

  private fun updateDisplayMode() {
    val defaultDisplayMode = prefs.displayMode.value
    if (defaultDisplayMode == DisplayMode.GRID) {
      binding.recyclerView.removeItemDecoration(listDecoration)
      binding.recyclerView.layoutManager = gridLayoutManager
    } else {
      binding.recyclerView.addItemDecoration(listDecoration, 0)
      binding.recyclerView.layoutManager = linearLayoutManager
    }
    adapter.displayMode = defaultDisplayMode
  }

  private fun invokeBookSelectionCallback(bookId: Long) {
    prefs.currentBookId.value = bookId

    val viewHolder = binding.recyclerView.findViewHolderForItemId(bookId) as BookShelfAdapter.BaseViewHolder?
    val transaction = RouterTransaction.with(BookPlayController(bookId))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      val transition = BookTransition()
      if (viewHolder != null) {
        val transitionName = viewHolder.coverView.supportTransitionName
        transition.transitionName = transitionName
      }
      transaction.pushChangeHandler(transition)
          .popChangeHandler(transition)
    }
    router.pushController(transaction)
  }

  /** Display a new set of books */
  fun displayNewBooks(books: List<Book>) {
    i { "${books.size} displayNewBooks" }
    adapter.newDataSet(books)
  }

  /** The book marked as current was changed. Updates the adapter and fab accordingly. */
  fun updateCurrentBook(currentBook: Book?) {
    i { "updateCurrentBook: ${currentBook?.name}" }
    this.currentBook = currentBook

    for (i in 0..adapter.itemCount - 1) {
      val itemId = adapter.getItemId(i)
      val vh = binding.recyclerView.findViewHolderForItemId(itemId) as BookShelfAdapter.BaseViewHolder?
      if (itemId == currentBook?.id || (vh != null && vh.indicatorVisible)) {
        adapter.notifyItemChanged(i)
      }
    }

    binding.fab.visible = currentBook != null

    currentPlaying.isVisible = currentBook != null
  }

  /** Sets the fab icon correctly accordingly to the new play state. */
  fun showPlaying(playing: Boolean) {
    i { "Called showPlaying $playing" }
    if (playing) {
      playPauseDrawable.transformToPause(!firstPlayStateUpdate)
    } else {
      playPauseDrawable.transformToPlay(!firstPlayStateUpdate)
    }
    firstPlayStateUpdate = false
  }

  /** Show a warning that no audiobook folder was chosen */
  fun showNoFolderWarning() {
    // show dialog if no folders are set
    val noFolderWarningIsShowing = (fragmentManager.findFragmentByTag(FM_NO_FOLDER_WARNING) as DialogFragment?)?.dialog?.isShowing ?: false
    if (!noFolderWarningIsShowing) {
      val warning = NoFolderWarningDialogFragment()
      warning.show(fragmentManager, FM_NO_FOLDER_WARNING)
    }
  }

  fun showLoading(loading: Boolean) {
    binding.loadingProgress.visible = loading
  }

  fun bookCoverChanged(bookId: Long) {
    // there is an issue where notifyDataSetChanges throws:
    // java.lang.IllegalStateException: Cannot call this method while RecyclerView is computing a layout or scrolling
    binding.recyclerView.postedIfComputingLayout {
      adapter.reloadBookCover(bookId)
    }
  }

  override fun onBookCoverChanged(book: Book) = adapter.reloadBookCover(book.id)

  override fun onInternetCoverRequested(book: Book) = router.pushController(RouterTransaction.with(ImagePickerController(book)))

  override fun onFileCoverRequested(book: Book) {
    menuBook = book
    val galleryPickerIntent = Intent(Intent.ACTION_PICK)
    galleryPickerIntent.type = "image/*"
    startActivityForResult(galleryPickerIntent, COVER_FROM_GALLERY)
  }

  enum class DisplayMode(@DrawableRes val icon: Int) {
    GRID(R.drawable.view_grid),
    LIST(R.drawable.ic_view_list);

    operator fun not(): DisplayMode = if (this == GRID) LIST else GRID
  }

  companion object {
    val TAG: String = BookShelfController::class.java.simpleName
    val FM_NO_FOLDER_WARNING = TAG + NoFolderWarningDialogFragment.TAG
  }
}
