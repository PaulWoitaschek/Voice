package de.ph1b.audiobook.features.folder_overview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.MenuItem
import android.view.View
import android.view.ViewAnimationUtils
import com.afollestad.materialdialogs.MaterialDialog
import com.getbase.floatingactionbutton.FloatingActionsMenu
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.folder_chooser.FolderChooserActivity
import de.ph1b.audiobook.misc.setupActionbar
import de.ph1b.audiobook.mvp.RxBaseActivity
import de.ph1b.audiobook.uitools.DividerItemDecoration
import kotlinx.android.synthetic.main.activity_folder_overview.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.*

/**
 * Activity that lets the user add, edit or remove the set audiobook folders.

 * @author Paul Woitaschek
 */
class FolderOverviewActivity : RxBaseActivity<FolderOverviewActivity, FolderOverviewPresenter>() {

    override fun newPresenter() = FolderOverviewPresenter()

    override fun provideView() = this

    private val BACKGROUND_OVERLAY_VISIBLE = "overlayVisibility"

    private val bookCollections = ArrayList<String>(10)
    private val singleBooks = ArrayList<String>(10)

    private lateinit var buttonRepresentingTheFam: View

    private lateinit var adapter: FolderOverviewAdapter

    private val famMenuListener = object : FloatingActionsMenu.OnFloatingActionsMenuUpdateListener {

        private val famCenter = Point()

        override fun onMenuExpanded() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getFamCenter(famCenter)

                // get the final radius for the clipping circle
                val finalRadius = Math.max(overlay.width, overlay.height)

                // create the animator for this view (the start radius is zero)
                val anim = ViewAnimationUtils.createCircularReveal(overlay,
                        famCenter.x, famCenter.y, 0f, finalRadius.toFloat())

                // make the view visible and start the animation
                overlay.visibility = View.VISIBLE
                anim.start()
            } else overlay.visibility = View.VISIBLE
        }

        override fun onMenuCollapsed() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // get the center for the clipping circle
                getFamCenter(famCenter)

                // get the initial radius for the clipping circle
                val initialRadius = Math.max(overlay.height, overlay.width)

                // create the animation (the final radius is zero)
                val anim = ViewAnimationUtils.createCircularReveal(overlay,
                        famCenter.x, famCenter.y, initialRadius.toFloat(), 0f)

                // make the view invisible when the animation is done
                anim.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        overlay.visibility = View.INVISIBLE
                    }
                })

                // start the animation
                anim.start()
            } else overlay.visibility = View.INVISIBLE
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
        buttonRepresentingTheFam = findViewById(R.id.fab_expand_menu_button)!!

        addAsSingle.setOnClickListener {
            startFolderChooserActivity(FolderChooserActivity.OperationMode.SINGLE_BOOK)
        }
        addAsLibrary.setOnClickListener {
            startFolderChooserActivity(FolderChooserActivity.OperationMode.COLLECTION_BOOK)
        }

        setupActionbar(toolbar = toolbar, homeAsUpEnabled = true, titleRes = R.string.audiobook_folders_title)

        //init views
        if (savedInstanceState != null) {
            // restoring overlay
            if (savedInstanceState.getBoolean(BACKGROUND_OVERLAY_VISIBLE)) {
                overlay.visibility = View.VISIBLE
            } else overlay.visibility = View.INVISIBLE
        }

        // preparing list
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        recycler.layoutManager = layoutManager
        recycler.addItemDecoration(DividerItemDecoration(this))

        adapter = FolderOverviewAdapter(bookCollections, singleBooks) { toDelete ->
            MaterialDialog.Builder(this@FolderOverviewActivity)
                    .title(R.string.delete_folder)
                    .content("${getString(R.string.delete_folder_content)}\n$toDelete")
                    .positiveText(R.string.remove)
                    .negativeText(R.string.dialog_cancel)
                    .onPositive { materialDialog, dialogAction ->
                        presenter().removeFolder(toDelete)
                    }
                    .show()
        }
        recycler.adapter = adapter

        fam.setOnFloatingActionsMenuUpdateListener(famMenuListener)

        addAsSingle.title = "${getString(R.string.folder_add_single_book)}\n${getString(R.string.for_example)} Harry Potter 4"
        addAsLibrary.title = "${getString(R.string.folder_add_collection)}\n${getString(R.string.for_example)} AudioBooks"
    }

    private fun startFolderChooserActivity(operationMode: FolderChooserActivity.OperationMode) {
        val intent = FolderChooserActivity.newInstanceIntent(this, operationMode)
        // we don't want our listener be informed.
        fam.setOnFloatingActionsMenuUpdateListener(null)
        fam.collapseImmediately()
        fam.setOnFloatingActionsMenuUpdateListener(famMenuListener)

        overlay.visibility = View.INVISIBLE
        startActivity(intent)
    }

    override fun onBackPressed() {
        if (fam.isExpanded) {
            fam.collapse()
        } else super.onBackPressed()
    }

    /**
     * Updates the adapter with new contents.
     *
     * @param bookCollections The folders added as book collections.
     * @param singleBooks The folders added as single books.
     */
    fun updateAdapterData(bookCollections: Collection<String>, singleBooks: Collection<String>) {
        this.bookCollections.clear()
        this.bookCollections.addAll(bookCollections)
        this.singleBooks.clear()
        this.singleBooks.addAll(singleBooks)
        adapter.notifyDataSetChanged()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(BACKGROUND_OVERLAY_VISIBLE, overlay.visibility == View.VISIBLE)

        super.onSaveInstanceState(outState)
    }
}