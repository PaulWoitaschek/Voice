package de.ph1b.audiobook.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.annotation.IdRes
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentManager
import android.support.v7.widget.Toolbar
import android.transition.TransitionInflater
import android.view.Menu
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.R
import de.ph1b.audiobook.fragment.BookPlayFragment
import de.ph1b.audiobook.fragment.BookShelfFragment
import de.ph1b.audiobook.interfaces.MultiPaneInformer
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.utils.App
import de.ph1b.audiobook.utils.PermissionHelper
import timber.log.Timber
import java.io.File
import java.util.*
import javax.inject.Inject

/**
 * Activity that coordinates the book shelf and play screens.

 * @author Paul Woitaschek
 */
class BookActivity : BaseActivity(), BookShelfFragment.BookSelectionCallback, MultiPaneInformer {

    private val TAG = BookActivity::class.java.simpleName
    private val FM_BOOK_SHELF = TAG + BookShelfFragment.TAG
    private val FM_BOOK_PLAY = TAG + BookPlayFragment.TAG
    @IdRes private val CONTAINER_PLAY = R.id.play_container
    @IdRes private val CONTAINER_SHELF = R.id.shelf_container

    @Inject internal lateinit var prefs: PrefsManager
    internal var bookShelfContainer: View? = null
    private var multiPanel = false

    override fun isMultiPanel(): Boolean {
        return multiPanel
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(SI_MULTI_PANEL, isMultiPanel)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val permissionGrantingWorked = PermissionHelper.permissionGrantingWorked(requestCode,
                    PERMISSION_RESULT_READ_EXT_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,
                    permissions, grantResults)
            Timber.i("permissionGrantingWorked=%b", permissionGrantingWorked)
            if (!permissionGrantingWorked) {
                PermissionHelper.handleExtStorageRescan(this, PERMISSION_RESULT_READ_EXT_STORAGE)
                Timber.e("could not get permission")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book)
        bookShelfContainer = findViewById(CONTAINER_SHELF)
        App.getComponent().inject(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val anyFolderSet = prefs.collectionFolders.size + prefs.singleBookFolders.size > 0
            val canReadStorage = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            if (anyFolderSet && !canReadStorage) {
                PermissionHelper.handleExtStorageRescan(this, PERMISSION_RESULT_READ_EXT_STORAGE)
            }
        }

        setSupportActionBar(findViewById(R.id.toolbar) as Toolbar)

        multiPanel = bookShelfContainer != null
        val multiPaneChanged = savedInstanceState != null && savedInstanceState.getBoolean(SI_MULTI_PANEL) != multiPanel
        Timber.i("multiPane=%b, multiPaneChanged=%b", multiPanel, multiPaneChanged)

        // first retrieve the fragments
        val fm = supportFragmentManager

        if (savedInstanceState == null) {
            val bookShelfFragment = BookShelfFragment()
            if (multiPanel) {
                fm.beginTransaction().replace(CONTAINER_SHELF, bookShelfFragment, FM_BOOK_SHELF).replace(CONTAINER_PLAY, BookPlayFragment.newInstance(prefs.currentBookId.value), FM_BOOK_PLAY).commit()
            } else {
                fm.beginTransaction().replace(CONTAINER_PLAY, bookShelfFragment, FM_BOOK_SHELF).commit()
            }
        } else if (multiPaneChanged) {
            // we need to pop the whole back-stack. Else we can't change the container id
            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

            // restore book shelf or create new one
            var bookShelfFragment: BookShelfFragment? = fm.findFragmentByTag(FM_BOOK_SHELF) as BookShelfFragment
            if (bookShelfFragment == null) {
                bookShelfFragment = BookShelfFragment()
                Timber.v("new fragment=%s", bookShelfFragment)
            } else {
                fm.beginTransaction().remove(bookShelfFragment).commit()
                fm.executePendingTransactions()
                Timber.v("restored fragment=%s", bookShelfFragment)
            }

            // restore book play fragment or create new one
            var bookPlayFragment: BookPlayFragment? = fm.findFragmentByTag(FM_BOOK_PLAY) as BookPlayFragment
            if (bookPlayFragment == null) {
                bookPlayFragment = BookPlayFragment.newInstance(prefs.currentBookId.value)
                Timber.v("new fragment=%s", bookPlayFragment)
            } else {
                fm.beginTransaction().remove(bookPlayFragment).commit()
                fm.executePendingTransactions()
                Timber.v("restored fragment=%s", bookPlayFragment)
                if (bookPlayFragment.bookId != prefs.currentBookId.value) {
                    bookPlayFragment = BookPlayFragment.newInstance(prefs.currentBookId.value)
                    Timber.v("id did not match. Created new fragment=%s", bookPlayFragment)
                }
            }

            if (multiPanel) {
                fm.beginTransaction().replace(CONTAINER_SHELF, bookShelfFragment, FM_BOOK_SHELF).replace(CONTAINER_PLAY, bookPlayFragment, FM_BOOK_PLAY).commit()
            } else {
                fm.beginTransaction().replace(CONTAINER_PLAY, bookShelfFragment, FM_BOOK_SHELF).commit()
                fm.beginTransaction().replace(CONTAINER_PLAY, bookPlayFragment, FM_BOOK_PLAY).addToBackStack(null).commit()
            }
        }

        if (savedInstanceState == null) {
            if (intent.hasExtra(NI_MALFORMED_FILE)) {
                val malformedFile = intent.getSerializableExtra(NI_MALFORMED_FILE) as File
                MaterialDialog.Builder(this).title(R.string.mal_file_title).content(getString(R.string.mal_file_message) + "\n\n" + malformedFile).show()
            }
            if (intent.hasExtra(NI_GO_TO_BOOK)) {
                val bookId = intent.getLongExtra(NI_GO_TO_BOOK, -1)
                onBookSelected(bookId, HashMap<View, String>(0))
            }
        }
    }

    override fun onBookSelected(bookId: Long, sharedViews: Map<View, String>) {
        Timber.i("onBookSelected with bookId=%d", bookId)

        val ft = supportFragmentManager.beginTransaction()
        val bookPlayFragment = BookPlayFragment.newInstance(bookId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !multiPanel) {
            val move = TransitionInflater.from(this).inflateTransition(android.R.transition.move)
            bookPlayFragment.sharedElementEnterTransition = move
            for (entry in sharedViews.entries) {
                Timber.v("Added sharedElement=%s", entry)
                ft.addSharedElement(entry.key, entry.value)
            }
        }

        // only replace if there is not already a fragment with that id
        val containingFragment = supportFragmentManager.findFragmentById(CONTAINER_PLAY)
        if (containingFragment == null || containingFragment !is BookPlayFragment || (containingFragment.bookId != bookId)) {
            ft.replace(CONTAINER_PLAY, bookPlayFragment, FM_BOOK_PLAY).addToBackStack(null).commit()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuItemIds = ArrayList<Int>(menu.size())
        for (i in 0..menu.size() - 1) {
            val item = menu.getItem(i)
            if (menuItemIds.contains(item.itemId)) {
                menu.removeItem(item.itemId)
            } else {
                menuItemIds.add(item.itemId)
            }
        }

        return true
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
         * Used for [.onSaveInstanceState] to get the previous panel mode.
         */
        private val SI_MULTI_PANEL = "siMultiPanel"
        private val PERMISSION_RESULT_READ_EXT_STORAGE = 17

        /**
         * Returns an intent to start the activity with to inform the user that a certain file may be
         * defect

         * @param c             The context
         * *
         * @param malformedFile The defect file
         * *
         * @return The intent to start the activity with.
         */
        fun malformedFileIntent(c: Context, malformedFile: File): Intent {
            val intent = Intent(c, BookActivity::class.java)
            intent.putExtra(NI_MALFORMED_FILE, malformedFile)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            return intent
        }

        /**
         * Returns an intent that lets you go directly to the playback screen for a certain book

         * @param c      The context
         * *
         * @param bookId The book id to target
         * *
         * @return The intent
         */
        fun goToBookIntent(c: Context, bookId: Long): Intent {
            val intent = Intent(c, BookActivity::class.java)
            intent.putExtra(NI_GO_TO_BOOK, bookId)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            return intent
        }
    }
}
