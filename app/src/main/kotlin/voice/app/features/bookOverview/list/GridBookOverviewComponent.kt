package voice.app.features.bookOverview.list

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.progressindicator.LinearProgressIndicator
import voice.app.R
import voice.app.databinding.BookOverviewRowGridBinding
import voice.app.databinding.BookOverviewRowListBinding
import voice.app.misc.RoundRectOutlineProvider
import voice.app.misc.dpToPx
import voice.app.misc.layoutInflater
import voice.app.misc.recyclerComponent.AdapterComponent
import voice.common.colorFromAttr
import voice.common.formatTime
import voice.data.Book
import java.io.File
import kotlin.math.roundToInt

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
  val progress: LinearProgressIndicator
)

class BookOverviewHolder(
  private val binding: BookOverviewBinding,
  private val listener: BookClickListener
) : RecyclerView.ViewHolder(binding.root) {

  private var boundBook: Book.Id? = null
  private var boundCover: File? = null

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
    val name = model.name
    binding.title.text = name
    binding.author?.text = model.author
    binding.author?.isVisible = model.author != null
    binding.title.maxLines = if (model.useGridView) {
      if (binding.author?.isVisible == true) 1 else 2
    } else {
      Int.MAX_VALUE
    }
    binding.cover.transitionName = model.transitionName
    binding.remainingTime.text = formatTime(model.remainingTimeInMs)
    binding.progress.progress = (model.progress * 100).roundToInt()
    binding.progress.setIndicatorColor(itemView.context.colorFromAttr(
      if (model.isCurrentBook) R.attr.colorPrimary else R.attr.colorSecondary
    ))

    val cover = model.cover
    if (boundCover != cover || boundBook != model.id) {
      binding.cover.load(cover) {
        fallback(R.drawable.album_art)
        error(R.drawable.album_art)
      }
    }
    binding.playingIndicator.isVisible = model.isCurrentBook

    boundBook = model.id
    boundCover = cover
  }
}
