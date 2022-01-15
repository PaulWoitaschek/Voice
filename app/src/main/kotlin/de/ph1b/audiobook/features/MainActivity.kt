package de.ph1b.audiobook.features

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import de.ph1b.audiobook.common.pref.CurrentBook
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.databinding.ActivityBookBinding
import de.ph1b.audiobook.features.bookOverview.BookOverviewController
import de.ph1b.audiobook.features.bookPlaying.BookPlayController
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.RouterProvider
import de.ph1b.audiobook.misc.conductor.asTransaction
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.playback.session.search.BookSearchHandler
import de.ph1b.audiobook.playback.session.search.BookSearchParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Activity that coordinates the book shelf and play screens.
 */
class MainActivity : AppCompatActivity(), RouterProvider {

  @field:[Inject CurrentBook]
  lateinit var currentBook: DataStore<Uri?>

  @Inject
  lateinit var repo: BookRepository

  @Inject
  lateinit var bookSearchParser: BookSearchParser

  @Inject
  lateinit var bookSearchHandler: BookSearchHandler

  @Inject
  lateinit var playerController: PlayerController

  private lateinit var router: Router

  override fun onCreate(savedInstanceState: Bundle?) {
    appComponent.inject(this)
    super.onCreate(savedInstanceState)
    val binding = ActivityBookBinding.inflate(layoutInflater)
    setContentView(binding.root)

    router = Conductor.attachRouter(this, binding.root, savedInstanceState)
    if (!router.hasRootController()) {
      setupRouter()
    }

    router.addChangeListener(
      object : ControllerChangeHandler.ControllerChangeListener {
        override fun onChangeStarted(
          to: Controller?,
          from: Controller?,
          isPush: Boolean,
          container: ViewGroup,
          handler: ControllerChangeHandler
        ) {
          from?.setOptionsMenuHidden(true)
        }

        override fun onChangeCompleted(
          to: Controller?,
          from: Controller?,
          isPush: Boolean,
          container: ViewGroup,
          handler: ControllerChangeHandler
        ) {
          from?.setOptionsMenuHidden(false)
        }
      }
    )

    setupFromIntent(intent)
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    setupFromIntent(intent)
  }

  private fun setupFromIntent(intent: Intent?) {
    bookSearchParser.parse(intent)?.let { bookSearchHandler.handle(it) }
  }

  private fun setupRouter() {
    // if we should enter a book set the backstack and return early
    intent.getStringExtra(NI_GO_TO_BOOK)
      ?.let {
        val bookId = Uri.parse(it)
        val bookShelf = RouterTransaction.with(BookOverviewController())
        val bookPlay = BookPlayController(bookId).asTransaction()
        router.setBackstack(listOf(bookShelf, bookPlay), null)
        return
      }

    // if we should play the current book, set the backstack and return early
    if (intent?.action == "playCurrent") {
      runBlocking { currentBook.data.first() }?.let { bookId ->
        val bookShelf = RouterTransaction.with(BookOverviewController())
        val bookPlay = BookPlayController(bookId).asTransaction()
        router.setBackstack(listOf(bookShelf, bookPlay), null)
        playerController.play()
        return
      }
    }

    val rootTransaction = RouterTransaction.with(BookOverviewController())
    router.setRoot(rootTransaction)
  }

  override fun provideRouter() = router

  override fun onBackPressed() {
    if (router.backstackSize == 1) {
      super.onBackPressed()
    } else router.handleBack()
  }

  companion object {
    private const val NI_GO_TO_BOOK = "niGotoBook"

    /** Returns an intent that lets you go directly to the playback screen for a certain book **/
    fun goToBookIntent(context: Context, bookId: Uri) = Intent(context, MainActivity::class.java).apply {
      putExtra(NI_GO_TO_BOOK, bookId.toString())
      flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
    }
  }
}
