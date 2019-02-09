package de.ph1b.audiobook.features.folderOverview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Point
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.getbase.floatingactionbutton.FloatingActionsMenu
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.folderChooser.FolderChooserActivity
import de.ph1b.audiobook.misc.tint
import de.ph1b.audiobook.mvp.MvpController
import kotlinx.android.synthetic.main.folder_overview.*

private const val SI_BACKGROUND_VISIBILITY = "si#overlayVisibility"

/**
 * Controller that lets the user add, edit or remove the set audio book folders.
 */
class FolderOverviewController :
  MvpController<FolderOverviewController, FolderOverviewPresenter>() {

  override fun createPresenter(): FolderOverviewPresenter = FolderOverviewPresenter()

  override val layoutRes = R.layout.folder_overview

  override fun onViewCreated() {
    buttonRepresentingTheFam = containerView!!.findViewById<View>(R.id.fab_expand_menu_button)

    addAsSingle.setOnClickListener {
      startFolderChooserActivity(FolderChooserActivity.OperationMode.SINGLE_BOOK)
    }
    addAsLibrary.setOnClickListener {
      startFolderChooserActivity(FolderChooserActivity.OperationMode.COLLECTION_BOOK)
    }

    overlay.isInvisible = true

    overlay.setOnClickListener {
      fam.collapse()
    }

    // preparing list
    val layoutManager = LinearLayoutManager(activity)
    recycler.layoutManager = layoutManager
    recycler.addItemDecoration(
      DividerItemDecoration(activity, DividerItemDecoration.VERTICAL)
    )

    adapter = FolderOverviewAdapter { toDelete ->
      val toDeleteName = toDelete.folder
      MaterialDialog(activity).show {
        title(R.string.delete_folder)
        message(text = "${getString(R.string.delete_folder_content)}\n$toDeleteName")
        positiveButton(R.string.remove) { presenter.removeFolder(toDelete) }
        negativeButton(R.string.dialog_cancel)
      }
    }
    recycler.adapter = adapter

    fam.setOnFloatingActionsMenuUpdateListener(famMenuListener)

    addAsSingle.title =
      "${getString(R.string.folder_add_single_book)}\n${getString(R.string.for_example)} Harry Potter 4"
    addAsLibrary.title = "${getString(R.string.folder_add_collection)}\n${getString(R.string.for_example)} AudioBooks"

    setupToolbar()
  }

  private fun setupToolbar() {
    toolbar.apply {
      setNavigationOnClickListener { activity.onBackPressed() }
      tint()
    }
  }

  override fun provideView() = this

  private lateinit var buttonRepresentingTheFam: View

  private lateinit var adapter: FolderOverviewAdapter

  private val famMenuListener = object : FloatingActionsMenu.OnFloatingActionsMenuUpdateListener {

    private val famCenter = Point()

    override fun onMenuExpanded() {
      getFamCenter(famCenter)

      // get the final radius for the clipping circle
      val finalRadius = Math.max(overlay.width, overlay.height)

      // create the animator for this view (the start radius is zero)
      val anim = ViewAnimationUtils.createCircularReveal(
        overlay,
        famCenter.x, famCenter.y, 0f, finalRadius.toFloat()
      )

      // make the view visible and start the animation
      overlay.isVisible = true
      anim.start()
    }

    override fun onMenuCollapsed() {
      // get the center for the clipping circle
      getFamCenter(famCenter)

      // get the initial radius for the clipping circle
      val initialRadius = Math.max(overlay.height, overlay.width)

      // create the animation (the final radius is zero)
      val anim = ViewAnimationUtils.createCircularReveal(
        overlay,
        famCenter.x, famCenter.y, initialRadius.toFloat(), 0f
      )

      // make the view invisible when the animation is done
      anim.addListener(
        object : AnimatorListenerAdapter() {
          override fun onAnimationEnd(animation: Animator) {
            super.onAnimationEnd(animation)
            overlay.isInvisible = true
          }
        }
      )

      // start the animation
      anim.start()
    }
  }

  /**
   * Calculates the point representing the center of the floating action menus button. Note, that
   * the fam is only a container, so we have to calculate the point relatively.
   */
  private fun getFamCenter(point: Point) {
    val x =
      fam.left + ((buttonRepresentingTheFam.left + buttonRepresentingTheFam.right) / 2)
    val y = fam.top + ((buttonRepresentingTheFam.top + buttonRepresentingTheFam.bottom) / 2)
    point.set(x, y)
  }

  override fun onRestoreViewState(view: View, savedViewState: Bundle) {
    // restoring overlay
    overlay.visibility = savedViewState.getInt(SI_BACKGROUND_VISIBILITY)
  }

  private fun startFolderChooserActivity(operationMode: FolderChooserActivity.OperationMode) {
    val intent = FolderChooserActivity.newInstanceIntent(activity, operationMode)
    // we don't want our listener be informed.
    fam.setOnFloatingActionsMenuUpdateListener(null)
    fam.collapseImmediately()
    fam.setOnFloatingActionsMenuUpdateListener(famMenuListener)

    overlay.isVisible = false
    startActivity(intent)
  }

  override fun handleBack(): Boolean = if (fam.isExpanded) {
    fam.collapse()
    true
  } else false

  /** Updates the adapter with new contents. **/
  fun newData(models: Collection<FolderModel>) {
    adapter.newItems(models)
  }

  override fun onSaveViewState(view: View, outState: Bundle) {
    super.onSaveViewState(view, outState)
    outState.putInt(SI_BACKGROUND_VISIBILITY, overlay.visibility)
  }
}
