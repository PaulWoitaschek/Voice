package de.ph1b.audiobook.features.folder_overview

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import de.ph1b.audiobook.R
import de.ph1b.audiobook.misc.layoutInflater
import java.util.*

class FolderOverviewAdapter(private val deleteClicked: (toDelete: FolderModel) -> Unit) :
        RecyclerView.Adapter<FolderOverviewHolder>() {

    private val items = ArrayList<FolderModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderOverviewHolder {
        val view = parent.layoutInflater().inflate(R.layout.activity_folder_overview_row_layout, parent, false)
        return FolderOverviewHolder(view) {
            deleteClicked(items[it])
        }
    }

    override fun onBindViewHolder(holder: FolderOverviewHolder, position: Int) {
        val model = items[position]
        holder.bind(model)
    }

    fun newItems(newItems: Collection<FolderModel>) {
        val newItemsSorted = newItems.sorted()
        val hadItems = this.items.isNotEmpty()
        val diff = FolderOverviewDiffHelper.diff(this.items, newItemsSorted)
        this.items.clear()
        this.items.addAll(newItemsSorted)
        if (hadItems) diff.dispatchUpdatesTo(this)
        else notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size
}