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
 * along with Material Audiobook Player. If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.features

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
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
import de.ph1b.audiobook.misc.startActivity
import de.ph1b.audiobook.persistence.PrefsManager
import e
import i
import kotlinx.android.synthetic.main.activity_book.*
import kotlinx.android.synthetic.main.toolbar.*
import v
import java.io.File
import java.util.*
import javax.inject.Inject

/**
 * Activity that coordinates the book shelf and play screens.

 * @author Paul Woitaschek
 */
class BookActivity : BaseActivity(), BookShelfFragment.Callback {

    private val TAG = BookActivity::class.java.simpleName
    private val FM_BOOK_SHELF = TAG + BookShelfFragment.TAG
    private val FM_BOOK_PLAY = TAG + BookPlayFragment.TAG

    @Inject internal lateinit var prefs: PrefsManager

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val permissionGrantingWorked = PermissionHelper.permissionGrantingWorked(requestCode,
                    PERMISSION_RESULT_READ_EXT_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,
                    permissions, grantResults)
            i { "permissionGrantingWorked=$permissionGrantingWorked" }
            if (!permissionGrantingWorked) {
                PermissionHelper.handleExtStorageRescan(this, PERMISSION_RESULT_READ_EXT_STORAGE)
                e { "could not get permission" }
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

        setSupportActionBar(toolbar!!)

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
        val initializer = ImagePickerActivity.Args(book.id)
        val args = ImagePickerActivity.arguments(initializer)
        startActivity<ImagePickerActivity>(args)
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
