package de.ph1b.audiobook.features.settings

import androidx.annotation.StringRes
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.BaseController
import de.ph1b.audiobook.features.bookPlaying.SeekDialogController
import de.ph1b.audiobook.features.settings.dialogs.AutoRewindDialogController
import de.ph1b.audiobook.features.settings.dialogs.SupportDialogController
import de.ph1b.audiobook.features.settings.dialogs.ThemePickerDialogController
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.tint
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.uitools.ThemeUtil
import kotlinx.android.synthetic.main.settings.*
import javax.inject.Inject
import javax.inject.Named

/**
 * Controller for the user settings
 */
class SettingsController : BaseController() {

  @field:[Inject Named(PrefKeys.THEME)]
  lateinit var themePref: Pref<ThemeUtil.Theme>
  @field:[Inject Named(PrefKeys.RESUME_ON_REPLUG)]
  lateinit var resumeOnReplugPref: Pref<Boolean>
  @field:[Inject Named(PrefKeys.AUTO_REWIND_AMOUNT)]
  lateinit var autoRewindAmountPref: Pref<Int>
  @field:[Inject Named(PrefKeys.SEEK_TIME)]
  lateinit var seekTimePref: Pref<Int>
  @field:[Inject Named(PrefKeys.SAVE_PLAYBACK_SPEED)]
  lateinit var savePlaybackPref: Pref<Boolean>

  init {
    appComponent.inject(this)
  }

  override val layoutRes = R.layout.settings

  override fun onViewCreated() {
    setupToolbar()

    // theme
    setupTextSetting(
      doubleSettingView = theme,
      titleRes = R.string.pref_theme_title
    ) {
      ThemePickerDialogController().showDialog(router)
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

    // skip amount
    setupTextSetting(
      doubleSettingView = skipAmount,
      titleRes = R.string.pref_seek_time
    ) {
      SeekDialogController().showDialog(router)
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
      AutoRewindDialogController().showDialog(router)
    }
    autoRewindAmountPref.stream
      .map { resources!!.getQuantityString(R.plurals.seconds, it, it) }
      .subscribe { autoRewind.setDescription(it) }
      .disposeOnDestroyView()

    // save playback speed
    setupSwitchSetting(
        settingView = savePlaybackSpeed,
        titleRes = R.string.pref_save_playback_speed,
        contentRes = R.string.pref_save_playback_speed_hint,
        pref = savePlaybackPref
    )
  }

  private fun setupToolbar() {
    toolbar.inflateMenu(R.menu.menu_settings)
    toolbar.tint()
    toolbar.setOnMenuItemClickListener {
      if (it.itemId == R.id.action_contribute) {
        SupportDialogController().showDialog(router)
        true
      } else
        false
    }
    toolbar.setNavigationOnClickListener {
      activity.onBackPressed()
    }
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
