package de.ph1b.audiobook.features.settings

import de.ph1b.audiobook.misc.DARK_THEME_SETTABLE
import de.ph1b.audiobook.prefs.Pref
import de.ph1b.audiobook.prefs.PrefKeys
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
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

  private val _viewEffects = BroadcastChannel<SettingsViewEffect>(1)
  val viewEffects: Flow<SettingsViewEffect> get() = _viewEffects.asFlow()

  fun viewState(): Flow<SettingsViewState> {
    return combine(
      useDarkTheme.flow,
      resumeOnReplugPref.flow,
      autoRewindAmountPref.flow,
      seekTimePref.flow
    ) { useDarkTheme, resumeOnreplug, autoRewindAmount, seekTime ->
      SettingsViewState(
        useDarkTheme = useDarkTheme,
        showDarkThemePref = DARK_THEME_SETTABLE,
        resumeOnReplug = resumeOnreplug,
        seekTimeInSeconds = seekTime,
        autoRewindInSeconds = autoRewindAmount
      )
    }
  }

  fun toggleResumeOnReplug() {
    resumeOnReplugPref.value = !resumeOnReplugPref.value
  }

  fun toggleDarkTheme() {
    useDarkTheme.value = !useDarkTheme.value
  }

  fun changeSkipAmount() {
    _viewEffects.offer(SettingsViewEffect.ShowChangeSkipAmountDialog(seekTimePref.value))
  }

  fun changeAutoRewindAmount() {
    _viewEffects.offer(SettingsViewEffect.ShowChangeAutoRewindAmountDialog(seekTimePref.value))
  }
}
