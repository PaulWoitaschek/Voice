package de.ph1b.audiobook.features.bookOverview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.support.v7.graphics.Palette
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.misc.coverFile
import de.ph1b.audiobook.misc.layoutInflater
import de.ph1b.audiobook.misc.onFirstPreDraw
import de.ph1b.audiobook.misc.supportTransitionName
import de.ph1b.audiobook.uitools.CoverReplacement
import de.ph1b.audiobook.uitools.maxImageSize
import de.ph1b.audiobook.uitools.visible
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.book_shelf_row.*
import java.util.*

class BookShelfAdapter(
    private val context: Context,
    private val bookClicked: (Book, ClickType) -> Unit
) : RecyclerView.Adapter<BookShelfAdapter.ViewHolder>() {

  private val books = ArrayList<Book>()

  init {
    setHasStableIds(true)
  }

  fun newDataSet(newBooks: List<Book>) {
    val oldBooks = books.toList()
    books.clear()
    books.addAll(newBooks)
    val callback = BookShelfDiffCallback(oldBooks = oldBooks, newBooks = books)
    val diffResult = DiffUtil.calculateDiff(callback, false)
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

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent)

  override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(books[position])

  override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) = when {
    payloads.isEmpty() -> onBindViewHolder(holder, position)
    else -> holder.bind(books[position])
  }

  override fun getItemCount(): Int = books.size

  inner class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
      parent.layoutInflater().inflate(
          R.layout.book_shelf_row,
          parent,
          false
      )
  ), LayoutContainer {

    override val containerView: View? get() = itemView

    init {
      itemView.clipToOutline = true
    }

    val coverView: ImageView = cover

    fun bind(book: Book) {
      val name = book.name
      title.text = name
      author.text = book.author
      author.visible = book.author != null
      title.maxLines = if (book.author == null) 2 else 1
      bindCover(book)

      itemView.setOnClickListener { bookClicked(getItem(adapterPosition), ClickType.REGULAR) }
      edit.setOnClickListener { bookClicked(getItem(adapterPosition), ClickType.MENU) }

      coverView.supportTransitionName = book.coverTransitionName

      val globalPosition = book.position
      val totalDuration = book.duration
      val progress = globalPosition.toFloat() / totalDuration.toFloat()

      this.progress.progress = progress
    }

    private fun bindCover(book: Book) {
      // (Cover)
      val coverFile = book.coverFile()
      val coverReplacement = CoverReplacement(book.name, context)

      if (coverFile.canRead() && coverFile.length() < maxImageSize) {
        Picasso.with(context)
            .load(coverFile)
            .placeholder(coverReplacement)
            .into(object : Target {
              override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                coverView.setImageDrawable(placeHolderDrawable)
              }

              override fun onBitmapFailed(errorDrawable: Drawable?) {
              }

              override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                bitmap?.let {
                  coverView.setImageBitmap(it)
                  Palette.from(it)
                      .generate {
                        val color = it.getVibrantColor(Color.BLACK)
                        progress.color = color
                      }
                }
              }
            })
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
