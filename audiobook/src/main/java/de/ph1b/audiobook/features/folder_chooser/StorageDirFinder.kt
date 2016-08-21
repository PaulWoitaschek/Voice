package de.ph1b.audiobook.features.folder_chooser

import android.Manifest
import android.os.Build
import android.os.Environment
import android.support.annotation.RequiresPermission
import android.text.TextUtils
import de.ph1b.audiobook.misc.NaturalOrderComparator
import java.io.File
import java.util.*
import java.util.regex.Pattern

/**
 * Finder for external storages.
 *
 * @author Paul Woitaschek
 */
object StorageDirFinder {

    /**
     * Collects the storage dirs of the device.
     *
     * @return the list of storages.
     */
    @RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    fun storageDirs(): List<File> {
        val dirSeparator = Pattern.compile("/")

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
                rv.add("/storage/sdcard0")
            } else {
                rv.add(rawExternalStorage)
            }
        } else {
            // Device has emulated storage; external storage paths should have
            // userId burned into them.
            val rawUserId = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                ""
            } else {
                val path = Environment.getExternalStorageDirectory().absolutePath
                val folders = dirSeparator.split(path)
                val lastFolder = folders[folders.size - 1]
                var isDigit = false
                try {
                    Integer.valueOf(lastFolder)
                    isDigit = true
                } catch (ignored: NumberFormatException) {
                }
                if (isDigit) lastFolder else ""
            }
            // /storage/emulated/0[1,2,...]
            if (TextUtils.isEmpty(rawUserId)) {
                rv.add(rawEmulatedStorageTarget)
            } else {
                rv.add(rawEmulatedStorageTarget + File.separator + rawUserId)
            }
        }
        // Add all secondary storage
        if (!TextUtils.isEmpty(rawSecondaryStorageStr)) {
            // All Secondary SD-CARDs splitted into array
            val rawSecondaryStorage = rawSecondaryStorageStr.split(File.pathSeparator)
            rv.addAll(rawSecondaryStorage)
        }
        rv.add("/storage/extSdCard")
        rv.add(Environment.getExternalStorageDirectory().absolutePath)
        rv.add("/storage/emulated/0")
        rv.add("/storage/sdcard1")
        rv.add("/storage/external_SD")
        rv.add("/storage/ext_sd")

        // this is a workaround for marshmallow as we can't know the paths of the sd cards any more.
        // if one of the files in the fallback dir has contents we add it to the list.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val fallbackFile = File(FolderChooserPresenter.MARSHMALLOW_SD_FALLBACK)
            val contents = fallbackFile.listFilesSafely()
            for (content in contents) {
                if (content.listFilesSafely().isNotEmpty()) {
                    rv.add(FolderChooserPresenter.MARSHMALLOW_SD_FALLBACK)
                    break
                }
            }
        }

        val paths = ArrayList<File>(rv.size)
        for (item in rv) {
            val f = File(item)
            if (f.listFilesSafely().isNotEmpty()) {
                paths.add(f)
            }
        }
        return paths.sortedWith(NaturalOrderComparator.FILE_COMPARATOR)
    }

    /**
     * As there are cases where [File.listFiles] returns null even though it is a directory, we return
     * an empty list instead.
     */
    private fun File.listFilesSafely(): List<File> {
        val list: Array<File>? = listFiles()
        return list?.toList() ?: emptyList()
    }
}