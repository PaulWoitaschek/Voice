package de.ph1b.audiobook.features.bookOverview.list

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.load
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.databinding.BookOverviewRowGridBinding
import de.ph1b.audiobook.databinding.BookOverviewRowListBinding
import de.ph1b.audiobook.misc.RoundRectOutlineProvider
import de.ph1b.audiobook.misc.dpToPx
import de.ph1b.audiobook.misc.layoutInflater
import de.ph1b.audiobook.misc.recyclerComponent.AdapterComponent
import de.ph1b.audiobook.uitools.SquareProgressView
import voice.common.formatTime

class GridBookOverviewComponent(private val listener: BookClickListener) :
  AdapterComponent<BookOverviewViewState, BookOverviewHolder>(BookOverviewViewState::class) {

  override val viewType = 42

  override fun onCreateViewHolder(parent: ViewGroup): BookOverviewHolder {
    val binding = BookOverviewRowGridBinding.inflate(parent.layoutInflater(), parent, false)
    return BookOverviewHolder(
      BookOverviewBinding(
        root = binding.root,
        cover = binding.cover,
        title = binding.title,
        author = null,
        remainingTime = binding.remainingTime,
        playingIndicator = binding.playingIndicator,
        progress = binding.progress
      ),
      listener = listener
    )
  }

  override fun onBindViewHolder(model: BookOverviewViewState, holder: BookOverviewHolder) {
    holder.bind(model)
  }

  override fun isForViewType(model: Any): Boolean {
    return model is BookOverviewViewState && model.useGridView
  }
}

class ListBookOverviewComponent(private val listener: BookClickListener) :
  AdapterComponent<BookOverviewViewState, BookOverviewHolder>(BookOverviewViewState::class) {

  override val viewType = 43

  override fun onCreateViewHolder(parent: ViewGroup): BookOverviewHolder {
    val binding = BookOverviewRowListBinding.inflate(parent.layoutInflater(), parent, false)
    return BookOverviewHolder(
      BookOverviewBinding(
        root = binding.root,
        cover = binding.cover,
        title = binding.title,
        author = binding.author,
        remainingTime = binding.remainingTime,
        playingIndicator = binding.playingIndicator,
        progress = binding.progress
      ),
      listener = listener
    )
  }

  override fun onBindViewHolder(model: BookOverviewViewState, holder: BookOverviewHolder) {
    holder.bind(model)
  }

  override fun isForViewType(model: Any): Boolean {
    return model is BookOverviewViewState && !model.useGridView
  }
}

data class BookOverviewBinding(
  val root: View,
  val cover: ImageView,
  val title: TextView,
  val author: TextView?,
  val remainingTime: TextView,
  val playingIndicator: View,
  val progress: SquareProgressView
)

class BookOverviewHolder(
  private val binding: BookOverviewBinding,
  private val listener: BookClickListener
) : RecyclerView.ViewHolder(binding.root) {

  private var boundBook: Book2.Id? = null

  init {
    binding.cover.clipToOutline = true
    binding.cover.outlineProvider = RoundRectOutlineProvider(itemView.context.dpToPx(2F))
    itemView.setOnClickListener {
      boundBook?.let { book ->
        listener(book, BookOverviewClick.REGULAR)
      }
    }
    itemView.setOnLongClickListener {
      boundBook?.let { book ->
        listener(book, BookOverviewClick.MENU)
        true
      } ?: false
    }
  }

  fun bind(model: BookOverviewViewState) {
    boundBook = model.id
    val name = model.name
    binding.title.text = name
    binding.author?.text = model.author
    binding.author?.isVisible = model.author != null
    binding.title.maxLines = if (binding.author?.isVisible == true) 1 else 2
    binding.cover.transitionName = model.transitionName
    binding.remainingTime.text = formatTime(model.remainingTimeInMs)
    binding.progress.progress = model.progress
    val cover = model.cover
    if (cover == null) {
      binding.cover.load(R.drawable.default_album_art)
    } else {
      binding.cover.load(cover) {
        fallback(R.drawable.default_album_art)
      }
    }
    binding.playingIndicator.isVisible = model.isCurrentBook
  }
}
