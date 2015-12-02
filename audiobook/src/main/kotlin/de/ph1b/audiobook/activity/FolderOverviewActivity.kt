package de.ph1b.audiobook.activity


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Intent
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.getbase.floatingactionbutton.FloatingActionButton
import com.getbase.floatingactionbutton.FloatingActionsMenu
import de.ph1b.audiobook.R
import de.ph1b.audiobook.adapter.FolderOverviewAdapter
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.model.BookAdder
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.uitools.DividerItemDecoration
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * Activity that lets the user add, edit or remove the set audiobook folders.

 * @author Paul Woitaschek
 */
class FolderOverviewActivity : BaseActivity() {

    private val BACKGROUND_OVERLAY_VISIBLE = "backgroundOverlayVisibility"
    private val PICKER_REQUEST_CODE = 42

    private val bookCollections = ArrayList<String>(10)
    private val singleBooks = ArrayList<String>(10)

    private lateinit var fam: FloatingActionsMenu
    private lateinit var singleBookButton: FloatingActionButton
    private lateinit var buttonRepresentingTheFam: View
    private lateinit var backgroundOverlay: View
    private lateinit var recyclerView: RecyclerView

    @Inject internal lateinit var prefs: PrefsManager
    @Inject internal lateinit var bookAdder: BookAdder

    private lateinit var adapter: FolderOverviewAdapter

    private val famMenuListener = object : FloatingActionsMenu.OnFloatingActionsMenuUpdateListener {

        private val famCenter = Point()

        override fun onMenuExpanded() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getFamCenter(famCenter)

                // get the final radius for the clipping circle
                val finalRadius = Math.max(backgroundOverlay.width, backgroundOverlay.height)

                // create the animator for this view (the start radius is zero)
                val anim = ViewAnimationUtils.createCircularReveal(backgroundOverlay,
                        famCenter.x, famCenter.y, 0f, finalRadius.toFloat())

                // make the view visible and start the animation
                backgroundOverlay.visibility = View.VISIBLE
                anim.start()
            } else {
                backgroundOverlay.visibility = View.VISIBLE
            }
        }

        override fun onMenuCollapsed() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // get the center for the clipping circle
                getFamCenter(famCenter)

                // get the initial radius for the clipping circle
                val initialRadius = Math.max(backgroundOverlay.height, backgroundOverlay.width)

                // create the animation (the final radius is zero)
                val anim = ViewAnimationUtils.createCircularReveal(backgroundOverlay,
                        famCenter.x, famCenter.y, initialRadius.toFloat(), 0f)

                // make the view invisible when the animation is done
                anim.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        backgroundOverlay.visibility = View.INVISIBLE
                    }
                })

                // start the animation
                anim.start()
            } else {
                backgroundOverlay.visibility = View.INVISIBLE
            }
        }
    }

    /**
     * Calculates the point representing the center of the floating action menus button. Note, that
     * the fam is only a container, so we have to calculate the point relatively.
     */
    private fun getFamCenter(point: Point) {
        val x = fam.left + ((buttonRepresentingTheFam.left + buttonRepresentingTheFam.right) / 2)
        val y = fam.top + ((buttonRepresentingTheFam.top + buttonRepresentingTheFam.bottom) / 2)
        point.set(x, y)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.component().inject(this)

        setContentView(R.layout.activity_folder_overview)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        fam = findViewById(R.id.fam) as FloatingActionsMenu
        recyclerView = findViewById(R.id.recycler) as RecyclerView
        val libraryBookButton = findViewById(R.id.add_library) as FloatingActionButton
        backgroundOverlay = findViewById(R.id.overlay)
        buttonRepresentingTheFam = findViewById(R.id.fab_expand_menu_button)
        singleBookButton = findViewById(R.id.add_single) as FloatingActionButton

        singleBookButton.setOnClickListener({
            startFolderChooserActivity(FolderChooserActivity.OperationMode.SINGLE_BOOK)
        })
        libraryBookButton.setOnClickListener({
            startFolderChooserActivity(FolderChooserActivity.OperationMode.COLLECTION_BOOK)
        })

        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        assert(actionBar != null)
        actionBar!!.setDisplayHomeAsUpEnabled(true)
        actionBar.title = getString(R.string.audiobook_folders_title)


        //init views
        if (savedInstanceState != null) {
            // restoring overlay
            if (savedInstanceState.getBoolean(BACKGROUND_OVERLAY_VISIBLE)) {
                backgroundOverlay.visibility = View.VISIBLE
            } else {
                backgroundOverlay.visibility = View.INVISIBLE
            }
        }

        // preparing list
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        recyclerView.layoutManager = layoutManager
        recyclerView.addItemDecoration(DividerItemDecoration(this))

        val moreClickedListener = object : FolderOverviewAdapter.OnFolderMoreClickedListener {
            override fun onFolderMoreClicked(position: Int) {
                MaterialDialog.Builder(this@FolderOverviewActivity)
                        .title(R.string.delete_folder)
                        .content("${getString(R.string.delete_folder_content)}\n${adapter.getItem(position)}")
                        .positiveText(R.string.remove)
                        .negativeText(R.string.dialog_cancel)
                        .onPositive { materialDialog, dialogAction ->
                            adapter.removeItem(position)
                            prefs.collectionFolders = bookCollections
                            prefs.singleBookFolders = singleBooks
                            bookAdder.scanForFiles(true)
                        }
                        .show()
            }
        }
        adapter = FolderOverviewAdapter(this, bookCollections, singleBooks, moreClickedListener)
        recyclerView.adapter = adapter

        fam.setOnFloatingActionsMenuUpdateListener(famMenuListener)

        singleBookButton.title = "${getString(R.string.folder_add_single_book)}\n${getString(R.string.for_example)} Harry Potter 4"
        libraryBookButton.title = "${getString(R.string.folder_add_collection)}\n${getString(R.string.for_example)} AudioBooks"
    }

    private fun startFolderChooserActivity(operationMode: FolderChooserActivity.OperationMode) {
        val intent = FolderChooserActivity.newInstanceIntent(this, operationMode)
        startActivityForResult(intent, PICKER_REQUEST_CODE)
    }

    /**
     * @param newFile the new folder file
     * *
     * @return true if the new folder is not added yet and is no sub- or parent folder of an existing
     * * book folder
     */
    private fun canAddNewFolder(newFile: String): Boolean {
        val folders = ArrayList<String>(bookCollections.size + singleBooks.size)
        folders.addAll(bookCollections)
        folders.addAll(singleBooks)

        var filesAreSubsets = true
        val firstAddedFolder = folders.isEmpty()
        var sameFolder = false
        for (s in folders) {
            if (s == newFile) {
                sameFolder = true
            }
            val oldParts = s.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val newParts = newFile.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (i in 0..Math.min(oldParts.size, newParts.size) - 1) {
                if (oldParts[i] != newParts[i]) {
                    filesAreSubsets = false
                }
            }
            if (!sameFolder && filesAreSubsets) {
                Toast.makeText(this, "${getString(R.string.adding_failed_subfolder)}\n$s\n$newFile", Toast.LENGTH_LONG).show()
            }
            if (filesAreSubsets) {
                break
            }
        }

        return firstAddedFolder || (!sameFolder && !filesAreSubsets)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // we don't want our listener be informed.
        fam.setOnFloatingActionsMenuUpdateListener(null)
        fam.collapseImmediately()
        fam.setOnFloatingActionsMenuUpdateListener(famMenuListener)

        backgroundOverlay.visibility = View.INVISIBLE

        if (resultCode == Activity.RESULT_OK && requestCode == PICKER_REQUEST_CODE && data != null) {
            val mode = FolderChooserActivity.OperationMode.valueOf(data.getStringExtra(FolderChooserActivity.RESULT_OPERATION_MODE))
            when (mode) {
                FolderChooserActivity.OperationMode.COLLECTION_BOOK -> {
                    val chosenCollection = data.getStringExtra(
                            FolderChooserActivity.RESULT_CHOSEN_FILE)
                    if (canAddNewFolder(chosenCollection)) {
                        bookCollections.add(chosenCollection)
                        prefs.collectionFolders = bookCollections
                    }
                    Timber.v("chosenCollection=%s", chosenCollection)
                }
                FolderChooserActivity.OperationMode.SINGLE_BOOK -> {
                    val chosenSingleBook = data.getStringExtra(
                            FolderChooserActivity.RESULT_CHOSEN_FILE)
                    if (canAddNewFolder(chosenSingleBook)) {
                        singleBooks.add(chosenSingleBook)
                        prefs.singleBookFolders = singleBooks
                    }
                    Timber.v("chosenSingleBook=%s", chosenSingleBook)
                }
                else -> {
                }
            }
            bookAdder.scanForFiles(true)
        }
    }

    override fun onBackPressed() {
        if (fam.isExpanded) {
            fam.collapse()
        } else {
            super.onBackPressed()
        }
    }


    override fun onResume() {
        super.onResume()

        bookCollections.clear()
        bookCollections.addAll(prefs.collectionFolders)
        singleBooks.clear()
        singleBooks.addAll(prefs.singleBookFolders)
        adapter.notifyDataSetChanged()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(BACKGROUND_OVERLAY_VISIBLE, backgroundOverlay.visibility == View.VISIBLE)

        super.onSaveInstanceState(outState)
    }
}