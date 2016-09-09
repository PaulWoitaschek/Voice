package de.ph1b.audiobook.features.book_overview

import android.content.Context
import android.support.annotation.CallSuper
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import com.afollestad.materialdialogs.internal.MDTintHelper
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.uitools.CoverReplacement
import i
import kotlinx.android.synthetic.main.fragment_book_shelf_list_layout.view.*
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// display all the books
class BookShelfAdapter(private val c: Context, private val bookClicked: (Book, ClickType) -> Unit) : RecyclerView.Adapter<BookShelfAdapter.BaseViewHolder>() {

    private val books = ArrayList<Book>()

    @Inject lateinit var prefs: PrefsManager

    init {
        i { "A new adapter was created." }
        App.component().inject(this)
        setHasStableIds(true)
    }

    private fun formatTime(ms: Int): String {
        val h = "%02d".format((TimeUnit.MILLISECONDS.toHours(ms.toLong())))
        val m = "%02d".format((TimeUnit.MILLISECONDS.toMinutes(ms.toLong()) % 60))
        return h + ":" + m
    }

    /**
     * Adds a new set of books and removes the ones that do not exist any longer

     * @param newBooks The new set of books
     */
    fun newDataSet(newBooks: List<Book>) {
        i { "newDataSet was called with ${newBooks.size} books" }

        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = books.size

            override fun getNewListSize(): Int = newBooks.size

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = books[oldItemPosition]
                val newItem = newBooks[newItemPosition]
                return oldItem.globalPosition() == newItem.globalPosition() && oldItem.name == newItem.name && oldItem.useCoverReplacement == newItem.useCoverReplacement
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = books[oldItemPosition]
                val newItem = newBooks[newItemPosition]
                return oldItem.id == newItem.id
            }
        }, false) // no need to detect moves as the list is sorted

        books.clear()
        books.addAll(newBooks)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemId(position: Int): Long = books[position].id

    /**
     * Gets the item at a requested position

     * @param position the adapter position
     * *
     * @return the book at the position
     */
    fun getItem(position: Int): Book = books[position]

    var displayMode: BookShelfController.DisplayMode = BookShelfController.DisplayMode.LIST
        set(value) {
            if (value != field) {
                field = value
                i { "displaymode changed to $field" }
                notifyDataSetChanged()
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        when (viewType) {
            1 -> return GridViewHolder(parent)
            0 -> return ListViewHolder(parent)
            else -> throw IllegalStateException("Illegal viewType=" + viewType)
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) = holder.bind(books[position])

    override fun getItemCount(): Int = books.size

    override fun getItemViewType(position: Int): Int {
        return if (displayMode == BookShelfController.DisplayMode.LIST) 0 else 1
    }

    inner class ListViewHolder(parent: ViewGroup) : BaseViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.fragment_book_shelf_list_layout, parent, false)) {


        init {
            MDTintHelper.setTint(itemView.progressBar, ContextCompat.getColor(parent.context, R.color.accent))
        }

        override fun bind(book: Book) {
            super.bind(book)

            val globalPosition = book.globalPosition()
            val globalDuration = book.globalDuration
            val progress = Math.round(100f * globalPosition.toFloat() / globalDuration.toFloat())

            itemView.leftTime.text = formatTime(globalPosition)
            itemView.progressBar.progress = progress
            itemView.rightTime.text = formatTime(globalDuration)
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

            indicatorVisible = book.id == prefs.currentBookId.value()
            if (indicatorVisible) {
                currentPlayingIndicator.visibility = View.VISIBLE
            } else {
                currentPlayingIndicator.visibility = View.GONE
            }

            itemView.setOnClickListener { bookClicked(getItem(adapterPosition), ClickType.REGULAR) }
            editBook.setOnClickListener { bookClicked(getItem(adapterPosition), ClickType.MENU) }

            ViewCompat.setTransitionName(coverView, book.coverTransitionName)
        }
    }

    enum class ClickType {
        REGULAR,
        MENU
    }
}