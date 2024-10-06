package voice.settings
import android.net.Uri

interface SettingsListener {
  fun close()
  fun toggleDarkTheme()
  fun toggleGrid()
  fun seekAmountChanged(seconds: Int)
  fun onSeekAmountRowClick()
  fun autoRewindAmountChang(seconds: Int)
  fun onAutoRewindRowClick()
  fun dismissDialog()
  fun getSupport()
  fun suggestIdea()
  fun backup(saveFile: (handle: (uri: Uri) -> Unit) -> Unit)
  fun restore(openFile: (handle: (uri: Uri) -> Unit) -> Unit)
  fun openBugReport()
  fun openTranslations()
}
