package de.ph1b.audiobook.features.folderChooser

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import java.io.File

/**
 * Adapter for displaying files and folders.
 * Constructor that initializes the class with the necessary values
 *
 * @param c The context
 * @param mode The operation mode which defines the interaction.
 */
class FolderChooserAdapter(
  private val c: Context,
  private val mode: FolderChooserActivity.OperationMode,
  private val listener: (selected: File) -> Unit
) : RecyclerView.Adapter<Holder>() {

  override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(data[position])

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
    Holder(parent, mode, listener)

  override fun getItemCount() = data.size

  private val data = ArrayList<File>()

  fun newData(newData: List<File>) {
    if (data == newData)
      return
    data.clear()
    data.addAll(newData)
    notifyDataSetChanged()
  }

}
