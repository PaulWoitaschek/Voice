package de.ph1b.audiobook.features.bookOverview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.support.annotation.DrawableRes
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentTransaction
import android.support.v7.widget.*
import android.view.*
import com.bluelinelabs.conductor.RouterTransaction
import com.getbase.floatingactionbutton.FloatingActionButton
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.bookPlaying.BookPlayController
import de.ph1b.audiobook.features.imagepicker.ImagePickerController
import de.ph1b.audiobook.features.settings.SettingsController
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.*
import de.ph1b.audiobook.mvp.MvpBaseController
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.uitools.BookTransition
import de.ph1b.audiobook.uitools.PlayPauseDrawable
import de.ph1b.audiobook.uitools.VerticalChangeHandler
import de.ph1b.audiobook.uitools.visible
import i
import javax.inject.Inject
import dagger.Lazy as DaggerLazy

/**
 * Showing the shelf of all the available books and provide a navigation to each book
 */
class BookShelfController : MvpBaseController<BookShelfController, BookShelfPresenter>(), EditCoverDialogFragment.Callback, EditBookBottomSheet.Callback {

  override val presenter = App.component.bookShelfPresenter
  private val COVER_FROM_GALLERY = 1

  override fun provideView() = this

  init {
    App.component.inject(this)
    setHasOptionsMenu(true)
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

  // views
  private lateinit var recyclerView: RecyclerView
  private lateinit var fab: FloatingActionButton
  private lateinit var toolbar: Toolbar
  private lateinit var loadingProgress: View

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
    val view = inflater.inflate(R.layout.book_shelf, container, false)
    recyclerView = view.find(R.id.recyclerView)
    fab = view.find(R.id.fab)
    toolbar = view.find(R.id.toolbar)
    loadingProgress = view.find(R.id.loadingProgress)

    // init fab
    fab.setIconDrawable(playPauseDrawable)
    fab.setOnClickListener { presenter.playPauseRequested() }

    // init RecyclerView
    recyclerView.setHasFixedSize(true)
    adapter = BookShelfAdapter(activity) { book, clickType ->
      if (clickType == BookShelfAdapter.ClickType.REGULAR) {
        invokeBookSelectionCallback(book.id)
      } else {
        EditBookBottomSheet.newInstance(this, book)
          .show(fragmentManager, "editBottomSheet")
      }
    }
    recyclerView.adapter = adapter
    // without this the item would blink on every change
    val anim = recyclerView.itemAnimator as SimpleItemAnimator
    anim.supportsChangeAnimations = false
    listDecoration = DividerItemDecoration(activity, DividerItemDecoration.VERTICAL)
    gridLayoutManager = GridLayoutManager(activity, amountOfColumns())
    linearLayoutManager = LinearLayoutManager(activity)
    initRecyclerView()

    return view
  }

  override fun onActivityResumed(activity: Activity) {
    super.onActivityResumed(activity)

    pendingTransaction?.commit()
    pendingTransaction = null
  }

  override fun onAttach(view: View) {
    // init ActionBar
    setupActionbar(toolbar = toolbar,
      title = getString(R.string.app_name))
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.book_shelf, menu)

    // sets menu item visible if there is a current book
    val currentPlaying = menu.findItem(R.id.action_current)
    currentPlaying.isVisible = currentBook != null

    // sets the grid / list toggle icon
    val displayModeItem = menu.findItem(R.id.action_change_layout)
    displayModeItem.setIcon(prefs.displayMode.value.inverted().icon)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.action_settings -> {
        val transaction = SettingsController().asTransaction(
          VerticalChangeHandler(),
          VerticalChangeHandler()
        )
        router.pushController(transaction)
        true
      }
      R.id.action_current -> {
        invokeBookSelectionCallback(prefs.currentBookId.value)
        true
      }
      R.id.action_change_layout -> {
        val newDisplayMode = prefs.displayMode.value.inverted()
        prefs.displayMode.value = newDisplayMode
        initRecyclerView()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
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
    val r = recyclerView.resources
    val displayMetrics = r.displayMetrics
    val widthPx = displayMetrics.widthPixels.toFloat()
    val desiredPx = r.getDimensionPixelSize(R.dimen.desired_medium_cover).toFloat()
    val columns = Math.round(widthPx / desiredPx)
    return Math.max(columns, 2)
  }

  private fun initRecyclerView() {
    val defaultDisplayMode = prefs.displayMode.value
    if (defaultDisplayMode == DisplayMode.GRID) {
      recyclerView.removeItemDecoration(listDecoration)
      recyclerView.layoutManager = gridLayoutManager
    } else {
      recyclerView.addItemDecoration(listDecoration, 0)
      recyclerView.layoutManager = linearLayoutManager
    }
    adapter.displayMode = defaultDisplayMode
    activity.invalidateOptionsMenu()
  }

  private fun invokeBookSelectionCallback(bookId: Long) {
    prefs.currentBookId.value = bookId

    val viewHolder = recyclerView.findViewHolderForItemId(bookId) as BookShelfAdapter.BaseViewHolder?
    val transaction = RouterTransaction.with(BookPlayController.newInstance(bookId))
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
  fun newBooks(books: List<Book>) {
    i { "${books.size} newBooks" }
    adapter.newDataSet(books)
  }

  /** The book marked as current was changed. Updates the adapter and fab accordingly. */
  fun currentBookChanged(currentBook: Book?) {
    i { "currentBookChanged: ${currentBook?.name}" }
    this.currentBook = currentBook

    for (i in 0..adapter.itemCount - 1) {
      val itemId = adapter.getItemId(i)
      val vh = recyclerView.findViewHolderForItemId(itemId) as BookShelfAdapter.BaseViewHolder?
      if (itemId == currentBook?.id || (vh != null && vh.indicatorVisible)) {
        adapter.notifyItemChanged(i)
      }
    }

    fab.visible = currentBook != null
  }

  /** Sets the fab icon correctly accordingly to the new play state. */
  fun setPlayerPlaying(playing: Boolean) {
    i { "Called setPlayerPlaying $playing" }
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
    loadingProgress.visible = loading
  }

  override fun onBookCoverChanged(book: Book) = adapter.reloadBookCover(book.id)

  fun bookCoverChanged(bookId: Long) {
    // there is an issue where notifyDataSetChanges throws:
    // java.lang.IllegalStateException: Cannot call this method while RecyclerView is computing a layout or scrolling
    recyclerView.postedIfComputingLayout {
      adapter.reloadBookCover(bookId)
    }
  }

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

    fun inverted(): DisplayMode = if (this == GRID) LIST else GRID
  }

  companion object {

    val TAG: String = BookShelfController::class.java.simpleName
    val FM_NO_FOLDER_WARNING = TAG + NoFolderWarningDialogFragment.TAG
  }
}
