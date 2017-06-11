package de.ph1b.audiobook.features

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.view.ViewGroup
import com.bluelinelabs.conductor.*
import de.ph1b.audiobook.R
import de.ph1b.audiobook.databinding.ActivityBookBinding
import de.ph1b.audiobook.features.bookOverview.BookShelfController
import de.ph1b.audiobook.features.bookOverview.NoFolderWarningDialogFragment
import de.ph1b.audiobook.features.bookPlaying.BookPlayController
import de.ph1b.audiobook.features.bookSearch.BookSearchHandler
import de.ph1b.audiobook.features.bookSearch.BookSearchParser
import de.ph1b.audiobook.features.folderOverview.FolderOverviewController
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.*
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayerController
import javax.inject.Inject


/**
 * Activity that coordinates the book shelf and play screens.
 *
 * @author Paul Woitaschek
 */
class MainActivity : BaseActivity(), NoFolderWarningDialogFragment.Callback, RouterProvider {

  private lateinit var permissionHelper: PermissionHelper
  private lateinit var permissions: Permissions
  @Inject lateinit var prefs: PrefsManager
  @Inject lateinit var playerController: PlayerController
  @Inject lateinit var repo: BookRepository
  @Inject lateinit var bookSearchParser: BookSearchParser
  @Inject lateinit var bookSearchHandler: BookSearchHandler

  private lateinit var router: Router

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val binding = DataBindingUtil.setContentView<ActivityBookBinding>(this, R.layout.activity_book)
    App.component.inject(this)

    permissions = Permissions(this)
    permissionHelper = PermissionHelper(this, permissions)

    router = Conductor.attachRouter(this, binding.root, savedInstanceState)
    if (!router.hasRootController()) {
      setupRouter()
    }

    router.addChangeListener(object : ControllerChangeHandler.ControllerChangeListener {
      override fun onChangeStarted(to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ControllerChangeHandler) {
        from?.setOptionsMenuHidden(true)
      }

      override fun onChangeCompleted(to: Controller?, from: Controller?, isPush: Boolean, container: ViewGroup, handler: ControllerChangeHandler) {
        from?.setOptionsMenuHidden(false)
      }
    })

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
    repo.bookById(intent.getLongExtra(NI_GO_TO_BOOK, -1))?.let {
      val bookShelf = RouterTransaction.with(BookShelfController())
      val bookPlay = BookPlayController(it.id).asTransaction()
      router.setBackstack(listOf(bookShelf, bookPlay), null)
      return
    }

    // if we should play the current book, set the backstack and return early
    if (intent.getBooleanExtra(NI_PLAY_CURRENT_BOOK_IMMEDIATELY, false)) {
      repo.bookById(prefs.currentBookId.get()!!)?.let {
        val bookShelf = RouterTransaction.with(BookShelfController())
        val bookPlay = BookPlayController(it.id).asTransaction()
        router.setBackstack(listOf(bookShelf, bookPlay), null)
        playerController.play()
        return
      }
    }

    val rootTransaction = RouterTransaction.with(BookShelfController())
    router.setRoot(rootTransaction)
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    this.permissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
  }

  override fun provideRouter() = router

  override fun onStart() {
    super.onStart()

    val anyFolderSet = prefs.collectionFolders.value.size + prefs.singleBookFolders.value.size > 0
    if (anyFolderSet) {
      permissionHelper.storagePermission()
    }
  }

  override fun onBackPressed() {
    if (!router.handleBack()) super.onBackPressed()
  }

  companion object {
    private const val NI_GO_TO_BOOK = "niGotoBook"
    private const val NI_PLAY_CURRENT_BOOK_IMMEDIATELY = "ni#playCurrentBookImmediately"

    /** Returns an intent that lets you go directly to the playback screen for a certain book **/
    fun goToBookIntent(c: Context, bookId: Long) = Intent(c, MainActivity::class.java).apply {
      putExtra(NI_GO_TO_BOOK, bookId)
      flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
    }

    fun newIntent(context: Context, playCurrentBookImmediately: Boolean) = Intent(context, MainActivity::class.java).apply {
      putExtra(NI_PLAY_CURRENT_BOOK_IMMEDIATELY, playCurrentBookImmediately)
    }
  }

  override fun onNoFolderWarningConfirmed() {
    val transaction = FolderOverviewController().asTransaction()
    router.pushController(transaction)
  }
}
