package de.ph1b.audiobook.features.bookOverview.list

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.support.v7.graphics.Palette
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.misc.coverFile
import de.ph1b.audiobook.misc.layoutInflater
import de.ph1b.audiobook.misc.onFirstPreDraw
import de.ph1b.audiobook.uitools.CoverReplacement
import de.ph1b.audiobook.uitools.maxImageSize
import de.ph1b.audiobook.uitools.visible
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.book_shelf_row.*

class BookShelfHolder(parent: ViewGroup, listener: (Book, BookShelfClick) -> Unit) : RecyclerView.ViewHolder(
    parent.layoutInflater().inflate(
        R.layout.book_shelf_row,
        parent,
        false
    )
), LayoutContainer {

  override val containerView: View? get() = itemView
  private var boundBook: Book? = null

  init {
    itemView.clipToOutline = true
    itemView.setOnClickListener {
      boundBook?.let { listener(it, BookShelfClick.REGULAR) }
    }
    edit.setOnClickListener {
      boundBook?.let { listener(it, BookShelfClick.MENU) }
    }
  }

  fun bind(book: Book) {
    boundBook = book
    val name = book.name
    title.text = name
    author.text = book.author
    author.visible = book.author != null
    title.maxLines = if (book.author == null) 2 else 1
    bindCover(book)

    cover.transitionName = book.coverTransitionName

    val globalPosition = book.position
    val totalDuration = book.duration
    val progress = globalPosition.toFloat() / totalDuration.toFloat()

    this.progress.progress = progress
  }

  private fun bindCover(book: Book) {
    val coverFile = book.coverFile()
    val coverReplacement = CoverReplacement(book.name, itemView.context)

    if (coverFile.canRead() && coverFile.length() < maxImageSize) {
      Picasso.with(itemView.context)
          .load(coverFile)
          .placeholder(coverReplacement)
          .into(object : Target {
            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
              cover.setImageDrawable(placeHolderDrawable)
            }

            override fun onBitmapFailed(errorDrawable: Drawable?) {
            }

            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
              bitmap?.let {
                cover.setImageBitmap(it)
                Palette.from(it)
                    .generate {
                      val color = it.getVibrantColor(Color.BLACK)
                      progress.color = color
                    }
              }
            }
          })
    } else {
      Picasso.with(itemView.context)
          .cancelRequest(cover)
      // we have to set the replacement in onPreDraw, else the transition will fail.
      cover.onFirstPreDraw { cover.setImageDrawable(coverReplacement) }
    }
  }
}
