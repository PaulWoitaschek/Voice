package de.ph1b.audiobook.features

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.book_overview.BookShelfController
import de.ph1b.audiobook.features.book_playing.BookPlayController
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.PermissionHelper
import de.ph1b.audiobook.misc.setupActionbar
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.PrefsManager
import kotlinx.android.synthetic.main.activity_book.*
import kotlinx.android.synthetic.main.toolbar.*
import java.io.File
import javax.inject.Inject

/**
 * Activity that coordinates the book shelf and play screens.

 * @author Paul Woitaschek
 */
class BookActivity : BaseActivity() {

    @Inject lateinit var prefs: PrefsManager
    @Inject lateinit var permissionHelper: PermissionHelper

    private lateinit var router: Router

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book)
        App.component().inject(this)

        setupActionbar(toolbar!!)

        router = Conductor.attachRouter(this, container, savedInstanceState)
        if (!router.hasRootController()) {
            router.setRoot(RouterTransaction.with(BookShelfController()))
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

        /**
         * Returns an intent to start the activity with to inform the user that a certain file may be
         * defect

         * @param c             The context
         * *
         * @param malformedFile The defect file
         * *
         * @return The intent to start the activity with.
         */
        fun malformedFileIntent(c: Context, malformedFile: File) = Intent(c, BookActivity::class.java).apply {
            putExtra(NI_MALFORMED_FILE, malformedFile)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        /**
         * Returns an intent that lets you go directly to the playback screen for a certain book

         * @param c      The context
         * *
         * @param bookId The book id to target
         * *
         * @return The intent
         */
        fun goToBookIntent(c: Context, bookId: Long) = Intent(c, BookActivity::class.java).apply {
            putExtra(NI_GO_TO_BOOK, bookId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
