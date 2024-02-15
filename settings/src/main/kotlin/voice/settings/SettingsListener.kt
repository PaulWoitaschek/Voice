package voice.settings
import androidx.activity.ComponentActivity
import android.net.Uri
import androidx.compose.runtime.MutableState

interface SettingsListener {
  fun close()
  fun toggleDarkTheme()
  fun toggleGrid()
  fun seekAmountChanged(seconds: Int)
  fun onSeekAmountRowClicked()
  fun autoRewindAmountChanged(seconds: Int)
  fun onAutoRewindRowClicked()
  fun dismissDialog()
  fun getSupport()
  fun suggestIdea()
  fun export(saveFile: () -> MutableState<Uri?>)
  fun openBugReport()
  fun openTranslations()
}
