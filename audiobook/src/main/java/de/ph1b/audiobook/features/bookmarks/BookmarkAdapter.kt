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

package de.ph1b.audiobook.features.bookmarks

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import de.ph1b.audiobook.Bookmark
import de.ph1b.audiobook.Chapter
import de.ph1b.audiobook.R
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Adapter for displaying a list of bookmarks.

 * @author Paul Woitaschek
 */
class BookmarkAdapter(private val chapters: List<Chapter>, private val listener: OnOptionsMenuClickedListener, private val context: Context) : RecyclerView.Adapter<BookmarkAdapter.ViewHolder>() {

    private val bookmarks = ArrayList<Bookmark>()

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

    fun remove(bookmark: Bookmark) {
        val index = bookmarks.indexOf(bookmark)
        bookmarks.remove(bookmark)
        notifyItemRemoved(index)
    }

    fun add(bookmark: Bookmark) {
        bookmarks.add(bookmark)
        bookmarks.sort()
        val index = bookmarks.indexOf(bookmark)
        notifyItemInserted(index)
    }

    fun addAll(bookmarks:Iterable<Bookmark>){
        this.bookmarks.addAll(bookmarks)
        this.bookmarks.sort()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.dialog_bookmark_row_layout, parent, false)
        return ViewHolder(v, listener)
    }

    fun replace(oldBookmark: Bookmark, newBookmark: Bookmark) {
        val oldIndex = bookmarks.indexOf(oldBookmark)
        bookmarks[oldIndex] = newBookmark
        notifyItemChanged(oldIndex)
        bookmarks.sort()
        notifyItemMoved(oldIndex, bookmarks.indexOf(newBookmark))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bookmark = bookmarks[position]
        holder.title.text = bookmark.title

        val size = chapters.size
        var currentChapter = chapters.single { it.file == bookmark.mediaFile }
        val index = chapters.indexOf(currentChapter)

        holder.summary.text = context.getString(R.string.format_bookmarks_n_of, index + 1, size)
        holder.time.text = context.getString(R.string.format_bookmarks_time, formatTime(bookmark.time),
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

    inner class ViewHolder(itemView: View, listener: OnOptionsMenuClickedListener) : RecyclerView.ViewHolder(itemView) {

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