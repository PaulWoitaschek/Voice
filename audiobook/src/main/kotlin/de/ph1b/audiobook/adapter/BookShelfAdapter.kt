package de.ph1b.audiobook.adapter

import android.content.Context
import android.support.annotation.CallSuper
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v7.util.SortedList
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.util.SortedListAdapterCallback
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.afollestad.materialdialogs.internal.MDTintHelper
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.R
import de.ph1b.audiobook.fragment.BookShelfFragment
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.model.NaturalOrderComparator
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.uitools.CoverReplacement
import de.ph1b.audiobook.utils.App
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Adapter for a recycler-view book shelf that keeps the items in a sorted list.
 *
 * @param c                   the context
 * @param displayMode         the display mode
 * @param onItemClickListener the listener that will be called when a book has been selected
 */
class BookShelfAdapter(private val c: Context, private val displayMode: BookShelfFragment.DisplayMode, private val onItemClickListener: BookShelfAdapter.OnItemClickListener) : RecyclerView.Adapter<BookShelfAdapter.BaseViewHolder>() {

    private val sortedList = SortedList(Book::class.java, object : SortedListAdapterCallback<Book>(this) {

        override fun compare(o1: Book, o2: Book): Int {
            return NaturalOrderComparator.STRING_COMPARATOR.compare(o1.name, o2.name)
        }

        override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
            return oldItem.globalPosition() == newItem.globalPosition() && oldItem.name == newItem.name && oldItem.useCoverReplacement == newItem.useCoverReplacement
        }

        override fun areItemsTheSame(item1: Book, item2: Book): Boolean {
            return item1.id == item2.id
        }
    })

    @Inject internal lateinit var prefs: PrefsManager

    init {
        App.getComponent().inject(this)
        setHasStableIds(true)
    }

    private fun formatTime(ms: Int): String {
        val h = "%02d".format((TimeUnit.MILLISECONDS.toHours(ms.toLong())))
        val m = "%02d".format((TimeUnit.MILLISECONDS.toMinutes(ms.toLong()) % 60))
        return h + ":" + m
    }

    fun removeBook(bookId: Long) {
        Timber.i("removeBook called with id %d", bookId)
        for (i in 0..sortedList.size() - 1) {
            val b = sortedList.get(i)
            if (b.id == bookId) {
                Timber.i("Found our book to remove %s", b)
                sortedList.remove(b)
                break
            }
        }
    }

    /**
     * Adds a book or updates it if it already exists.

     * @param book The new book
     */
    fun updateOrAddBook(book: Book) {
        var index = -1
        for (i in 0..sortedList.size() - 1) {
            if (sortedList.get(i).id == book.id) {
                index = i
                break
            }
        }

        if (index == -1) {
            sortedList.add(book) // add it if it doesnt exist
        } else {
            sortedList.updateItemAt(index, book) // update it if it exists
        }
    }

    /**
     * Adds a new set of books and removes the ones that do not exist any longer

     * @param books The new set of books
     */
    fun newDataSet(books: List<Book>) {
        sortedList.beginBatchedUpdates()
        try {
            // remove old books
            val booksToDelete = ArrayList<Book>(sortedList.size())
            for (i in 0..sortedList.size() - 1) {
                val existing = sortedList.get(i)
                var deleteBook = true
                for (b in books) {
                    if (existing.id == b.id) {
                        deleteBook = false
                        break
                    }
                }
                if (deleteBook) {
                    booksToDelete.add(existing)
                }
            }
            for (b in booksToDelete) {
                sortedList.remove(b)
            }

            // add new books
            for (b in books) {
                updateOrAddBook(b)
            }

        } finally {
            sortedList.endBatchedUpdates()
        }
    }

    override fun getItemId(position: Int): Long {
        return sortedList.get(position).id
    }

    /**
     * Gets the item at a requested position

     * @param position the adapter position
     * *
     * @return the book at the position
     */
    fun getItem(position: Int): Book {
        return sortedList.get(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        when (displayMode) {
            BookShelfFragment.DisplayMode.GRID -> return GridViewHolder(parent)
            BookShelfFragment.DisplayMode.LIST -> return ListViewHolder(parent)
            else -> throw IllegalStateException("Illegal viewType=" + viewType)
        }
    }

    /**
     * Calls [.notifyItemChanged] for a specified id

     * @param id the id of the item
     */
    fun notifyItemAtIdChanged(id: Long) {
        for (i in 0..sortedList.size() - 1) {
            if (sortedList.get(i).id == id) {
                notifyItemChanged(i)
                break
            }
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.bind(sortedList.get(position))
    }

    override fun getItemCount(): Int {
        return sortedList.size()
    }

    interface OnItemClickListener {
        /**
         * This method will be invoked when a item has been clicked

         * @param position adapter position of the item
         */
        fun onItemClicked(position: Int)

        /**
         * This method will be invoked when the menu of an item has been clicked

         * @param position The adapter position
         * *
         * @param view     The view that was clicked
         */
        fun onMenuClicked(position: Int, view: View)
    }

    /**
     * List viewHolder
     */
    inner class ListViewHolder(parent: ViewGroup) : BaseViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.fragment_book_shelf_list_layout, parent, false)) {

        private val progressBar: ProgressBar
        private val leftTime: TextView
        private val rightTime: TextView

        init {
            progressBar = itemView.findViewById(R.id.progressBar) as ProgressBar
            leftTime = itemView.findViewById(R.id.leftTime) as TextView
            rightTime = itemView.findViewById(R.id.rightTime) as TextView
            MDTintHelper.setTint(progressBar, ContextCompat.getColor(parent.context, R.color.accent))
        }

        override fun bind(book: Book) {
            super.bind(book)

            val globalPosition = book.globalPosition()
            val globalDuration = book.globalDuration()
            val progress = Math.round(100f * globalPosition.toFloat() / globalDuration.toFloat())

            leftTime.text = formatTime(globalPosition)
            progressBar.progress = progress
            rightTime.text = formatTime(globalDuration)
        }
    }

    /**
     * ViewHolder for the grid
     */
    inner class GridViewHolder(parent: ViewGroup) : BaseViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.fragment_book_shelf_grid_layout, parent, false))


    /**
     * ViewHolder base class

     * @param itemView The view to bind to
     */
    abstract inner class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val coverView: ImageView
        private val currentPlayingIndicator: ImageView
        private val titleView: TextView
        private val editBook: View
        private var indicatorVisible = false

        init {
            coverView = itemView.findViewById(R.id.coverView) as ImageView
            currentPlayingIndicator = itemView.findViewById(R.id.currentPlayingIndicator) as ImageView
            titleView = itemView.findViewById(R.id.title) as TextView
            editBook = itemView.findViewById(R.id.editBook)
        }

        fun indicatorIsVisible(): Boolean {
            return indicatorVisible
        }

        /**
         * Binds the ViewHolder to a book

         * @param book The book to bind to
         */
        @CallSuper
        open fun bind(book: Book) {

            //setting text
            val name = book.name
            titleView.text = name

            // (Cover)
            val coverFile = book.coverFile()
            val coverReplacement = CoverReplacement(book.name, c)

            if (!book.useCoverReplacement && coverFile.exists() && coverFile.canRead()) {
                Picasso.with(c).load(coverFile).placeholder(coverReplacement).into(coverView)
            } else {
                Picasso.with(c).cancelRequest(coverView)
                // we have to set the replacement in onPreDraw, else the transition will fail.
                coverView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        coverView.viewTreeObserver.removeOnPreDrawListener(this)
                        coverView.setImageDrawable(coverReplacement)
                        return true
                    }
                })
            }

            indicatorVisible = book.id == prefs.currentBookId.value
            if (indicatorVisible) {
                currentPlayingIndicator.visibility = View.VISIBLE
            } else {
                currentPlayingIndicator.visibility = View.GONE
            }

            itemView.setOnClickListener { onItemClickListener.onItemClicked(adapterPosition) }
            editBook.setOnClickListener { onItemClickListener.onMenuClicked(adapterPosition, it) }

            ViewCompat.setTransitionName(coverView, book.coverTransitionName())
        }
    }
}