package de.ph1b.audiobook.features.folder_chooser

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import de.ph1b.audiobook.R
import de.ph1b.audiobook.misc.FileRecognition
import de.ph1b.audiobook.misc.drawable
import de.ph1b.audiobook.misc.layoutInflater
import kotlinx.android.synthetic.main.activity_folder_chooser_adapter_row_layout.view.*
import java.io.File
import java.util.*


/**
 * Adapter for displaying files and folders.
 * Constructor that initializes the class with the necessary values
 *
 * @param c    The context
 * @param mode The operation mode which defines the interaction.
 *
 * @author Paul Woitaschek
 */
class FolderChooserAdapter(private val c: Context,
                           private val mode: FolderChooserActivity.OperationMode,
                           private val listener: (selected: File) -> Unit)
: RecyclerView.Adapter<FolderChooserAdapter.Holder>() {

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(data[position])

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = parent.layoutInflater().inflate(R.layout.activity_folder_chooser_adapter_row_layout, parent, false)
        return Holder(view)
    }

    override fun getItemCount() = data.size

    private val data = ArrayList<File>()
    private val selectedItems = ArrayList<Boolean>()

    fun checkFolder(folder : File) : Boolean {
        if (!folder.isDirectory)
            return false
        val listFiles = folder.listFiles(FileRecognition.folderAndMusicFilter)
        for (item in listFiles) {
            if (FileRecognition.musicFilter.accept(item)) {
                return true
            } else {
                if (item.isDirectory) {
                    return checkFolder(item)
                }
            }
        }
        return false
    }

    fun newData(newData: List<File>) {
        data.clear()
        selectedItems.clear()

        for (item in newData) {
            if (item.isDirectory && checkFolder(item)) {
                data.add(item)
            }
        }

//        data.addAll(newData)

        for (item in data) {
            selectedItems.add(false)
        }

        notifyDataSetChanged()
    }

    inner class Holder(private val root: View) : RecyclerView.ViewHolder(root) {

        init {
            root.setOnClickListener {
                selectedItems[adapterPosition] = !selectedItems[adapterPosition]
                root.checkBox.setChecked(selectedItems[adapterPosition])
            }
        }

        fun bind(selectedFile: File) {
            val isDirectory = selectedFile.isDirectory

            root.text.text = selectedFile.name

            // if its not a collection its also fine to pick a file
            if (mode == FolderChooserActivity.OperationMode.COLLECTION_BOOK) {
                root.text.isEnabled = isDirectory
            }

            if (selectedItems[adapterPosition] == true) {
                root.checkBox.setChecked(true)
            } else {
                root.checkBox.setChecked(false)
            }

            val icon = c.drawable(if (isDirectory) R.drawable.ic_folder else R.drawable.ic_album)
            root.icon.setImageDrawable(icon)
            root.icon.contentDescription =
                    c.getString(if (isDirectory) R.string.content_is_folder else R.string.content_is_file)
        }
    }
}