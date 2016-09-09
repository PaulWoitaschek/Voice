package de.ph1b.audiobook.features.book_overview

import android.content.Intent
import android.os.Build
import android.support.annotation.DrawableRes
import android.support.v4.app.DialogFragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.view.*
import com.bluelinelabs.conductor.RouterTransaction
import com.getbase.floatingactionbutton.FloatingActionButton
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.book_overview.BookShelfAdapter.ClickType
import de.ph1b.audiobook.features.book_playing.BookPlayController
import de.ph1b.audiobook.features.settings.SettingsActivity
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.setupActionbar
import de.ph1b.audiobook.misc.supportTransitionName
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.mvp.RxBaseController
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.uitools.BookTransition
import de.ph1b.audiobook.uitools.DividerItemDecoration
import de.ph1b.audiobook.uitools.PlayPauseDrawable
import i
import kotlinx.android.synthetic.main.fragment_book_shelf.view.*
import javax.inject.Inject
import dagger.Lazy as DaggerLazy

/**
 * Showing the shelf of all the available books and provide a navigation to each book
 */
class BookShelfController : RxBaseController<BookShelfController, BookShelfPresenter>() {

    override fun newPresenter(): BookShelfPresenter = App.component().bookShelfPresenter

    override fun provideView() = this

    init {
        App.component().inject(this)
        setHasOptionsMenu(true)
    }

    // injection
    @Inject lateinit var prefs: PrefsManager

    // viewAdded
    private val playPauseDrawable = PlayPauseDrawable()
    private lateinit var adapter: BookShelfAdapter
    private lateinit var listDecoration: RecyclerView.ItemDecoration
    private lateinit var gridLayoutManager: GridLayoutManager
    private lateinit var linearLayoutManager: RecyclerView.LayoutManager

    // callbacks
    private val hostingActivity: AppCompatActivity by lazy { activity as AppCompatActivity }

    // vars
    private var firstPlayStateUpdate = true
    private var currentBook: Book? = null

    override fun onAttach(view: View) {
        // init fab
        fab.setIconDrawable(playPauseDrawable)
        fab.setOnClickListener { presenter.playPauseRequested() }

        // init ActionBar
        setupActionbar(title = activity.getString(R.string.app_name))

        // init RecyclerView
        recyclerView.setHasFixedSize(true)
        adapter = BookShelfAdapter(activity) { book, clickType ->
            if (clickType == ClickType.REGULAR) {
                invokeBookSelectionCallback(book.id)
            } else {
                EditBookBottomSheet.newInstance(book)
                        .show(hostingActivity.supportFragmentManager, "editBottomSheet")
            }
        }
        recyclerView.adapter = adapter
        // without this the item would blink on every change
        val anim = recyclerView.itemAnimator as SimpleItemAnimator
        anim.supportsChangeAnimations = false
        listDecoration = DividerItemDecoration(activity)
        gridLayoutManager = GridLayoutManager(activity, amountOfColumns())
        linearLayoutManager = LinearLayoutManager(activity)
        initRecyclerView()

        super.onAttach(view)
    }

    private lateinit var recyclerReplacement: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        val view = inflater.inflate(R.layout.fragment_book_shelf, container, false)
        recyclerView = view.recyclerView
        fab = view.fab
        recyclerReplacement = view.recyclerReplacement

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.book_shelf, menu)

        // sets menu item visible if there is a current book
        val currentPlaying = menu.findItem(R.id.action_current)
        currentPlaying.isVisible = currentBook != null

        // sets the grid / list toggle icon
        val displayModeItem = menu.findItem(R.id.action_change_layout)
        displayModeItem.setIcon(prefs.displayMode.value().inverted().icon)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(router.activity, SettingsActivity::class.java))
                true
            }
            R.id.action_current -> {
                invokeBookSelectionCallback(prefs.currentBookId.value())
                true
            }
            R.id.action_change_layout -> {
                prefs.displayMode.set(prefs.displayMode.value().inverted())
                initRecyclerView()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Returns the amount of columns the main-grid will need
    private fun amountOfColumns(): Int {
        val r = recyclerView.resources
        val displayMetrics = resources.displayMetrics
        val widthPx = displayMetrics.widthPixels.toFloat()
        val desiredPx = r.getDimensionPixelSize(R.dimen.desired_medium_cover).toFloat()
        val columns = Math.round(widthPx / desiredPx)
        return Math.max(columns, 2)
    }

    private fun initRecyclerView() {
        val defaultDisplayMode = prefs.displayMode.value()
        if (defaultDisplayMode == DisplayMode.GRID) {
            recyclerView.removeItemDecoration(listDecoration)
            recyclerView.layoutManager = gridLayoutManager
        } else {
            recyclerView.addItemDecoration(listDecoration, 0)
            recyclerView.layoutManager = linearLayoutManager
        }
        adapter.displayMode = defaultDisplayMode
        hostingActivity.invalidateOptionsMenu()
    }

    private fun invokeBookSelectionCallback(bookId: Long) {
        prefs.currentBookId.set(bookId)

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

    /**
     * There is a completely new set of books
     *
     * @param books the new books
     */
    fun newBooks(books: List<Book>) {
        i { "${books.size} newBooks" }
        adapter.newDataSet(books)
    }

    /**
     * The book marked as current was changed. Updates the adapter and fab accordingly.
     */
    fun currentBookChanged(currentBook: Book?) {
        i { "currentBookChanged: ${currentBook?.name}" }
        this.currentBook = currentBook

        for (i in 0..adapter.itemCount - 1) {
            val itemId = adapter.getItemId(i)
            val vh = recyclerView.findViewHolderForItemId(itemId) as BookShelfAdapter.BaseViewHolder?
            if (itemId == currentBook?.id || (vh != null && vh.indicatorIsVisible())) {
                adapter.notifyItemChanged(i)
            }
        }

        if (currentBook == null) {
            fab.visibility = View.GONE
        } else {
            fab.visibility = View.VISIBLE
        }
    }


    /**
     * Sets the fab icon correctly accordingly to the new play state.
     */
    fun setPlayerPlaying(playing: Boolean) {
        i { "Called setPlayerPlaying $playing" }
        if (playing) {
            playPauseDrawable.transformToPause(!firstPlayStateUpdate)
        } else {
            playPauseDrawable.transformToPlay(!firstPlayStateUpdate)
        }
        firstPlayStateUpdate = false
    }

    /**
     * Show a warning that no audiobook folder was chosen
     */
    fun showNoFolderWarning() {
        // show dialog if no folders are set
        val noFolderWarningIsShowing = (hostingActivity.supportFragmentManager.findFragmentByTag(FM_NO_FOLDER_WARNING) as DialogFragment?)?.dialog?.isShowing ?: false
        if (noFolderWarningIsShowing.not()) {
            val warning = NoFolderWarningDialogFragment()
            warning.show(hostingActivity.supportFragmentManager, FM_NO_FOLDER_WARNING)
        }
    }

    fun showSpinnerIfNoData(showSpinnerIfNoData: Boolean) {
        val shouldShowSpinner = adapter.itemCount == 0 && showSpinnerIfNoData
        recyclerView.visibility = if (shouldShowSpinner) View.INVISIBLE else View.VISIBLE
        recyclerReplacement.visibility = if (shouldShowSpinner) View.VISIBLE else View.INVISIBLE
    }

    enum class DisplayMode constructor(@DrawableRes val icon: Int) {
        GRID(R.drawable.view_grid),
        LIST(R.drawable.ic_view_list);

        fun inverted(): DisplayMode = if (this == GRID) LIST else GRID
    }

    companion object {

        val TAG: String = BookShelfController::class.java.simpleName
        val FM_NO_FOLDER_WARNING = TAG + NoFolderWarningDialogFragment.TAG
    }
}
