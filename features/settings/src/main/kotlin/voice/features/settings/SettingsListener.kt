package voice.features.settings

import voice.core.data.ThemeColorScheme
import voice.core.data.ThemeMode
import java.time.LocalTime

interface SettingsListener {
  fun close()
  fun onThemeModeRowClick()
  fun onThemeColorSchemeRowClick()
  fun setThemeMode(themeMode: ThemeMode)
  fun setThemeColorScheme(themeColorScheme: ThemeColorScheme)
  fun toggleGrid()
  fun seekAmountChanged(seconds: Int)
  fun onSeekAmountRowClick()
  fun autoRewindAmountChang(seconds: Int)
  fun onAutoRewindRowClick()
  fun dismissDialog()
  fun getSupport()
  fun suggestIdea()
  fun openBugReport()
  fun openTranslations()
  fun openFaq()
  fun openSupportVoice()
  fun setAutoSleepTimer(checked: Boolean)
  fun setAutoSleepTimerStart(time: LocalTime)
  fun setAutoSleepTimerEnd(time: LocalTime)
  fun toggleAnalytics()
  fun openFolderPicker()
  fun onAppVersionClick()

  fun openDeveloperMenu()

  companion object {
    fun noop() = object : SettingsListener {
      override fun close() {}
      override fun onThemeModeRowClick() {}
      override fun onThemeColorSchemeRowClick() {}
      override fun setThemeMode(themeMode: ThemeMode) {}
      override fun setThemeColorScheme(themeColorScheme: ThemeColorScheme) {}
      override fun toggleGrid() {}
      override fun seekAmountChanged(seconds: Int) {}
      override fun onSeekAmountRowClick() {}
      override fun autoRewindAmountChang(seconds: Int) {}
      override fun onAutoRewindRowClick() {}
      override fun dismissDialog() {}
      override fun getSupport() {}
      override fun suggestIdea() {}
      override fun openBugReport() {}
      override fun openTranslations() {}
      override fun openFaq() {}
      override fun openSupportVoice() {}
      override fun setAutoSleepTimer(checked: Boolean) {}
      override fun setAutoSleepTimerStart(time: LocalTime) {}
      override fun setAutoSleepTimerEnd(time: LocalTime) {}
      override fun toggleAnalytics() {}
      override fun openFolderPicker() {}
      override fun onAppVersionClick() {}
      override fun openDeveloperMenu() {}
    }
  }
}
