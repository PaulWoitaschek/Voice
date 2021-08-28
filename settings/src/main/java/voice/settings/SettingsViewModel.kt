package voice.settings

import de.paulwoitaschek.flowpref.Pref
import de.ph1b.audiobook.common.DARK_THEME_SETTABLE
import de.ph1b.audiobook.common.pref.PrefKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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
  private val seekTimePref: Pref<Int>
) {

  private val _viewEffects = MutableSharedFlow<SettingsViewEffect>(extraBufferCapacity = 1)
  val viewEffects: Flow<SettingsViewEffect> get() = _viewEffects

  private val dialog = MutableStateFlow<SettingsViewState.Dialog?>(null)

  fun viewState(): Flow<SettingsViewState> {
    return combine(
      useDarkTheme.flow,
      resumeOnReplugPref.flow,
      autoRewindAmountPref.flow,
      seekTimePref.flow,
      dialog
    ) { useDarkTheme, resumeOnReplug, autoRewindAmount, seekTime, dialog ->
      SettingsViewState(
        useDarkTheme = useDarkTheme,
        showDarkThemePref = DARK_THEME_SETTABLE,
        resumeOnReplug = resumeOnReplug,
        seekTimeInSeconds = seekTime,
        autoRewindInSeconds = autoRewindAmount,
        dialog = dialog
      )
    }
  }

  fun close() {
    SettingsViewEffect.CloseScreen.emit()
  }

  fun toggleResumeOnReplug() {
    resumeOnReplugPref.value = !resumeOnReplugPref.value
  }

  fun toggleDarkTheme() {
    useDarkTheme.value = !useDarkTheme.value
  }

  fun seekAmountChanged(seconds: Int) {
    seekTimePref.value = seconds
  }

  fun autoRewindAmountChanged(seconds: Int) {
    autoRewindAmountPref.value = seconds
  }

  fun onLikeClicked() {
    dialog.tryEmit(SettingsViewState.Dialog.Contribute)
  }

  fun dismissContributeDialog() {
    dialog.tryEmit(null)
  }

  fun openSupport() {
    dialog.tryEmit(null)
    SettingsViewEffect.ToSupport.emit()
  }

  fun openTranslations() {
    dialog.tryEmit(null)
    SettingsViewEffect.ToTranslations.emit()
  }

  private fun SettingsViewEffect.emit() {
    _viewEffects.tryEmit(this)
  }
}
