package de.ph1b.audiobook.adapter

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import de.ph1b.audiobook.R

class FolderOverviewAdapter(private val c: Context,
                            private val bookCollections: MutableList<String>,
                            private val singleBooks: MutableList<String>,
                            private val listener: FolderOverviewAdapter.OnFolderMoreClickedListener) : RecyclerView.Adapter<FolderOverviewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.activity_folder_overview_row_layout, parent, false)
        return ViewHolder(v, listener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = getItem(position)
        holder.textView.text = file

        if (bookCollections.contains(file)) {
            holder.icon.setImageDrawable(ContextCompat.getDrawable(c, R.drawable.folder_multiple))
            holder.icon.contentDescription = c.getString(R.string.folder_add_collection)
        } else {
            holder.icon.setImageDrawable(ContextCompat.getDrawable(c, R.drawable.ic_folder))
            holder.icon.contentDescription = c.getString(R.string.folder_add_single_book)
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return bookCollections.size + singleBooks.size
    }

    fun getItem(position: Int): String {
        if (bookCollections.size > position) {
            return bookCollections[position]
        } else {
            return singleBooks[position - bookCollections.size]
        }
    }

    interface OnFolderMoreClickedListener {
        fun onFolderMoreClicked(position: Int)
    }

    class ViewHolder(itemView: View, listener: OnFolderMoreClickedListener) : RecyclerView.ViewHolder(itemView) {

        internal val icon: ImageView
        internal val textView: TextView

        init {
            icon = itemView.findViewById(R.id.icon) as ImageView
            textView = itemView.findViewById(R.id.containing) as TextView
            itemView.findViewById(R.id.remove).setOnClickListener { listener.onFolderMoreClicked(adapterPosition) }
        }
    }
}
