package voice.settings

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import android.content.Context
import android.content.Intent
import java.io.File
import androidx.core.net.toUri
import de.paulwoitaschek.flowpref.Pref
import voice.common.AppInfoProvider
import voice.common.DARK_THEME_SETTABLE
import voice.common.grid.GridCount
import voice.common.grid.GridMode
import voice.common.navigation.Destination
import voice.common.navigation.Navigator
import voice.common.pref.PrefKeys
import voice.data.repo.internals.AppDb
import javax.inject.Inject
import javax.inject.Named
import android.net.Uri
import java.nio.file.Files
import kotlin.io.path.Path
import java.io.OutputStream
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.zip.ZipInputStream


class SettingsViewModel
@Inject constructor(
  @Named(PrefKeys.DARK_THEME)
  private val useDarkTheme: Pref<Boolean>,
  @Named(PrefKeys.AUTO_REWIND_AMOUNT)
  private val autoRewindAmountPref: Pref<Int>,
  @Named(PrefKeys.SEEK_TIME)
  private val seekTimePref: Pref<Int>,
  private val navigator: Navigator,
  private val appInfoProvider: AppInfoProvider,
  @Named(PrefKeys.GRID_MODE)
  private val gridModePref: Pref<GridMode>,
  private val gridCount: GridCount,
  private val appDb: AppDb,
  private val context: Context,
) : SettingsListener {

  private val dialog = mutableStateOf<SettingsViewState.Dialog?>(null)

  @Composable
  fun viewState(): SettingsViewState {
    val useDarkTheme by remember { useDarkTheme.flow }.collectAsState(initial = false)
    val autoRewindAmount by remember { autoRewindAmountPref.flow }.collectAsState(initial = 0)
    val seekTime by remember { seekTimePref.flow }.collectAsState(initial = 0)
    val gridMode by remember { gridModePref.flow }.collectAsState(initial = GridMode.GRID)
    return SettingsViewState(
      useDarkTheme = useDarkTheme,
      showDarkThemePref = DARK_THEME_SETTABLE,
      seekTimeInSeconds = seekTime,
      autoRewindInSeconds = autoRewindAmount,
      dialog = dialog.value,
      appVersion = appInfoProvider.versionName,
      useGrid = when (gridMode) {
        GridMode.LIST -> false
        GridMode.GRID -> true
        GridMode.FOLLOW_DEVICE -> gridCount.useGridAsDefault()
      },
    )
  }

  override fun close() {
    navigator.goBack()
  }

  override fun toggleDarkTheme() {
    useDarkTheme.value = !useDarkTheme.value
  }

  override fun toggleGrid() {
    gridModePref.value = when (gridModePref.value) {
      GridMode.LIST -> GridMode.GRID
      GridMode.GRID -> GridMode.LIST
      GridMode.FOLLOW_DEVICE -> if (gridCount.useGridAsDefault()) {
        GridMode.LIST
      } else {
        GridMode.GRID
      }
    }
  }

  override fun seekAmountChanged(seconds: Int) {
    seekTimePref.value = seconds
  }

  override fun onSeekAmountRowClicked() {
    dialog.value = SettingsViewState.Dialog.SeekTime
  }

  override fun autoRewindAmountChanged(seconds: Int) {
    autoRewindAmountPref.value = seconds
  }

  override fun onAutoRewindRowClicked() {
    dialog.value = SettingsViewState.Dialog.AutoRewindAmount
  }

  override fun dismissDialog() {
    dialog.value = null
  }

  override fun getSupport() {
    navigator.goTo(Destination.Website("https://github.com/PaulWoitaschek/Voice/discussions/categories/q-a"))
  }

  override fun suggestIdea() {
    navigator.goTo(Destination.Website("https://github.com/PaulWoitaschek/Voice/discussions/categories/ideas"))
  }

  override fun export(saveFile: (handle: (uri: Uri) -> Unit) -> Unit) {
    val db = appDb.openHelper.readableDatabase
    saveFile({ uri ->
      val outp: OutputStream = context.contentResolver.openOutputStream(uri)!!
      val zip = ZipOutputStream(outp)

      val files = listOf(File(db.path!!), File(db.path!! + "-shm"), File(db.path!! + "-wal"))
      for (file in files) {
        if (!file.exists()) {
          continue
        }
        zip.putNextEntry(ZipEntry(file.name))

        val fileInputStream = file.inputStream()
        val buffer = ByteArray(1024)
        var length: Int

        while (fileInputStream.read(buffer).also { length = it } > 0) {
            zip.write(buffer, 0, length)
        }

        fileInputStream.close()
        zip.closeEntry()
      }

      zip.close()
    })
  }

  override fun import(openFile: (handle: (uri: Uri) -> Unit) -> Unit) {
    openFile({ uri ->
      val db = appDb.openHelper.readableDatabase
      val inp = context.contentResolver.openInputStream(uri)!!
      val zip = ZipInputStream(inp)

      val dbFile = File(db.path!!)

      var entry = zip.getNextEntry()
      while (entry != null) {
        if (!entry.name.startsWith(dbFile.name) || entry.name.contains("/")) {
          // invalid
          continue
        }
        val outp = Path(dbFile.parent!!, entry.name)
        Files.copy(zip, outp, REPLACE_EXISTING)

        entry = zip.getNextEntry()
      }

      val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)!!
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
      context.startActivity(intent)
      System.exit(0)
    })
  }

  override fun openBugReport() {
    val url = "https://github.com/PaulWoitaschek/Voice/issues/new".toUri()
      .buildUpon()
      .appendQueryParameter("template", "bug.yml")
      .appendQueryParameter("version", appInfoProvider.versionName)
      .appendQueryParameter("androidversion", Build.VERSION.SDK_INT.toString())
      .appendQueryParameter("device", Build.MODEL)
      .toString()
    navigator.goTo(Destination.Website(url))
  }

  override fun openTranslations() {
    dismissDialog()
    navigator.goTo(Destination.Website("https://hosted.weblate.org/engage/voice/"))
  }
}
