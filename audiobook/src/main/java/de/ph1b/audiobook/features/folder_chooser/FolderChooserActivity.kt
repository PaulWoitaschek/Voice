package de.ph1b.audiobook.features.folder_chooser

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.jakewharton.rxbinding.widget.RxAdapterView
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.folder_chooser.FolderChooserActivity.Companion.newInstanceIntent
import de.ph1b.audiobook.misc.MultiLineSpinnerAdapter
import de.ph1b.audiobook.mvp.RxBaseActivity
import de.ph1b.audiobook.uitools.DividerItemDecoration
import i
import kotlinx.android.synthetic.main.activity_folder_chooser.*
import permissions.dispatcher.*
import java.io.File

/**
 * Activity for choosing an audiobook folder. If there are multiple SD-Cards, the Activity unifies
 * them to a fake-folder structure. We must make sure that this is not choosable. When there are no
 * multiple sd-cards, we will directly show the content of the 1 SD Card.
 *
 *
 * Use [newInstanceIntent] to get a new intent with the necessary
 * values.

 * @author Paul Woitaschek
 */
@RuntimePermissions class FolderChooserActivity : RxBaseActivity<FolderChooserView, FolderChooserPresenter>(), FolderChooserView, HideFolderDialog.OnChosenListener {

    override fun newPresenter() = FolderChooserPresenter()

    override fun provideView() = this

    override fun showSubFolderWarning(first: String, second: String) {
        val message = "${getString(R.string.adding_failed_subfolder)}\n$first\n$second"
        Toast.makeText(this, message, Toast.LENGTH_LONG)
                .show()
    }

    private lateinit var adapter: FolderChooserAdapter
    private lateinit var spinnerAdapter: MultiLineSpinnerAdapter<File>

    override fun askAddNoMediaFile(folderToHide: File) {
        val hideFolderDialog = HideFolderDialog.newInstance(folderToHide)
        hideFolderDialog.show(supportFragmentManager, HideFolderDialog.TAG)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        FolderChooserActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults)
    }

    override fun getMode() = OperationMode.valueOf(intent.getStringExtra(NI_OPERATION_MODE))

    @NeedsPermission(STORAGE_PERMISSION) fun ensurePermissions() {
        presenter().gotPermission()
    }

    @OnShowRationale(STORAGE_PERMISSION) fun showRationaleForStorage(request: PermissionRequest) {
        val content = "${getString(R.string.permission_read_ext_explanation)}\n\n${getString(R.string.permission_read_ext_request)}"
        MaterialDialog.Builder(this)
                .cancelable(false)
                .positiveText(R.string.permission_rescan)
                .onPositive { materialDialog, dialogAction -> request.proceed() }
                .content(content)
                .show()
    }

    @OnPermissionDenied(STORAGE_PERMISSION) fun denied() {
        FolderChooserActivityPermissionsDispatcher.ensurePermissionsWithCheck(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // permissions
        FolderChooserActivityPermissionsDispatcher.ensurePermissionsWithCheck(this)

        // find views
        setContentView(R.layout.activity_folder_chooser)

        // toolbar
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // listeners
        choose.setOnClickListener { presenter().chooseClicked() }
        abort.setOnClickListener { finish() }
        upButton.setOnClickListener { onBackPressed() }

        //recycler
        adapter = FolderChooserAdapter(this, getMode()) {
            presenter().fileSelected(it)
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.addItemDecoration(DividerItemDecoration(this))
        recycler.adapter = adapter

        // spinner
        spinnerAdapter = MultiLineSpinnerAdapter(toolSpinner, this, Color.WHITE)
        toolSpinner.adapter = spinnerAdapter
        RxAdapterView.itemSelections(toolSpinner)
                .filter { it != AdapterView.INVALID_POSITION } // filter invalid entries
                .skip(1) // skip the first that passed as its no real user input
                .subscribe {
                    i { "spinner selected with position $it and adapter.count ${spinnerAdapter.count}" }
                    val item = spinnerAdapter.getItem(it)
                    presenter().fileSelected(item.data)
                }
    }

    override fun onBackPressed() {
        if (!presenter().backConsumed()) {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            finish()
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    override fun setCurrentFolderText(text: String) {
        currentFolder.text = text
    }

    override fun showNewData(newData: List<File>) {
        adapter.newData(newData)
    }

    override fun setChooseButtonEnabled(chooseEnabled: Boolean) {
        choose.isEnabled = chooseEnabled
    }

    override fun newRootFolders(newFolders: List<File>) {
        i { "newRootFolders called with $newFolders" }
        spinnerGroup.visibility = if (newFolders.size <= 1) View.INVISIBLE else View.VISIBLE

        val newData = newFolders
                .map {
                    val name = if (it.absolutePath == FolderChooserPresenter.MARSHMALLOW_SD_FALLBACK) {
                        getString(R.string.storage_all)
                    } else {
                        it.name
                    }
                    MultiLineSpinnerAdapter.Data(it, name)
                }
        spinnerAdapter.setData(newData)
    }


    /**
     * Sets the choose button enabled or disabled, depending on where we are in the hierarchy
     */
    override fun setUpButtonEnabled(upEnabled: Boolean) {
        upButton.isEnabled = upEnabled
        val upIcon = if (upEnabled) ContextCompat.getDrawable(this, R.drawable.ic_arrow_upward) else null
        upButton.setImageDrawable(upIcon)
    }

    override fun onChosen() {
        presenter().hideFolderSelectionMade()
    }

    enum class OperationMode {
        COLLECTION_BOOK,
        SINGLE_BOOK
    }

    companion object {

        private const val NI_OPERATION_MODE = "niOperationMode"
        private const val STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE

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