package de.ph1b.audiobook.features.settings

import androidx.annotation.StringRes
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.BaseController
import de.ph1b.audiobook.features.bookPlaying.SeekDialogFragment
import de.ph1b.audiobook.features.settings.dialogs.AutoRewindDialogFragment
import de.ph1b.audiobook.features.settings.dialogs.SupportDialogFragment
import de.ph1b.audiobook.features.settings.dialogs.ThemePickerDialogFragment
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.uitools.ThemeUtil
import kotlinx.android.synthetic.main.settings.*
import org.koin.standalone.inject

/**
 * Controller for the user settings
 */
class SettingsController : BaseController() {

  private val themePref: Pref<ThemeUtil.Theme> by inject(PrefKeys.THEME)
  private val resumeOnReplugPref: Pref<Boolean> by inject(PrefKeys.RESUME_ON_REPLUG)
  private val resumeAfterCallPref: Pref<Boolean> by inject(PrefKeys.RESUME_AFTER_CALL)
  private val autoRewindAmountPref: Pref<Int> by inject(PrefKeys.AUTO_REWIND_AMOUNT)
  private val seekTimePref: Pref<Int> by inject(PrefKeys.SEEK_TIME)
  private val crashReportEnabledPref: Pref<Boolean> by inject(PrefKeys.CRASH_REPORT_ENABLED)

  override val layoutRes = R.layout.settings

  override fun onViewCreated() {
    setupToolbar()

    // theme
    setupTextSetting(
      doubleSettingView = theme,
      titleRes = R.string.pref_theme_title
    ) {
      ThemePickerDialogFragment().show(fragmentManager, ThemePickerDialogFragment.TAG)
    }
    themePref.stream
      .subscribe { theme.setDescription(it.nameId) }
      .disposeOnDestroyView()

    // resume on playback
    setupSwitchSetting(
      settingView = resumePlayback,
      titleRes = R.string.pref_resume_on_replug,
      contentRes = R.string.pref_resume_on_replug_hint,
      pref = resumeOnReplugPref
    )

    // resume on playback
    setupSwitchSetting(
      settingView = resumeAfterCall,
      titleRes = R.string.pref_resume_after_call,
      contentRes = R.string.pref_resume_after_call_hint,
      pref = resumeAfterCallPref
    )

    setupSwitchSetting(
      settingView = crashReports,
      titleRes = R.string.pref_allow_crash_report_title,
      contentRes = R.string.pref_allow_crash_report_content,
      pref = crashReportEnabledPref
    )

    // skip amount
    setupTextSetting(
      doubleSettingView = skipAmount,
      titleRes = R.string.pref_seek_time
    ) {
      SeekDialogFragment().show(fragmentManager, SeekDialogFragment.TAG)
    }
    seekTimePref.stream
      .map { resources!!.getQuantityString(R.plurals.seconds, it, it) }
      .subscribe { skipAmount.setDescription(it) }
      .disposeOnDestroyView()

    // auto rewind
    setupTextSetting(
      doubleSettingView = autoRewind,
      titleRes = R.string.pref_auto_rewind_title
    ) {
      AutoRewindDialogFragment().show(fragmentManager, AutoRewindDialogFragment.TAG)
    }
    autoRewindAmountPref.stream
      .map { resources!!.getQuantityString(R.plurals.seconds, it, it) }
      .subscribe { autoRewind.setDescription(it) }
      .disposeOnDestroyView()
  }

  private fun setupToolbar() {
    toolbar.setNavigationIcon(R.drawable.close)
    toolbar.inflateMenu(R.menu.menu_settings)
    toolbar.setOnMenuItemClickListener {
      if (it.itemId == R.id.action_contribute) {
        SupportDialogFragment().show(fragmentManager, SupportDialogFragment.TAG)
        true
      } else
        false
    }
    toolbar.setNavigationOnClickListener {
      activity.onBackPressed()
    }
    toolbar.title = getString(R.string.action_settings)
  }

  private inline fun setupTextSetting(
    doubleSettingView: DoubleSettingView,
    @StringRes titleRes: Int,
    @StringRes contentRes: Int? = null,
    crossinline onClick: () -> Unit
  ) {
    doubleSettingView.setTitle(titleRes)
    if (contentRes != null) doubleSettingView.setDescription(contentRes)
    doubleSettingView.setOnClickListener {
      onClick()
    }
  }

  private fun setupSwitchSetting(
    settingView: SwitchSettingView,
    @StringRes titleRes: Int,
    @StringRes contentRes: Int,
    pref: Pref<Boolean>
  ) {
    settingView.setTitle(titleRes)
    settingView.setDescription(contentRes)

    settingView.onCheckedChanged {
      pref.value = it
    }
    pref.stream
      .subscribe { settingView.setChecked(it) }
      .disposeOnDestroyView()

    settingView.setOnClickListener { settingView.toggle() }
  }
}
