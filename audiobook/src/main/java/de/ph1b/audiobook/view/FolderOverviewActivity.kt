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

package de.ph1b.audiobook.view


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
import com.afollestad.materialdialogs.MaterialDialog
import com.getbase.floatingactionbutton.FloatingActionButton
import com.getbase.floatingactionbutton.FloatingActionsMenu
import de.ph1b.audiobook.R
import de.ph1b.audiobook.adapter.FolderOverviewAdapter
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.mvp.RxBaseActivity
import de.ph1b.audiobook.presenter.FolderOverviewPresenter
import de.ph1b.audiobook.uitools.DividerItemDecoration
import java.util.*

/**
 * Activity that lets the user add, edit or remove the set audiobook folders.

 * @author Paul Woitaschek
 */
class FolderOverviewActivity : RxBaseActivity<FolderOverviewActivity, FolderOverviewPresenter> () {

    override fun newPresenter() = FolderOverviewPresenter()

    override fun provideView() = this

    init {
        App.component().inject(this)
    }

    private val BACKGROUND_OVERLAY_VISIBLE = "backgroundOverlayVisibility"

    private val bookCollections = ArrayList<String>(10)
    private val singleBooks = ArrayList<String>(10)

    private lateinit var fam: FloatingActionsMenu
    private lateinit var singleBookButton: FloatingActionButton
    private lateinit var buttonRepresentingTheFam: View
    private lateinit var backgroundOverlay: View
    private lateinit var recyclerView: RecyclerView

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

        setContentView(R.layout.activity_folder_overview)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        fam = findViewById(R.id.fam) as FloatingActionsMenu
        recyclerView = findViewById(R.id.recycler) as RecyclerView
        val libraryBookButton = findViewById(R.id.add_library) as FloatingActionButton
        backgroundOverlay = findViewById(R.id.overlay)!!
        buttonRepresentingTheFam = findViewById(R.id.fab_expand_menu_button)!!
        singleBookButton = findViewById(R.id.add_single) as FloatingActionButton

        singleBookButton.setOnClickListener {
            startFolderChooserActivity(FolderChooserActivity.OperationMode.SINGLE_BOOK)
        }
        libraryBookButton.setOnClickListener {
            startFolderChooserActivity(FolderChooserActivity.OperationMode.COLLECTION_BOOK)
        }

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
                            val itemToDelete = adapter.getItem(position)
                            presenter()!!.removeFolder(itemToDelete)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // we don't want our listener be informed.
        fam.setOnFloatingActionsMenuUpdateListener(null)
        fam.collapseImmediately()
        fam.setOnFloatingActionsMenuUpdateListener(famMenuListener)

        backgroundOverlay.visibility = View.INVISIBLE
    }

    override fun onBackPressed() {
        if (fam.isExpanded) {
            fam.collapse()
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Updates the adapter with new contents.
     *
     * @param bookCollections The folders added as book collections.
     * @param singleBooks The folders added as single books.
     */
    fun updateAdapterData(bookCollections: List<String>, singleBooks: List<String>) {
        this.bookCollections.clear()
        this.bookCollections.addAll(bookCollections)
        this.singleBooks.clear()
        this.singleBooks.addAll(singleBooks)
        adapter.notifyDataSetChanged()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(BACKGROUND_OVERLAY_VISIBLE, backgroundOverlay.visibility == View.VISIBLE)

        super.onSaveInstanceState(outState)
    }

    companion object {
        val PICKER_REQUEST_CODE = 42
    }
}