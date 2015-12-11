/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.annotation.IdRes
import android.support.v4.app.ActivityCompat
import android.support.v7.widget.Toolbar
import android.transition.TransitionInflater
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.R
import de.ph1b.audiobook.fragment.BookPlayFragment
import de.ph1b.audiobook.fragment.BookShelfFragment
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.utils.PermissionHelper
import timber.log.Timber
import java.io.File
import java.util.*
import javax.inject.Inject

/**
 * Activity that coordinates the book shelf and play screens.

 * @author Paul Woitaschek
 */
class BookActivity : BaseActivity(), BookShelfFragment.BookSelectionCallback {

    private val TAG = BookActivity::class.java.simpleName
    private val FM_BOOK_SHELF = TAG + BookShelfFragment.TAG
    private val FM_BOOK_PLAY = TAG + BookPlayFragment.TAG
    @IdRes private val FRAME_CONTAINER = R.id.play_container

    @Inject internal lateinit var prefs: PrefsManager

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
        App.component().inject(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val anyFolderSet = prefs.collectionFolders.size + prefs.singleBookFolders.size > 0
            val canReadStorage = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            if (anyFolderSet && !canReadStorage) {
                PermissionHelper.handleExtStorageRescan(this, PERMISSION_RESULT_READ_EXT_STORAGE)
            }
        }
        setSupportActionBar(findViewById(R.id.toolbar) as Toolbar)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(FRAME_CONTAINER, BookShelfFragment(), FM_BOOK_SHELF)
                    .commit()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
            val move = TransitionInflater.from(this).inflateTransition(android.R.transition.move)
            bookPlayFragment.sharedElementEnterTransition = move
            for (entry in sharedViews.entries) {
                Timber.v("Added sharedElement=%s", entry)
                ft.addSharedElement(entry.key, entry.value)
            }
        }

        ft.replace(FRAME_CONTAINER, bookPlayFragment, FM_BOOK_PLAY)
                .addToBackStack(null)
                .commit()
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
