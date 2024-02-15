package voice.settings

import android.database.Cursor.FIELD_TYPE_BLOB
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import android.content.Intent
import java.io.File
import java.io.FileOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import androidx.core.content.FileProvider
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
import androidx.activity.ComponentActivity
import android.content.ContextWrapper
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import android.net.Uri
import androidx.compose.runtime.MutableState
import voice.logging.core.Logger
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter


const val CSV_NEWLINE = "\n"
const val CSV_INDICATOR_START = "{START}"
const val CSV_INDICATOR_END = "{END}"
const val CSV_INDICATOR_TABLE = "{TABLE}"
const val CSV_COMMA_REPLACE = "{COMMA}"

fun Context.getActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> this.baseContext.getActivity()
    else -> null
}

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

  // val getContent = registerForActivityResult(GetContent()) { uri: Uri? ->
  //     // Handle the returned Uri
  // }

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
    val suppDb = appDb.openHelper.readableDatabase
    val sql = StringBuilder()
    val csr = appDb.query(
      "SELECT name FROM sqlite_master " +
              "WHERE type='table' " +
              "AND name NOT LIKE('sqlite_%') " +
              "AND name NOT LIKE('room_%') " +
              "AND name NOT LIKE('bookSearchFts_%') " +
              "AND name NOT LIKE('android_%')",
      arrayOf<Any>()
    )

    var rows = mutableListOf(listOf("Voice Export"))

    while (csr.moveToNext()) {
        sql.clear()
        sql.append("SELECT ")
        val currentTableName = csr.getString(0)
        rows.add(listOf("========"))
        rows.add(listOf("Table", currentTableName))
        val colNames = getTableColumnNames(currentTableName,suppDb)
        sql.append(colNames.joinToString(","))
        rows.add(colNames)
        rows.add(listOf("========"))
        sql.append(" FROM `${currentTableName}`")
        val csr2 = appDb.query(sql.toString(),null)
        while (csr2.moveToNext()) {
          val row = mutableListOf<String>()
          for (i in 0..csr2.getColumnCount() - 1) {
            val type = csr2.getType(i)
            if (type == FIELD_TYPE_BLOB) {
              row.add("{blob}")
              continue
            }

            val got = csr2.getString(i)
            if (got != null) {
              row.add(got)
            } else {
              row.add("")
            }
          }
          rows.add(row)
        }
    }

    Logger.w("Calling save file")
    saveFile({ uri ->
      Logger.w("Saving file $uri")
      val stream = context.contentResolver.openOutputStream(uri)!!
      csvWriter().writeAll(rows, stream)
    })
  }

  private fun getTableColumnNames(tableName: String, suppDB: SupportSQLiteDatabase): List<String> {
    val rv = arrayListOf<String>()
    val csr = suppDB.query("SELECT name FROM pragma_table_info('${tableName}')",arrayOf<Any>())
    while (csr.moveToNext()) {
        rv.add(csr.getString(0))
    }
    csr.close()
    return rv.toList()
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
