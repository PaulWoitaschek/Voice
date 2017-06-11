package de.ph1b.audiobook.features.folderChooser

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.graphics.Color
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.widget.AdapterView
import android.widget.Toast
import de.ph1b.audiobook.R
import de.ph1b.audiobook.databinding.ActivityFolderChooserBinding
import de.ph1b.audiobook.features.folderChooser.FolderChooserActivity.Companion.newInstanceIntent
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.*
import de.ph1b.audiobook.mvp.RxBaseActivity
import de.ph1b.audiobook.uitools.visible
import i
import java.io.File

/**
 * Activity for choosing an audiobook folder. If there are multiple SD-Cards, the Activity unifies
 * them to a fake-folder structure. We must make sure that this is not choosable. When there are no
 * multiple sd-cards, we will directly show the content of the 1 SD Card.
 *
 *
 * Use [newInstanceIntent] to get a new intent with the necessary
 * values.
 *
 * @author Paul Woitaschek
 */
class FolderChooserActivity : RxBaseActivity<FolderChooserView, FolderChooserPresenter>(), FolderChooserView {

  override fun newPresenter() = FolderChooserPresenter()

  override fun provideView() = this

  override fun showSubFolderWarning(first: String, second: String) {
    val message = "${getString(R.string.adding_failed_subfolder)}\n$first\n$second"
    Toast.makeText(this, message, Toast.LENGTH_LONG)
        .show()
  }

  private lateinit var binding: ActivityFolderChooserBinding
  private lateinit var adapter: FolderChooserAdapter
  private lateinit var spinnerAdapter: MultiLineSpinnerAdapter<File>
  private lateinit var permissions: Permissions
  private lateinit var permissionHelper: PermissionHelper

  override fun getMode() = OperationMode.valueOf(intent.getStringExtra(NI_OPERATION_MODE))

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    App.component.inject(this)

    permissions = Permissions(this)
    permissionHelper = PermissionHelper(this, permissions)

    binding = DataBindingUtil.setContentView(this, R.layout.activity_folder_chooser)

    setupToolbar()

    // listeners
    binding.choose.setOnClickListener { presenter().chooseClicked() }
    binding.abort.setOnClickListener { finish() }
    binding.upButton.setOnClickListener { onBackPressed() }

    //recycler
    adapter = FolderChooserAdapter(this, getMode()) {
      presenter().fileSelected(it)
    }
    binding.recycler.layoutManager = LinearLayoutManager(this)
    binding.recycler.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
    binding.recycler.adapter = adapter
    val itemAnimator = binding.recycler.itemAnimator as DefaultItemAnimator
    itemAnimator.supportsChangeAnimations = false

    // spinner
    spinnerAdapter = MultiLineSpinnerAdapter(binding.toolSpinner, this, Color.WHITE) { file, _ ->
      if (file.absolutePath == FolderChooserPresenter.MARSHMALLOW_SD_FALLBACK) {
        getString(R.string.storage_all)
      } else {
        file.name
      }
    }
    binding.toolSpinner.adapter = spinnerAdapter
    binding.toolSpinner.itemSelections {
      if (it != AdapterView.INVALID_POSITION) {
        i { "spinner selected with position $it and adapter.count ${spinnerAdapter.count}" }
        val item = spinnerAdapter.getItem(it)
        presenter().fileSelected(item)
      }
    }
  }

  private fun setupToolbar() {
    binding.toolbar.setNavigationIcon(R.drawable.close)
    binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    binding.toolbar.setTitle(R.string.audiobook_folders_title)
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    this.permissions.onRequestPermissionsResult(requestCode, permissions, grantResults)
  }

  override fun onStart() {
    super.onStart()

    // permissions
    permissionHelper.storagePermission { presenter().gotPermission() }
  }

  override fun onBackPressed() {
    if (!presenter().backConsumed()) {
      super.onBackPressed()
    }
  }

  override fun setCurrentFolderText(text: String) {
    binding.currentFolder.text = text
  }

  override fun showNewData(newData: List<File>) {
    adapter.newData(newData)
  }

  override fun setChooseButtonEnabled(chooseEnabled: Boolean) {
    binding.choose.isEnabled = chooseEnabled
  }

  override fun newRootFolders(newFolders: List<File>) {
    i { "newRootFolders called with $newFolders" }
    binding.spinnerGroup.visible = newFolders.size > 1
    spinnerAdapter.setData(newFolders)
  }


  /**
   * Sets the choose button enabled or disabled, depending on where we are in the hierarchy
   */
  override fun setUpButtonEnabled(upEnabled: Boolean) {
    binding.upButton.isEnabled = upEnabled
    val upIcon = if (upEnabled) drawable(R.drawable.ic_arrow_upward) else null
    binding.upButton.setImageDrawable(upIcon)
  }

  enum class OperationMode {
    COLLECTION_BOOK,
    SINGLE_BOOK
  }

  companion object {

    private const val NI_OPERATION_MODE = "niOperationMode"

    /**
     * Generates a new intent with the necessary extras

     * @param c             The context
     * *
     * @param operationMode The operation mode for the activity
     * *
     * @return The new intent
     */
    fun newInstanceIntent(c: Context, operationMode: OperationMode): Intent {
      val intent = Intent(c, FolderChooserActivity::class.java)
      intent.putExtra(NI_OPERATION_MODE, operationMode.name)
      return intent
    }
  }
}
