package de.ph1b.audiobook.features.folder_overview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.*
import com.afollestad.materialdialogs.MaterialDialog
import com.getbase.floatingactionbutton.FloatingActionButton
import com.getbase.floatingactionbutton.FloatingActionsMenu
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.folder_chooser.FolderChooserActivity
import de.ph1b.audiobook.misc.find
import de.ph1b.audiobook.misc.setupActionbar
import de.ph1b.audiobook.mvp.MvpBaseController
import de.ph1b.audiobook.uitools.setVisibleWeak
import de.ph1b.audiobook.uitools.visible

/**
 * Activity that lets the user add, edit or remove the set audio book folders.
 *
 * @author Paul Woitaschek
 */
class FolderOverviewController : MvpBaseController<FolderOverviewController, FolderOverviewPresenter>() {

  override val presenter: FolderOverviewPresenter = FolderOverviewPresenter()

  init {
    setHasOptionsMenu(true)
  }

  private lateinit var overlay: View
  private lateinit var fam: FloatingActionsMenu
  private lateinit var recycler: RecyclerView
  private lateinit var addAsSingle: FloatingActionButton
  private lateinit var addAsLibrary: FloatingActionButton
  private lateinit var toolbar: Toolbar

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
    val view = inflater.inflate(R.layout.activity_folder_overview, container, false)
    buttonRepresentingTheFam = view.find(R.id.fab_expand_menu_button)
    overlay = view.find(R.id.overlay)
    fam = view.find(R.id.fam)
    recycler = view.find(R.id.recycler)
    addAsSingle = view.find(R.id.addAsSingle)
    addAsLibrary = view.find(R.id.addAsLibrary)
    toolbar = view.find(R.id.toolbar)

    addAsSingle.setOnClickListener {
      startFolderChooserActivity(FolderChooserActivity.OperationMode.SINGLE_BOOK)
    }
    addAsLibrary.setOnClickListener {
      startFolderChooserActivity(FolderChooserActivity.OperationMode.COLLECTION_BOOK)
    }

    overlay.setVisibleWeak()

    overlay.setOnClickListener {
      fam.collapse()
    }

    // preparing list
    val layoutManager = LinearLayoutManager(activity)
    recycler.layoutManager = layoutManager
    recycler.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))

    adapter = FolderOverviewAdapter { toDelete ->
      MaterialDialog.Builder(activity)
        .title(R.string.delete_folder)
        .content("${getString(R.string.delete_folder_content)}\n$toDelete")
        .positiveText(R.string.remove)
        .negativeText(R.string.dialog_cancel)
        .onPositive { materialDialog, dialogAction ->
          presenter.removeFolder(toDelete)
        }
        .show()
    }
    recycler.adapter = adapter

    fam.setOnFloatingActionsMenuUpdateListener(famMenuListener)

    addAsSingle.title = "${getString(R.string.folder_add_single_book)}\n${getString(R.string.for_example)} Harry Potter 4"
    addAsLibrary.title = "${getString(R.string.folder_add_collection)}\n${getString(R.string.for_example)} AudioBooks"

    return view
  }

  override fun onAttach(view: View) {
    super.onAttach(view)

    setupActionbar(toolbar = toolbar,
      title = activity.getString(R.string.audiobook_folders_title),
      upIndicator = R.drawable.close)
  }

  override fun provideView() = this

  private val BACKGROUND_VISIBILITY = "overlayVisibility"

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
        overlay.visible = true
        anim.start()
      } else overlay.visible = true
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
            overlay.setVisibleWeak()
          }
        })

        // start the animation
        anim.start()
      } else overlay.setVisibleWeak()
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
        router.popCurrentController()
        return true
      }
      else -> return super.onOptionsItemSelected(item)
    }
  }

  override fun onRestoreViewState(view: View, savedViewState: Bundle) {
    // restoring overlay
    overlay.visibility = savedViewState.getInt(BACKGROUND_VISIBILITY)
  }

  private fun startFolderChooserActivity(operationMode: FolderChooserActivity.OperationMode) {
    val intent = FolderChooserActivity.newInstanceIntent(activity, operationMode)
    // we don't want our listener be informed.
    fam.setOnFloatingActionsMenuUpdateListener(null)
    fam.collapseImmediately()
    fam.setOnFloatingActionsMenuUpdateListener(famMenuListener)

    overlay.visible = false
    startActivity(intent)
  }

  override fun handleBack(): Boolean {
    if (fam.isExpanded) {
      fam.collapse()
      return true
    } else return false
  }

  /** Updates the adapter with new contents. **/
  fun newData(models: Collection<FolderModel>) {
    adapter.newItems(models)
  }

  override fun onSaveViewState(view: View, outState: Bundle) {
    super.onSaveViewState(view, outState)
    outState.putInt(BACKGROUND_VISIBILITY, overlay.visibility)
  }
}