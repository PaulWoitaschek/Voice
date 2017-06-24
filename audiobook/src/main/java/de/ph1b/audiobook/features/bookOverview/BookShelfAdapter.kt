package de.ph1b.audiobook.features.bookOverview

import android.content.Context
import android.support.annotation.CallSuper
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.afollestad.materialdialogs.internal.MDTintHelper
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.*
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.uitools.CoverReplacement
import de.ph1b.audiobook.uitools.maxImageSize
import de.ph1b.audiobook.uitools.visible
import i
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// display all the books
class BookShelfAdapter(private val context: Context, private val bookClicked: (Book, ClickType) -> Unit) : RecyclerView.Adapter<BookShelfAdapter.BaseViewHolder>() {

  private val books = ArrayList<Book>()

  @Inject lateinit var prefs: PrefsManager

  init {
    i { "A new adapter was created." }
    App.component.inject(this)
    setHasStableIds(true)
  }

  private fun formatTime(ms: Int): String {
    val h = "%02d".format((TimeUnit.MILLISECONDS.toHours(ms.toLong())))
    val m = "%02d".format((TimeUnit.MILLISECONDS.toMinutes(ms.toLong()) % 60))
    return h + ":" + m
  }

  /** Adds a new set of books and removes the ones that do not exist any longer **/
  fun newDataSet(newBooks: List<Book>) {
    val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {

      override fun getOldListSize(): Int = books.size

      override fun getNewListSize(): Int = newBooks.size

      override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = books[oldItemPosition]
        val newItem = newBooks[newItemPosition]
        return oldItem.id == newItem.id && oldItem.globalPosition() == newItem.globalPosition() && oldItem.name == newItem.name
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

  fun reloadBookCover(bookId: Long) {
    val index = books.indexOfFirst { it.id == bookId }
    if (index >= 0) {
      notifyItemChanged(index)
    }
  }

  override fun getItemId(position: Int) = books[position].id

  fun getItem(position: Int): Book = books[position]

  var displayMode: BookShelfController.DisplayMode = BookShelfController.DisplayMode.LIST
    set(value) {
      if (value != field) {
        field = value
        i { "displayMode changed to $field" }
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

  override fun onBindViewHolder(holder: BaseViewHolder, position: Int, payloads: MutableList<Any>) = when {
    payloads.isEmpty() -> onBindViewHolder(holder, position)
    else -> holder.bind(books[position])
  }

  override fun getItemCount(): Int = books.size

  override fun getItemViewType(position: Int): Int = if (displayMode == BookShelfController.DisplayMode.LIST) 0 else 1

  inner class ListViewHolder(parent: ViewGroup) : BaseViewHolder(parent.layoutInflater().inflate(R.layout.book_shelf_list_layout, parent, false)) {

    private val progressBar = find<ProgressBar>(R.id.progressBar)
    private val leftTime: TextView = find(R.id.leftTime)
    private val rightTime: TextView = find(R.id.rightTime)

    init {
      MDTintHelper.setTint(progressBar, parent.context.color(R.color.accent))
    }

    override fun bind(book: Book) {
      super.bind(book)

      val globalPosition = book.globalPosition()
      val globalDuration = book.globalDuration
      val progress = Math.round(100f * globalPosition.toFloat() / globalDuration.toFloat())

      leftTime.text = formatTime(globalPosition)
      progressBar.progress = progress
      rightTime.text = formatTime(globalDuration)
    }
  }

  /** ViewHolder for the grid **/
  inner class GridViewHolder(parent: ViewGroup) : BaseViewHolder(parent.layoutInflater()
      .inflate(R.layout.book_shelf_grid_layout, parent, false))


  /** ViewHolder base class **/
  abstract inner class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val coverView: ImageView = itemView.find(R.id.coverView)
    private val currentPlayingIndicator: ImageView = itemView.find(R.id.currentPlayingIndicator)
    private val titleView: TextView = itemView.find(R.id.title)
    private val editBook: View = itemView.find(R.id.editBook)
    var indicatorVisible = false
      private set


    /** Binds the ViewHolder to a book */
    @CallSuper
    open fun bind(book: Book) {

      //setting text
      val name = book.name
      titleView.text = name

      bindCover(book)

      indicatorVisible = book.id == prefs.currentBookId.value
      currentPlayingIndicator.visible = indicatorVisible

      itemView.setOnClickListener { bookClicked(getItem(adapterPosition), ClickType.REGULAR) }
      editBook.setOnClickListener { bookClicked(getItem(adapterPosition), ClickType.MENU) }

      coverView.supportTransitionName = book.coverTransitionName
    }

    private fun bindCover(book: Book) {
      // (Cover)
      val coverFile = book.coverFile()
      val coverReplacement = CoverReplacement(book.name, context)

      if (coverFile.canRead() && coverFile.length() < maxImageSize) {
        Picasso.with(context)
            .load(coverFile)
            .placeholder(coverReplacement)
            .into(coverView)
      } else {
        Picasso.with(context)
            .cancelRequest(coverView)
        // we have to set the replacement in onPreDraw, else the transition will fail.
        coverView.onFirstPreDraw { coverView.setImageDrawable(coverReplacement) }
      }
    }
  }

  enum class ClickType {
    REGULAR,
    MENU
  }
}
