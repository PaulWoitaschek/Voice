/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Material Audiobook Player. If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.view.fragment

import android.content.Intent
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.v4.app.DialogFragment
import android.support.v4.view.ViewCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.view.*
import android.widget.PopupMenu
import com.getbase.floatingactionbutton.FloatingActionButton
import de.ph1b.audiobook.BuildConfig
import de.ph1b.audiobook.R
import de.ph1b.audiobook.actionBar
import de.ph1b.audiobook.activity.SettingsActivity
import de.ph1b.audiobook.adapter.BookShelfAdapter
import de.ph1b.audiobook.dialog.BookmarkDialogFragment
import de.ph1b.audiobook.dialog.EditBookTitleDialogFragment
import de.ph1b.audiobook.dialog.NoFolderWarningDialogFragment
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.mvp.RxBaseFragment
import de.ph1b.audiobook.persistence.LogStorage
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.presenter.BookShelfBasePresenter
import de.ph1b.audiobook.uitools.DividerItemDecoration
import de.ph1b.audiobook.uitools.PlayPauseDrawable
import i
import java.util.*
import javax.inject.Inject
import dagger.Lazy as DaggerLazy

/**
 * Showing the shelf of all the available books and provide a navigation to each book
 */
class BookShelfFragment : RxBaseFragment<BookShelfFragment, BookShelfBasePresenter>(), BookShelfAdapter.OnItemClickListener {

    override fun newPresenter() = App.component().bookShelfBasePresenter

    override fun provideView() = this

    init {
        App.component().inject(this)
    }

    // injection
    @Inject internal lateinit var prefs: PrefsManager

    // viewAdded
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerReplacementView: View
    private lateinit var fab: FloatingActionButton
    private val playPauseDrawable = PlayPauseDrawable()
    private val adapter by lazy { BookShelfAdapter(context, this) }
    private lateinit var listDecoration: RecyclerView.ItemDecoration
    private lateinit var gridLayoutManager: GridLayoutManager
    private lateinit var linearLayoutManager: RecyclerView.LayoutManager

    // callbacks
    private val hostingActivity: AppCompatActivity by lazy { activity as AppCompatActivity }
    private val callBack: Callback by lazy { activity as Callback }

    // vars
    private var firstPlayStateUpdate = true
    private var currentBook: Book? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        i { "onCreate" }

        setHasOptionsMenu(true)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // find views
        val view = inflater.inflate(R.layout.fragment_book_shelf, container, false)
        recyclerView = view.findViewById(R.id.recyclerView) as RecyclerView
        recyclerReplacementView = view.findViewById(R.id.recyclerReplacement)
        fab = view.findViewById(R.id.fab) as FloatingActionButton

        // init fab
        fab.setIconDrawable(playPauseDrawable)
        fab.setOnClickListener { presenter()!!.playPauseRequested() }

        // init ActionBar
        actionBar().apply {
            setDisplayHomeAsUpEnabled(false)
            setHomeAsUpIndicator(R.drawable.ic_arrow_back)
            title = getString(R.string.app_name)
        }

        // init RecyclerView
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
        // without this the item would blink on every change
        val anim = recyclerView.itemAnimator as SimpleItemAnimator
        anim.supportsChangeAnimations = false
        listDecoration = DividerItemDecoration(context)
        gridLayoutManager = GridLayoutManager(context, amountOfColumns())
        linearLayoutManager = LinearLayoutManager(context)
        initRecyclerView()

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.book_shelf, menu)

        // sets menu item visible if there is a current book
        val currentPlaying = menu.findItem(R.id.action_current)
        currentPlaying.isVisible = currentBook != null

        if (BuildConfig.DEBUG) {
            val sendLogs = menu.findItem(R.id.sendLogs)
            sendLogs.isVisible = true
        }

        // sets the grid / list toggle icon
        val displayModeItem = menu.findItem(R.id.action_change_layout)
        displayModeItem.setIcon(prefs.displayMode.inverted().icon)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(context, SettingsActivity::class.java))
                true
            }
            R.id.action_current -> {
                invokeBookSelectionCallback(prefs.currentBookId.value)
                true
            }
            R.id.action_change_layout -> {
                prefs.displayMode = prefs.displayMode.inverted()
                initRecyclerView()
                true
            }
            R.id.sendLogs -> {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "message/rfc822";
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("woitaschek@gmail.com"));
                    putExtra(Intent.EXTRA_SUBJECT, "MAP Logs");
                    val logs = LogStorage.get()
                    val logsBuilder = StringBuilder()
                    logs.forEach { logsBuilder.append(it).append("\n") }
                    putExtra(Intent.EXTRA_TEXT, logsBuilder.toString());
                }
                startActivity(Intent.createChooser(intent, "Send mail..."));
                return true;
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onItemClicked(position: Int) {
        val bookId = adapter.getItemId(position)
        invokeBookSelectionCallback(bookId)
    }

    override fun onMenuClicked(position: Int, view: View) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.bookshelf_popup)
        popupMenu.setOnMenuItemClickListener {
            val book = adapter.getItem(position)
            when (it.itemId) {
                R.id.edit_cover -> {
                    callBack.onCoverChanged(book)
                    return@setOnMenuItemClickListener true
                }
                R.id.edit_title -> {
                    EditBookTitleDialogFragment.newInstance(book).show(fragmentManager,
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

    /**
     * Returns the amount of columns the main-grid will need.

     * @return The amount of columns, but at least 2.
     */
    private fun amountOfColumns(): Int {
        val r = recyclerView.resources
        val displayMetrics = resources.displayMetrics
        var widthPx = displayMetrics.widthPixels.toFloat()
        val desiredPx = r.getDimensionPixelSize(R.dimen.desired_medium_cover).toFloat()
        val columns = Math.round(widthPx / desiredPx)
        return Math.max(columns, 2)
    }

    private fun initRecyclerView() {
        val defaultDisplayMode = prefs.displayMode
        if (defaultDisplayMode == BookShelfFragment.DisplayMode.GRID) {
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
        prefs.setCurrentBookId(bookId)

        val sharedElements = HashMap<View, String>(2)
        val viewHolder = recyclerView.findViewHolderForItemId(bookId) as BookShelfAdapter.BaseViewHolder?
        if (viewHolder != null) {
            sharedElements.put(viewHolder.coverView, ViewCompat.getTransitionName(viewHolder.coverView))
        }
        sharedElements.put(fab, ViewCompat.getTransitionName(fab))
        callBack.onBookSelected(bookId, sharedElements)
    }

    /**
     * A book was removed
     *
     * @param book the removed book
     */
    fun bookRemoved(book: Book) {
        i { "bookRemoved ${book.name}" }
        adapter.removeBook(book)
    }

    /**
     * A book was added or updated
     *
     * @param book the changed book
     */
    fun bookAddedOrUpdated(book: Book) {
        i { "bookAddedOrUpdated: ${book.name}" }
        adapter.updateOrAddBook(book)
    }

    /**
     * There is a completely new set of books
     *
     *@param books the new books
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
        val noFolderWarningIsShowing = (fragmentManager.findFragmentByTag(FM_NO_FOLDER_WARNING) as DialogFragment?)?.dialog?.isShowing ?: false
        if (noFolderWarningIsShowing.not()) {
            val warning = NoFolderWarningDialogFragment()
            warning.show(fragmentManager, FM_NO_FOLDER_WARNING)
        }
    }

    fun showSpinnerIfNoData(showSpinnerIfNoData: Boolean) {
        val shouldShowSpinner = adapter.itemCount == 0 && showSpinnerIfNoData
        recyclerView.visibility = if (shouldShowSpinner) View.INVISIBLE else View.VISIBLE
        recyclerReplacementView.visibility = if (shouldShowSpinner) View.VISIBLE else View.INVISIBLE
    }

    enum class DisplayMode constructor(@DrawableRes val icon: Int) {
        GRID(R.drawable.view_grid),
        LIST(R.drawable.ic_view_list);

        fun inverted(): DisplayMode = if (this == GRID) LIST else GRID
    }

    interface Callback {
        fun onBookSelected(bookId: Long, sharedViews: Map<View, String>)
        fun onCoverChanged(book: Book)
    }

    companion object {

        val TAG = BookShelfFragment::class.java.simpleName
        val FM_NO_FOLDER_WARNING = TAG + NoFolderWarningDialogFragment.TAG
    }
}
