package de.ph1b.audiobook.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.view.ViewCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.view.*
import android.widget.PopupMenu
import com.afollestad.materialdialogs.MaterialDialog
import com.getbase.floatingactionbutton.FloatingActionButton
import de.ph1b.audiobook.R
import de.ph1b.audiobook.activity.FolderOverviewActivity
import de.ph1b.audiobook.activity.SettingsActivity
import de.ph1b.audiobook.adapter.BookShelfAdapter
import de.ph1b.audiobook.dialog.BookmarkDialogFragment
import de.ph1b.audiobook.dialog.EditBookTitleDialogFragment
import de.ph1b.audiobook.dialog.EditCoverDialogFragment
import de.ph1b.audiobook.interfaces.MultiPaneInformer
import de.ph1b.audiobook.mediaplayer.MediaPlayerController
import de.ph1b.audiobook.model.BookAdder
import de.ph1b.audiobook.persistence.BookShelf
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.service.ServiceController
import de.ph1b.audiobook.uitools.DividerItemDecoration
import de.ph1b.audiobook.uitools.PlayPauseDrawable
import de.ph1b.audiobook.utils.App
import de.ph1b.audiobook.utils.BookVendor
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Action1
import rx.subscriptions.CompositeSubscription
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * Showing the shelf of all the available books and provide a navigation to each book

 * @author Paul Woitaschek
 */
class BookShelfFragment : BaseFragment(), BookShelfAdapter.OnItemClickListener, EditBookTitleDialogFragment.OnTextChanged, EditCoverDialogFragment.OnEditBookFinished {

    private val playPauseDrawable = PlayPauseDrawable()

    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerReplacementView: View
    private lateinit var fab: FloatingActionButton

    @Inject internal lateinit var prefs: PrefsManager
    @Inject internal lateinit var db: BookShelf
    @Inject internal lateinit var bookAdder: BookAdder
    @Inject internal lateinit var mediaPlayerController: MediaPlayerController
    @Inject internal lateinit var bookVendor: BookVendor

    private var isMultiPanel: Boolean = false
    private var subscriptions: CompositeSubscription? = null

    private lateinit var adapter: BookShelfAdapter
    private lateinit var controller: ServiceController
    private lateinit var noFolderWarning: MaterialDialog
    private lateinit var listDecoration: RecyclerView.ItemDecoration
    private lateinit var gridLayoutManager: GridLayoutManager
    private lateinit var linearLayoutManager: RecyclerView.LayoutManager

    private lateinit var multiPaneInformer: MultiPaneInformer
    private lateinit var bookSelectionCallback: BookSelectionCallback
    private lateinit var hostingActivity: AppCompatActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_book_shelf, container, false)
        recyclerView = view.findViewById(R.id.recyclerView) as RecyclerView
        recyclerReplacementView = view.findViewById(R.id.recyclerReplacement)
        fab = view.findViewById(R.id.fab) as FloatingActionButton

        isMultiPanel = multiPaneInformer.isMultiPanel

        // find views
        val actionBar = hostingActivity.supportActionBar!!

        // init views
        actionBar.setDisplayHomeAsUpEnabled(false)
        actionBar.title = getString(R.string.app_name)
        fab.setIconDrawable(playPauseDrawable)
        fab.setOnClickListener({ playPauseClicked() })
        recyclerView.setHasFixedSize(true)
        // without this the item would blink on every change
        val anim = recyclerView.itemAnimator as SimpleItemAnimator
        anim.supportsChangeAnimations = false

        listDecoration = DividerItemDecoration(context)
        gridLayoutManager = GridLayoutManager(context, amountOfColumns)
        linearLayoutManager = LinearLayoutManager(context)
        initRecyclerView()

        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        App.getComponent().inject(this)

        setHasOptionsMenu(true)

        // init variables
        controller = ServiceController(context)
        noFolderWarning = MaterialDialog.Builder(context)
                .title(R.string.no_audiobook_folders_title)
                .content(getString(R.string.no_audiobook_folders_summary_start) +
                        "\n\n" + getString(R.string.no_audiobook_folders_end))
                .positiveText(R.string.dialog_confirm)
                .onPositive { materialDialog, dialogAction ->
                    startActivity(Intent(context, FolderOverviewActivity::class.java))
                }
                .cancelable(false)
                .build()
    }

    private fun initRecyclerView() {
        val defaultDisplayMode = prefs.displayMode
        recyclerView.removeItemDecoration(listDecoration)
        if (defaultDisplayMode == BookShelfFragment.DisplayMode.GRID) {
            recyclerView.layoutManager = gridLayoutManager
        } else {
            recyclerView.layoutManager = linearLayoutManager
            recyclerView.addItemDecoration(listDecoration)
        }
        adapter = BookShelfAdapter(context, defaultDisplayMode, this)
        recyclerView.adapter = adapter
        hostingActivity.invalidateOptionsMenu()
    }

    /**
     * Returns the amount of columns the main-grid will need.

     * @return The amount of columns, but at least 2.
     */
    private val amountOfColumns: Int
        get() {
            val r = recyclerView.resources
            val displayMetrics = resources.displayMetrics
            var widthPx = displayMetrics.widthPixels.toFloat()
            if (isMultiPanel) {
                widthPx /= 2
            }
            val desiredPx = r.getDimensionPixelSize(R.dimen.desired_medium_cover).toFloat()
            val columns = Math.round(widthPx / desiredPx)
            return Math.max(columns, 2)
        }

    override fun onStart() {
        super.onStart()

        // scan for files
        bookAdder.scanForFiles(false)

        subscriptions = CompositeSubscription()

        // observe if the scanner is active and show spinner accordingly.
        subscriptions!!.add(bookAdder.scannerActive().observeOn(AndroidSchedulers.mainThread())
                .subscribe { checkVisibilities() })

        // show dialog if no folders are set
        val audioFoldersEmpty = (prefs.collectionFolders.size + prefs.singleBookFolders.size) == 0
        val noFolderWarningIsShowing = noFolderWarning.isShowing
        if (audioFoldersEmpty && !noFolderWarningIsShowing) {
            noFolderWarning.show()
        }

        // initially updates the adapter with a new set of items
        adapter.newDataSet(bookVendor.all())

        // Subscription that informs the adapter about a removed book
        subscriptions!!.add(db.removedObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    adapter.removeBook(it.id)
                })

        // Subscription that notifies the adapter when the current book has changed. It also notifies
        // the item with the old indicator now falsely showing.
        subscriptions!!.add(prefs.currentBookId.subscribe {
            for (i in 0..adapter.itemCount - 1) {
                val itemId = adapter.getItemId(i)
                val vh = recyclerView.findViewHolderForItemId(itemId) as BookShelfAdapter.BaseViewHolder?
                if (itemId == it || (vh != null && vh.indicatorIsVisible())) {
                    adapter.notifyItemChanged(i)
                }
            }
            checkVisibilities()
        })

        // Subscription that notifies the adapter when there is a new or updated book.
        subscriptions!!.add(Observable.merge(db.updateObservable(), db.addedObservable())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    adapter.updateOrAddBook(it)
                    checkVisibilities()
                })

        // Subscription that updates the UI based on the play state.
        subscriptions!!.add(mediaPlayerController.playState.observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Action1<MediaPlayerController.PlayState> {
                    private var firstRun = true

                    override fun call(playState: MediaPlayerController.PlayState) {
                        // animate only if this is not the first run
                        Timber.i("onNext with playState %s", playState)
                        if (playState === MediaPlayerController.PlayState.PLAYING) {
                            playPauseDrawable.transformToPause(!firstRun)
                        } else {
                            playPauseDrawable.transformToPlay(!firstRun)
                        }

                        firstRun = false
                    }
                }))
    }

    override fun onStop() {
        super.onStop()

        subscriptions!!.unsubscribe()
    }

    private fun checkVisibilities() {
        val hideRecycler = adapter.itemCount == 0 && bookAdder.scannerActive().value
        if (hideRecycler) {
            recyclerReplacementView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            recyclerReplacementView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }

        var currentBookExists = false
        val currentBookId = prefs.currentBookId.value
        for (i in 0..adapter.itemCount - 1) {
            if (currentBookId == adapter.getItemId(i)) {
                currentBookExists = true
                break
            }
        }

        if (isMultiPanel || !currentBookExists) {
            fab.visibility = View.GONE
        } else {
            fab.visibility = View.VISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater!!.inflate(R.menu.book_shelf, menu)

        // sets menu item visible if there is a current book
        val currentPlaying = menu!!.findItem(R.id.action_current)
        currentPlaying.setVisible(!isMultiPanel && bookVendor.byId(prefs.currentBookId.value) != null)

        // sets the grid / list toggle icon
        val displayModeItem = menu.findItem(R.id.action_change_layout)
        val gridMode = prefs.displayMode == DisplayMode.GRID
        displayModeItem.setIcon(if (gridMode) R.drawable.ic_view_list else R.drawable.ic_view_grid_white_24dp)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(context, SettingsActivity::class.java))
                return true
            }
            R.id.action_current -> {
                invokeBookSelectionCallback(prefs.currentBookId.value)
                return true
            }
            R.id.action_change_layout -> {
                val mode = prefs.displayMode
                val invertedMode = if (mode == DisplayMode.GRID) DisplayMode.LIST else DisplayMode.GRID
                prefs.displayMode = invertedMode
                initRecyclerView()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun invokeBookSelectionCallback(bookId: Long) {
        prefs.setCurrentBookId(bookId)

        val sharedElements = HashMap<View, String>(2)
        val viewHolder = recyclerView.findViewHolderForItemId(bookId) as BookShelfAdapter.BaseViewHolder?
        if (viewHolder != null) {
            sharedElements.put(viewHolder.coverView, ViewCompat.getTransitionName(viewHolder.coverView))
        }
        sharedElements.put(fab, ViewCompat.getTransitionName(fab))
        bookSelectionCallback.onBookSelected(bookId, sharedElements)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        bookSelectionCallback = context as BookSelectionCallback
        multiPaneInformer = context as MultiPaneInformer
        hostingActivity = context as AppCompatActivity
    }

    private fun playPauseClicked() {
        controller.playPause()
    }

    override fun onItemClicked(position: Int) {
        val bookId = adapter.getItemId(position)
        invokeBookSelectionCallback(bookId)
    }

    override fun onMenuClicked(position: Int, view: View) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.bookshelf_popup)
        popupMenu.setOnMenuItemClickListener { item ->
            val book = adapter.getItem(position)
            when (item.itemId) {
                R.id.edit_cover -> {
                    EditCoverDialogFragment.newInstance(this, book).show(fragmentManager, EditCoverDialogFragment.TAG)
                    return@setOnMenuItemClickListener true
                }
                R.id.edit_title -> {
                    EditBookTitleDialogFragment.newInstance(this, book).show(fragmentManager,
                            EditBookTitleDialogFragment.TAG)
                    return@setOnMenuItemClickListener true
                }
                R.id.bookmark -> {
                    BookmarkDialogFragment.newInstance(adapter.getItemId(position)).show(fragmentManager, TAG)
                    return@setOnMenuItemClickListener true
                }
                else -> return@setOnMenuItemClickListener false
            }
        }
        popupMenu.show()
    }

    override fun onTitleChanged(newTitle: String, bookId: Long) {
        Timber.i("onTitleChanged with title %s and id %d", newTitle, bookId)
        var dbBook = bookVendor.byId(bookId)?.copy(name = newTitle)
        if (dbBook != null) {
            db.updateBook(dbBook)
        }
    }

    override fun onEditBookFinished(bookId: Long) {
        // this is necessary for the cover update
        adapter.notifyItemAtIdChanged(bookId)
    }

    enum class DisplayMode {
        GRID,
        LIST
    }

    interface BookSelectionCallback {
        /**
         * This is called when a selection has been made

         * @param bookId      the id of the selected book
         * *
         * @param sharedViews A mapping of the shared views and their transition names
         */
        fun onBookSelected(bookId: Long, sharedViews: Map<View, String>)
    }

    companion object {

        @JvmField val TAG = BookShelfFragment::class.java.simpleName
    }
}
