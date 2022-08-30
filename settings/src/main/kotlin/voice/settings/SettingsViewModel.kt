package voice.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import de.paulwoitaschek.flowpref.Pref
import voice.common.AppInfoProvider
import voice.common.DARK_THEME_SETTABLE
import voice.common.navigation.Destination
import voice.common.navigation.Navigator
import voice.common.pref.PrefKeys
import javax.inject.Inject
import javax.inject.Named

class SettingsViewModel
@Inject constructor(
  @Named(PrefKeys.DARK_THEME)
  private val useDarkTheme: Pref<Boolean>,
  @Named(PrefKeys.RESUME_ON_REPLUG)
  private val resumeOnReplugPref: Pref<Boolean>,
  @Named(PrefKeys.AUTO_REWIND_AMOUNT)
  private val autoRewindAmountPref: Pref<Int>,
  @Named(PrefKeys.SEEK_TIME)
  private val seekTimePref: Pref<Int>,
  private val navigator: Navigator,
  private val appInfoProvider: AppInfoProvider,
) : SettingsListener {

  private val dialog = mutableStateOf<SettingsViewState.Dialog?>(null)

  @Composable
  fun viewState(): SettingsViewState {
    val useDarkTheme by remember { useDarkTheme.flow }.collectAsState(initial = false)
    val resumeOnReplug by remember { resumeOnReplugPref.flow }.collectAsState(initial = false)
    val autoRewindAmount by remember { autoRewindAmountPref.flow }.collectAsState(initial = 0)
    val seekTime by remember { seekTimePref.flow }.collectAsState(initial = 0)
    return SettingsViewState(
      useDarkTheme = useDarkTheme,
      showDarkThemePref = DARK_THEME_SETTABLE,
      resumeOnReplug = resumeOnReplug,
      seekTimeInSeconds = seekTime,
      autoRewindInSeconds = autoRewindAmount,
      dialog = dialog.value,
      appVersion = appInfoProvider.versionName,
    )
  }

  override fun close() {
    navigator.goBack()
  }

  override fun toggleResumeOnReplug() {
    resumeOnReplugPref.value = !resumeOnReplugPref.value
  }

  override fun toggleDarkTheme() {
    useDarkTheme.value = !useDarkTheme.value
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

  override fun onLikeClicked() {
    dialog.value = SettingsViewState.Dialog.Contribute
  }

  override fun dismissDialog() {
    dialog.value = null
  }

  override fun openSupport() {
    dismissDialog()
    navigator.goTo(Destination.Website("https://github.com/PaulWoitaschek/Voice"))
  }

  override fun openTranslations() {
    dismissDialog()
    navigator.goTo(Destination.Website("https://www.transifex.com/projects/p/voice"))
  }
}
