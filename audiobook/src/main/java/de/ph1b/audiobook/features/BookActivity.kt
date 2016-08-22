package de.ph1b.audiobook.features

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.book_overview.BookShelfFragment
import de.ph1b.audiobook.features.book_playing.BookPlayFragment
import de.ph1b.audiobook.features.imagepicker.ImagePickerActivity
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.PermissionHelper
import de.ph1b.audiobook.misc.setupActionbar
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.PrefsManager
import i
import kotlinx.android.synthetic.main.activity_book.*
import kotlinx.android.synthetic.main.toolbar.*
import permissions.dispatcher.*
import v
import java.io.File
import java.util.*
import javax.inject.Inject

/**
 * Activity that coordinates the book shelf and play screens.

 * @author Paul Woitaschek
 */
@RuntimePermissions class BookActivity : BaseActivity(), BookShelfFragment.Callback {

    private val TAG = BookActivity::class.java.simpleName
    private val FM_BOOK_SHELF = TAG + BookShelfFragment.TAG
    private val FM_BOOK_PLAY = TAG + BookPlayFragment.TAG

    @Inject lateinit var prefs: PrefsManager

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        BookActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults)
    }

    // just ensure permissions, dont react
    @NeedsPermission(PermissionHelper.NEEDED_PERMISSION) fun ensurePermissions() {
    }

    @OnShowRationale(PermissionHelper.NEEDED_PERMISSION) fun showRationaleForStorage(request: PermissionRequest) {
        PermissionHelper.showRationaleAndProceed(root, request)
    }

    @OnPermissionDenied(PermissionHelper.NEEDED_PERMISSION) fun denied() {
        BookActivityPermissionsDispatcher.ensurePermissionsWithCheck(this)
    }

    @OnNeverAskAgain(PermissionHelper.NEEDED_PERMISSION) fun deniedForever() {
        PermissionHelper.handleDeniedForever(root)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book)
        App.component().inject(this)

        setupActionbar(toolbar!!)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(container.id, BookShelfFragment(), FM_BOOK_SHELF)
                    .commit()
        }

        if (savedInstanceState == null) {
            if (intent.hasExtra(NI_MALFORMED_FILE)) {
                val malformedFile = intent.getSerializableExtra(NI_MALFORMED_FILE) as File
                MaterialDialog.Builder(this).title(R.string.mal_file_title).content(getString(R.string.mal_file_message) + "\n\n" + malformedFile).show()
            }
            if (intent.hasExtra(NI_GO_TO_BOOK)) {
                val bookId = intent.getLongExtra(NI_GO_TO_BOOK, -1)
                onBookSelected(bookId, HashMap())
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val anyFolderSet = prefs.collectionFolders.value().size + prefs.singleBookFolders.value().size > 0
        if (anyFolderSet) {
            BookActivityPermissionsDispatcher.ensurePermissionsWithCheck(this)
        }
    }

    override fun onBookSelected(bookId: Long, sharedViews: Map<View, String>) {
        i { "onBookSelected with $bookId" }

        val ft = supportFragmentManager.beginTransaction()
        val bookPlayFragment = BookPlayFragment.newInstance(bookId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val move = TransitionInflater.from(this@BookActivity).inflateTransition(android.R.transition.move)
            bookPlayFragment.sharedElementEnterTransition = move
            for (entry in sharedViews.entries) {
                v { "Added sharedElement=$entry" }
                ft.addSharedElement(entry.key, entry.value)
            }
        }

        ft.replace(container.id, bookPlayFragment, FM_BOOK_PLAY)
                .addToBackStack(null)
                .commit()
    }

    override fun onCoverChanged(book: Book) {
        val intent = ImagePickerActivity.newIntent(this, book.id)
        startActivity(intent)
    }

    override fun onBackPressed() {
        val bookShelfFragment = supportFragmentManager.findFragmentByTag(FM_BOOK_SHELF)
        if (bookShelfFragment != null && bookShelfFragment.isVisible) {
            finish()
        } else {
            super.onBackPressed()
        }
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
