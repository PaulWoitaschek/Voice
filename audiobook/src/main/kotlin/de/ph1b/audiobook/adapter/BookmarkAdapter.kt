package de.ph1b.audiobook.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.model.Bookmark
import de.ph1b.audiobook.model.Chapter
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Adapter for displaying a list of bookmarks.

 * @author Paul Woitaschek
 */
class BookmarkAdapter(bookmarks: List<Bookmark>, chapters: List<Chapter>, private val listener: BookmarkAdapter.OnOptionsMenuClickedListener) : RecyclerView.Adapter<BookmarkAdapter.ViewHolder>() {

    private val bookmarks: MutableList<Bookmark>
    private val chapters: List<Chapter>

    @Inject
    internal lateinit var c: Context


    init {
        App.getComponent().inject(this)
        this.bookmarks = ArrayList(bookmarks)
        this.chapters = ArrayList(chapters)
    }

    private fun formatTime(ms: Int): String {
        val h = TimeUnit.MILLISECONDS.toHours(ms.toLong()).toString()
        val m = "%02d".format((TimeUnit.MILLISECONDS.toMinutes(ms.toLong()) % 60))
        val s = "%02d".format((TimeUnit.MILLISECONDS.toSeconds(ms.toLong()) % 60))
        var returnString = ""
        if (h != "0") {
            returnString += h + ":"
        }
        returnString += m + ":" + s
        return returnString
    }

    fun removeItem(bookmark: Bookmark) {
        val index = bookmarks.indexOf(bookmark)
        bookmarks.remove(bookmark)
        notifyItemRemoved(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.dialog_bookmark_row_layout, parent, false)
        return ViewHolder(v, listener)
    }

    fun bookmarkUpdated(oldBookmark: Bookmark, newBookmark: Bookmark) {
        val oldIndex = bookmarks.indexOf(oldBookmark)
        bookmarks[oldIndex] = newBookmark
        notifyItemChanged(oldIndex)
        Collections.sort(bookmarks)
        notifyItemMoved(oldIndex, bookmarks.indexOf(newBookmark))
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bookmark = bookmarks[position]
        holder.title.text = bookmark.title

        val size = chapters.size
        var currentChapter = chapters.single { it.file == bookmark.mediaFile }
        val index = chapters.indexOfRaw(currentChapter)

        holder.summary.text = c.getString(R.string.format_bookmarks_n_of, index + 1, size)
        holder.time.text = c.getString(R.string.format_bookmarks_time, formatTime(bookmark.time),
                formatTime(currentChapter.duration))
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return bookmarks.size
    }

    interface OnOptionsMenuClickedListener {
        fun onOptionsMenuClicked(bookmark: Bookmark, v: View)

        fun onBookmarkClicked(bookmark: Bookmark)
    }

    internal inner class ViewHolder(itemView: View, listener: OnOptionsMenuClickedListener) : RecyclerView.ViewHolder(itemView) {

        private val imageButton: ImageButton
        internal val title: TextView
        internal val summary: TextView
        internal val time: TextView

        init {
            imageButton = itemView.findViewById(R.id.edit) as ImageButton
            title = itemView.findViewById(R.id.text1) as TextView
            summary = itemView.findViewById(R.id.text2) as TextView
            time = itemView.findViewById(R.id.text3) as TextView

            imageButton.setOnClickListener { listener.onOptionsMenuClicked(bookmarks[adapterPosition], imageButton) }
            itemView.setOnClickListener { listener.onBookmarkClicked(bookmarks[adapterPosition]) }
        }
    }
}