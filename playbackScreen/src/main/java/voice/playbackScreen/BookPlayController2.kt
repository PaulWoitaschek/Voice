package voice.playbackScreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import com.bluelinelabs.conductor.Controller
import com.squareup.anvil.annotations.ContributesTo
import de.ph1b.audiobook.AppScope
import de.ph1b.audiobook.rootComponentAs
import java.util.UUID
import voice.playbackScreen.views.BookPlayView
import javax.inject.Inject

private const val NI_BOOK_ID = "ni#bookId"

class BookPlayController2(bundle: Bundle) : Controller(bundle) {

  constructor(bookId: UUID) : this(Bundle().apply { putSerializable(NI_BOOK_ID, bookId) })

  @Inject
  lateinit var viewModel: BookPlayViewModel

  init {
    rootComponentAs<Component>().inject(this)
    viewModel.bookId = args.getSerializable(NI_BOOK_ID) as UUID
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
    val listener = object : BookPlayListener {
      override fun close() {
        router.popController(this@BookPlayController2)
      }

      override fun fastForward() {
        viewModel.fastForward()
      }

      override fun rewind() {
        viewModel.rewind()
      }

      override fun playPause() {
        viewModel.playPause()
      }

      override fun seekTo(milliseconds: Long) {
        viewModel.seekTo(milliseconds)
      }

      override fun previousTrack() {
        viewModel.previous()
      }

      override fun nextTrack() {
        viewModel.next()
      }
    }
    return ComposeView(container.context).apply {
      setContent {
        val viewState = viewModel.viewState().collectAsState(null)
        viewState.value?.let {
          BookPlayView(it, listener)
        }
      }
    }
  }

  @ContributesTo(AppScope::class)
  interface Component {
    fun inject(target: BookPlayController2)
  }
}
