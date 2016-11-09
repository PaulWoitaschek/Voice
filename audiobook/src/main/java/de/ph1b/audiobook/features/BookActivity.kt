package de.ph1b.audiobook.features

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.book_overview.BookShelfController
import de.ph1b.audiobook.features.book_overview.EditBookBottomSheet
import de.ph1b.audiobook.features.book_overview.EditCoverDialogFragment
import de.ph1b.audiobook.features.book_overview.NoFolderWarningDialogFragment
import de.ph1b.audiobook.features.book_playing.BookPlayController
import de.ph1b.audiobook.features.folder_overview.FolderOverviewController
import de.ph1b.audiobook.features.imagepicker.ImagePickerController
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.PermissionHelper
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.PrefsManager
import java.io.File
import javax.inject.Inject

/**
 * Activity that coordinates the book shelf and play screens.
 *
 * @author Paul Woitaschek
 */
class BookActivity : BaseActivity(), NoFolderWarningDialogFragment.Callback, EditBookBottomSheet.Callback, EditCoverDialogFragment.Callback {

    @Inject lateinit var prefs: PrefsManager
    @Inject lateinit var permissionHelper: PermissionHelper

    private lateinit var router: Router

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book)
        App.component().inject(this)

        val root = findViewById(R.id.root) as ViewGroup
        router = Conductor.attachRouter(this, root, savedInstanceState)
        if (!router.hasRootController()) {
            val rootTransaction = RouterTransaction.with(BookShelfController())
                    .tag(TAG_BOOKSHELF_CONTROLLER)
            router.setRoot(rootTransaction)
        }

        if (savedInstanceState == null) {
            if (intent.hasExtra(NI_MALFORMED_FILE)) {
                val malformedFile = intent.getSerializableExtra(NI_MALFORMED_FILE) as File
                MaterialDialog.Builder(this).title(R.string.mal_file_title)
                        .content(getString(R.string.mal_file_message) + "\n\n" + malformedFile)
                        .show()
            }
            if (intent.hasExtra(NI_GO_TO_BOOK)) {
                val bookId = intent.getLongExtra(NI_GO_TO_BOOK, -1)
                router.pushController(RouterTransaction.with(BookPlayController.newInstance(bookId)))
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val anyFolderSet = prefs.collectionFolders.value().size + prefs.singleBookFolders.value().size > 0
        if (anyFolderSet) {
            permissionHelper.storagePermission(this)
        }
    }

    override fun onBackPressed() {
        if (!router.handleBack()) super.onBackPressed()
    }

    companion object {
        private val NI_MALFORMED_FILE = "malformedFile"
        private val NI_GO_TO_BOOK = "niGotoBook"
        private val TAG_BOOKSHELF_CONTROLLER = BookShelfController::class.java.simpleName


        /** Returns an intent to start the activity with to inform the user that a certain file may be defect **/
        fun malformedFileIntent(c: Context, malformedFile: File) = Intent(c, BookActivity::class.java).apply {
            putExtra(NI_MALFORMED_FILE, malformedFile)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        /** Returns an intent that lets you go directly to the playback screen for a certain book **/
        fun goToBookIntent(c: Context, bookId: Long) = Intent(c, BookActivity::class.java).apply {
            putExtra(NI_GO_TO_BOOK, bookId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    private fun bookShelfController() = router.getControllerWithTag(TAG_BOOKSHELF_CONTROLLER) as BookShelfController

    override fun onBookCoverChanged(book: Book) {
        val bookShelfController = bookShelfController()
        bookShelfController.bookCoverChanged(book)
    }

    override fun onNoFolderWarningConfirmed() {
        router.pushController(RouterTransaction.with(FolderOverviewController()))
    }

    override fun onInternetCoverRequested(book: Book) {
        router.pushController(RouterTransaction.with(ImagePickerController(book)))
    }

    override fun onFileCoverRequested(book: Book) {
        val bookShelfController = bookShelfController()
        bookShelfController.changeCover(book)
    }
}
