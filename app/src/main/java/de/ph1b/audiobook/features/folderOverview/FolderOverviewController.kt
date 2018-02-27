package de.ph1b.audiobook.features.folderOverview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Point
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.view.ViewAnimationUtils
import com.afollestad.materialdialogs.MaterialDialog
import com.getbase.floatingactionbutton.FloatingActionsMenu
import de.ph1b.audiobook.R
import de.ph1b.audiobook.databinding.FolderOverviewBinding
import de.ph1b.audiobook.features.folderChooser.FolderChooserActivity
import de.ph1b.audiobook.mvp.MvpController
import de.ph1b.audiobook.uitools.setVisibleWeak
import de.ph1b.audiobook.uitools.visible

private const val SI_BACKGROUND_VISIBILITY = "si#overlayVisibility"

/**
 * Controller that lets the user add, edit or remove the set audio book folders.
 */
class FolderOverviewController :
  MvpController<FolderOverviewController, FolderOverviewPresenter, FolderOverviewBinding>() {

  override fun createPresenter(): FolderOverviewPresenter = FolderOverviewPresenter()

  override val layoutRes = R.layout.folder_overview

  override fun onBindingCreated(binding: FolderOverviewBinding) {
    buttonRepresentingTheFam = binding.root.findViewById<View>(R.id.fab_expand_menu_button)

    binding.addAsSingle.setOnClickListener {
      startFolderChooserActivity(FolderChooserActivity.OperationMode.SINGLE_BOOK)
    }
    binding.addAsLibrary.setOnClickListener {
      startFolderChooserActivity(FolderChooserActivity.OperationMode.COLLECTION_BOOK)
    }

    binding.overlay.setVisibleWeak()

    binding.overlay.setOnClickListener {
      binding.fam.collapse()
    }

    // preparing list
    val layoutManager = LinearLayoutManager(activity)
    binding.recycler.layoutManager = layoutManager
    binding.recycler.addItemDecoration(
      DividerItemDecoration(
        activity,
        DividerItemDecoration.VERTICAL
      )
    )

    adapter = FolderOverviewAdapter { toDelete ->
      val toDeleteName = toDelete.folder
      MaterialDialog.Builder(activity)
        .title(R.string.delete_folder)
        .content("${getString(R.string.delete_folder_content)}\n$toDeleteName")
        .positiveText(R.string.remove)
        .negativeText(R.string.dialog_cancel)
        .onPositive { _, _ ->
          presenter.removeFolder(toDelete)
        }
        .show()
    }
    binding.recycler.adapter = adapter

    binding.fam.setOnFloatingActionsMenuUpdateListener(famMenuListener)

    binding.addAsSingle.title =
        "${getString(R.string.folder_add_single_book)}\n${getString(R.string.for_example)} Harry Potter 4"
    binding.addAsLibrary.title =
        "${getString(R.string.folder_add_collection)}\n${getString(R.string.for_example)} AudioBooks"

    setupToolbar()
  }

  private fun setupToolbar() {
    binding.toolbarInclude!!.toolbar.apply {
      setTitle(R.string.audiobook_folders_title)
      setNavigationIcon(R.drawable.close)
      setNavigationOnClickListener { activity.onBackPressed() }
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
      val finalRadius = Math.max(binding.overlay.width, binding.overlay.height)

      // create the animator for this view (the start radius is zero)
      val anim = ViewAnimationUtils.createCircularReveal(
        binding.overlay,
        famCenter.x, famCenter.y, 0f, finalRadius.toFloat()
      )

      // make the view visible and start the animation
      binding.overlay.visible = true
      anim.start()
    }

    override fun onMenuCollapsed() {
      // get the center for the clipping circle
      getFamCenter(famCenter)

      // get the initial radius for the clipping circle
      val initialRadius = Math.max(binding.overlay.height, binding.overlay.width)

      // create the animation (the final radius is zero)
      val anim = ViewAnimationUtils.createCircularReveal(
        binding.overlay,
        famCenter.x, famCenter.y, initialRadius.toFloat(), 0f
      )

      // make the view invisible when the animation is done
      anim.addListener(
        object : AnimatorListenerAdapter() {
          override fun onAnimationEnd(animation: Animator) {
            super.onAnimationEnd(animation)
            binding.overlay.setVisibleWeak()
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
      binding.fam.left + ((buttonRepresentingTheFam.left + buttonRepresentingTheFam.right) / 2)
    val y = binding.fam.top + ((buttonRepresentingTheFam.top + buttonRepresentingTheFam.bottom) / 2)
    point.set(x, y)
  }

  override fun onRestoreViewState(view: View, savedViewState: Bundle) {
    // restoring overlay
    binding.overlay.visibility = savedViewState.getInt(SI_BACKGROUND_VISIBILITY)
  }

  private fun startFolderChooserActivity(operationMode: FolderChooserActivity.OperationMode) {
    val intent = FolderChooserActivity.newInstanceIntent(activity, operationMode)
    // we don't want our listener be informed.
    binding.fam.setOnFloatingActionsMenuUpdateListener(null)
    binding.fam.collapseImmediately()
    binding.fam.setOnFloatingActionsMenuUpdateListener(famMenuListener)

    binding.overlay.visible = false
    startActivity(intent)
  }

  override fun handleBack(): Boolean = if (binding.fam.isExpanded) {
    binding.fam.collapse()
    true
  } else false

  /** Updates the adapter with new contents. **/
  fun newData(models: Collection<FolderModel>) {
    adapter.newItems(models)
  }

  override fun onSaveViewState(view: View, outState: Bundle) {
    super.onSaveViewState(view, outState)
    outState.putInt(SI_BACKGROUND_VISIBILITY, binding.overlay.visibility)
  }
}
