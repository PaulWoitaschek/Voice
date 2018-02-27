package de.ph1b.audiobook.features.folderChooser

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Environment
import android.support.annotation.RequiresPermission
import android.text.TextUtils
import de.ph1b.audiobook.common.comparator.NaturalOrderComparator
import timber.log.Timber
import java.io.File
import java.util.*
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Finder for external storages.
 */
@Singleton
class StorageDirFinder @Inject constructor(private val context: Context) {

  /**
   * Collects the storage dirs of the device.
   *
   * @return the list of storages.
   */
  @SuppressLint("MissingPermission")
  @RequiresPermission(value = Manifest.permission.READ_EXTERNAL_STORAGE)
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
      } else {
        rv.add(rawExternalStorage)
      }
    } else {
      // Device has emulated storage; external storage paths should have
      // userId burned into them.
      val path = Environment.getExternalStorageDirectory().absolutePath
      val folders = dirSeparator.split(path)
      val lastFolder = folders[folders.size - 1]
      var isDigit = false
      try {
        Integer.valueOf(lastFolder)
        isDigit = true
      } catch (ignored: NumberFormatException) {
      }
      val rawUserId = if (isDigit) lastFolder else ""
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
    rv.add(Environment.getExternalStorageDirectory().absolutePath)
    rv.add("/storage/extSdCard")
    rv.add("/storage/emulated/0")
    rv.add("/storage/sdcard1")
    rv.add("/storage/external_SD")
    rv.add("/storage/ext_sd")
    rv.add("/storage/sdcard0")
    rv.add("/mnt/external_sd")
    rv.add("/mnt/external_sd1")
    rv.add("/mnt/external_sd2")

    // this is a workaround for marshmallow as we can't know the paths of the sd cards any more.
    // if one of the files in the fallback dir has contents we add it to the list.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      rv.add(FolderChooserPresenter.MARSHMALLOW_SD_FALLBACK)
    }
    rv.addAll(storageDirs2())

    // get the non empty files
    val nonEmptyFiles = rv.map(::File).filter { it.length() > 0 }
    // make sure they are unique by putting them with their canonical path as key
    val map = HashMap<String, File>()
    nonEmptyFiles.forEach {
      map.put(it.canonicalPath, it)
    }
    // sort them
    return map.values.sortedWith(NaturalOrderComparator.fileComparator)
  }

  // solution from http://stackoverflow.com/a/40205116
  private fun storageDirs2(): List<String> {

    val results = ArrayList<String>()

    //Method 1 for KitKat & above
    val externalDirs: Array<File?>? = context.getExternalFilesDirs(null)
    externalDirs?.forEach {
      if (it != null) {
        val path = it.path.split("/Android")[0]
        val externalStorageRemovable = try {
          Environment.isExternalStorageRemovable(it)
        } catch (e: IllegalArgumentException) {
          Timber.e(e, "Error in isExternalStorageRemovable")
          false
        }
        if (externalStorageRemovable) {
          results.add(path)
        }
      }
    }

    //Method 2 for all versions
    // better variation of: http://stackoverflow.com/a/40123073/5002496
    var output = ""
    try {
      val process = ProcessBuilder().command("mount | grep /dev/block/vold")
        .redirectErrorStream(true).start()
      process.waitFor()
      val inputStream = process.inputStream
      val buffer = ByteArray(1024)
      while (inputStream.read(buffer) != -1) {
        output += String(buffer)
      }
      inputStream.close()
    } catch (ignored: Exception) {
    }

    if (!output.trim { it <= ' ' }.isEmpty()) {
      val devicePoints = output.split("\n".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
      devicePoints.mapTo(results) { it.split(" ".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()[2] }
    }

    //Below few lines is to remove paths which may not be external memory card, like OTG (feel free to comment them out)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      var i = 0
      while (i < results.size) {
        if (!results[i].toLowerCase().matches(".*[0-9a-f]{4}[-][0-9a-f]{4}".toRegex())) {
          results.removeAt(i--)
        }
        i++
      }
    } else {
      var i = 0
      while (i < results.size) {
        if (!results[i].toLowerCase().contains("ext") && !results[i].toLowerCase().contains("sdcard")) {
          results.removeAt(i--)
        }
        i++
      }
    }

    return results
  }

}
