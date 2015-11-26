package de.ph1b.audiobook.activity

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.widget.Button
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import de.ph1b.audiobook.R
import de.ph1b.audiobook.adapter.FolderChooserAdapter
import de.ph1b.audiobook.dialog.HideFolderDialog
import de.ph1b.audiobook.model.NaturalOrderComparator
import de.ph1b.audiobook.utils.FileRecognition
import de.ph1b.audiobook.utils.PermissionHelper
import timber.log.Timber
import java.io.File
import java.util.*
import java.util.regex.Pattern

/**
 * Activity for choosing an audiobook folder. If there are multiple SD-Cards, the Activity unifies
 * them to a fake-folder structure. We must make sure that this is not choosable. When there are no
 * multiple sd-cards, we will directly show the content of the 1 SD Card.
 *
 *
 * Use [.newInstanceIntent] to get a new intent with the necessary
 * values.

 * @author Paul Woitaschek
 */
class FolderChooserActivity : BaseActivity(), HideFolderDialog.OnChosenListener {
    private val currentFolderContent = ArrayList<File>(30)
    private lateinit var upButton: ImageButton
    private lateinit var currentFolderName: TextView
    private lateinit var chooseButton: Button
    private lateinit var toolbar: Toolbar
    private lateinit var listView: ListView
    private lateinit var chosenFolderDescription: TextView
    private var multiSd = true
    private lateinit var rootDirs: List<File>
    private var currentFolder: File? = null
    private var chosenFile: File? = null
    private lateinit var adapter: FolderChooserAdapter
    private lateinit var mode: OperationMode

    private fun changeFolder(newFolder: File) {
        currentFolder = newFolder
        currentFolderContent.clear()
        currentFolderContent.addAll(getFilesFromFolder(currentFolder!!))
        adapter.notifyDataSetChanged()
        setButtonEnabledDisabled()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val permissionGrantingWorked = PermissionHelper.permissionGrantingWorked(requestCode,
                    PERMISSION_RESULT_READ_EXT_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,
                    permissions, grantResults)
            Timber.i("permissionGrantingWorked=%b", permissionGrantingWorked)
            if (permissionGrantingWorked) {
                refreshRootDirs()
            } else {
                PermissionHelper.handleExtStorageRescan(this, PERMISSION_RESULT_READ_EXT_STORAGE)
                Timber.e("could not get permission")
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun askForReadExternalStoragePermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_RESULT_READ_EXT_STORAGE)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folder_chooser)
        upButton = findViewById(R.id.twoline_image1) as ImageButton
        currentFolderName = findViewById(R.id.twoline_text2) as TextView
        chooseButton = findViewById(R.id.choose) as Button
        toolbar = findViewById(R.id.toolbar) as Toolbar
        listView = findViewById(R.id.listView) as ListView
        chosenFolderDescription = findViewById(R.id.twoline_text1) as TextView

        chooseButton.setOnClickListener({ chooseClicked() })
        // cancel button
        findViewById(R.id.abort).setOnClickListener({ finish() })
        upButton.setOnClickListener({ up() })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val hasExternalStoragePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            Timber.i("hasExternalStoragePermission=%b", hasExternalStoragePermission)
            if (!hasExternalStoragePermission) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    PermissionHelper.handleExtStorageRescan(this, PERMISSION_RESULT_READ_EXT_STORAGE)
                } else {
                    askForReadExternalStoragePermission()
                }
            }
        }

        mode = OperationMode.valueOf(intent.getStringExtra(NI_OPERATION_MODE))

        // toolbar
        setSupportActionBar(toolbar)

        //setup
        adapter = FolderChooserAdapter(this, currentFolderContent, mode)
        listView.adapter = adapter
        listView.setOnItemClickListener { parent, view, position, id ->
            val selectedFile = adapter.getItem(position)
            if (selectedFile.isDirectory && selectedFile.canRead()) {
                chosenFile = selectedFile
                currentFolderName.text = chosenFile!!.name
                changeFolder(adapter.getItem(position))
            } else if (mode == OperationMode.SINGLE_BOOK && selectedFile.isFile) {
                chosenFile = selectedFile
                currentFolderName.text = chosenFile!!.name
            }
        }
        chosenFolderDescription.setText(R.string.chosen_folder_description)

        refreshRootDirs()

        //handle runtime
        if (savedInstanceState != null) {
            val savedFolderPath = savedInstanceState.getString(CURRENT_FOLDER_NAME)
            if (savedFolderPath != null) {
                val f = File(savedFolderPath)
                if (f.exists() && f.canRead()) {
                    chosenFile = f
                    currentFolderName.text = chosenFile!!.name
                    changeFolder(f)
                }
            }
        }

        setButtonEnabledDisabled()
    }

    private fun refreshRootDirs() {
        rootDirs = storageDirs()
        currentFolderContent.clear()

        Timber.i("refreshRootDirs found rootDirs=%s", rootDirs)
        if (rootDirs.size == 1) {
            chosenFile = rootDirs[0]
            currentFolderName.text = chosenFile!!.name
            changeFolder(chosenFile!!)
            multiSd = false
        } else {
            currentFolderContent.addAll(rootDirs)
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (currentFolder != null) {
            outState.putString(CURRENT_FOLDER_NAME, currentFolder!!.absolutePath)
        }
    }

    override fun onBackPressed() {
        if (canGoBack()) {
            up()
        } else {
            super.onBackPressed()
        }
    }

    private fun canGoBack(): Boolean {
        if (multiSd) {
            return currentFolder != null
        } else {
            for (f in rootDirs) {
                if (f == currentFolder) {
                    return false //to go up we must not already be in top level
                }
            }
            return true
        }
    }

    private fun up() {
        Timber.d("up called. currentFolder=%s", currentFolder)

        var chosenFolderIsInRoot = false
        for (f in rootDirs) {
            if (f == currentFolder) {
                Timber.d("chosen folder is in root")
                chosenFolderIsInRoot = true
            }
        }
        if (multiSd && chosenFolderIsInRoot) {
            currentFolder = null
            currentFolderName.text = ""
            currentFolderContent.clear()
            currentFolderContent.addAll(rootDirs)
            adapter.notifyDataSetChanged()
        } else {
            currentFolder = currentFolder!!.parentFile
            chosenFile = currentFolder
            currentFolderName.text = currentFolder!!.name
            val parentContaining = getFilesFromFolder(currentFolder!!)
            currentFolderContent.clear()
            currentFolderContent.addAll(parentContaining)
            adapter.notifyDataSetChanged()
        }
        setButtonEnabledDisabled()
    }

    /**
     * Sets the choose button enabled or disabled, depending on where we are in the hierarchy
     */
    private fun setButtonEnabledDisabled() {
        val upEnabled = canGoBack()
        val chooseEnabled = !multiSd || upEnabled

        chooseButton.isEnabled = chooseEnabled
        upButton.isEnabled = upEnabled
        val upIcon = if (upEnabled) ContextCompat.getDrawable(this, R.drawable.ic_arrow_up_white_48dp) else null
        upButton.setImageDrawable(upIcon)
    }

    internal fun chooseClicked() {
        if (chosenFile!!.isDirectory && !HideFolderDialog.getNoMediaFileByFolder(chosenFile!!).exists()) {
            val hideFolderDialog = HideFolderDialog.newInstance(chosenFile!!)
            hideFolderDialog.show(supportFragmentManager, HideFolderDialog.TAG)
        } else {
            finishActivityWithSuccess(chosenFile!!)
        }
    }

    private fun storageDirs(): List<File> {
        val dirSeparator = Pattern.compile("/");

        // Final set of paths
        val rv: HashSet<String> = HashSet(5)
        // Primary physical SD-CARD (not emulated)
        val rawExternalStorage = System.getenv("EXTERNAL_STORAGE")
        // All Secondary SD-CARDs (all exclude primary) separated by ":"
        val rawSecondaryStorageStr = System.getenv("SECONDARY_STORAGE")
        // Primary emulated SD-CARD
        val rawEmulatedStorageTarget = System.getenv("EMULATED_STORAGE_TARGET")

        if (TextUtils.isEmpty(rawEmulatedStorageTarget)) {
            // Device has physical external storage; use plain paths.
            if (TextUtils.isEmpty(rawExternalStorage)) {
                // EXTERNAL_STORAGE undefined; falling back to default.
                rv.add("/storage/sdcard0");
            } else {
                rv.add(rawExternalStorage);
            }
        } else {
            // Device has emulated storage; external storage paths should have
            // userId burned into them.
            val rawUserId: String;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                rawUserId = "";
            } else {
                val path = Environment.getExternalStorageDirectory().absolutePath;
                val folders = dirSeparator.split(path);
                val lastFolder = folders[folders.size - 1];
                var isDigit = false;
                try {
                    Integer.valueOf(lastFolder);
                    isDigit = true;
                } catch (ignored: NumberFormatException) {
                }
                rawUserId = if (isDigit) lastFolder else ""
            }
            // /storage/emulated/0[1,2,...]
            if (TextUtils.isEmpty(rawUserId)) {
                rv.add(rawEmulatedStorageTarget);
            } else {
                rv.add(rawEmulatedStorageTarget + File.separator + rawUserId);
            }
        }
        // Add all secondary storage
        if (!TextUtils.isEmpty(rawSecondaryStorageStr)) {
            // All Secondary SD-CARDs splitted into array
            val rawSecondaryStorage = rawSecondaryStorageStr.split(File.pathSeparator);
            rv.addAll(rawSecondaryStorage)
        }
        rv.add("/storage/extSdCard");
        rv.add(Environment.getExternalStorageDirectory().absolutePath);
        rv.add("/storage/emulated/0");
        rv.add("/storage/sdcard1");
        rv.add("/storage/external_SD");
        rv.add("/storage/ext_sd");

        val paths = ArrayList<File>(rv.size);
        for (item  in rv) {
            val f = File(item);
            if (f.exists() && f.isDirectory && f.canRead() && f.listFiles() != null && f.listFiles().size > 0) {
                paths.add(f);
            }
        }
        Collections.sort(paths, NaturalOrderComparator.FILE_COMPARATOR);
        return paths;
    }

    private fun finishActivityWithSuccess(chosenFile: File) {
        val data = Intent()
        data.putExtra(RESULT_CHOSEN_FILE, chosenFile.absolutePath)
        data.putExtra(RESULT_OPERATION_MODE, mode.name)
        setResult(Activity.RESULT_OK, data)
        finish()
    }


    override fun onChosen() {
        finishActivityWithSuccess(chosenFile!!)
    }

    enum class OperationMode {
        COLLECTION_BOOK,
        SINGLE_BOOK
    }

    companion object {

        val RESULT_CHOSEN_FILE = "chosenFile"
        val RESULT_OPERATION_MODE = "operationMode"

        private val CURRENT_FOLDER_NAME = "currentFolderName"
        private val NI_OPERATION_MODE = "niOperationMode"
        private val PERMISSION_RESULT_READ_EXT_STORAGE = 1


        /**
         * Gets the containing files of a folder (restricted to music and folders) in a naturally sorted
         * order.

         * @param file The file to look for containing files
         * *
         * @return The containing files
         */
        private fun getFilesFromFolder(file: File): List<File> {
            val containing = file.listFiles(FileRecognition.FOLDER_AND_MUSIC_FILTER)
            if (containing != null) {
                val asList = ArrayList(Arrays.asList(*containing))
                Collections.sort(asList, NaturalOrderComparator.FILE_COMPARATOR)
                return asList
            } else {
                return emptyList()
            }
        }

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