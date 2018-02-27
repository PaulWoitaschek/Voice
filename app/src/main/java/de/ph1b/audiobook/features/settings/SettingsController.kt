package de.ph1b.audiobook.features.settings

import android.support.annotation.StringRes
import android.widget.TextView
import de.ph1b.audiobook.R
import de.ph1b.audiobook.databinding.SettingRowDoubleBinding
import de.ph1b.audiobook.databinding.SettingRowSwitchBinding
import de.ph1b.audiobook.databinding.SettingsBinding
import de.ph1b.audiobook.features.BaseController
import de.ph1b.audiobook.features.bookPlaying.SeekDialogFragment
import de.ph1b.audiobook.features.settings.dialogs.AutoRewindDialogFragment
import de.ph1b.audiobook.features.settings.dialogs.SupportDialogFragment
import de.ph1b.audiobook.features.settings.dialogs.ThemePickerDialogFragment
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.uitools.ThemeUtil
import javax.inject.Inject
import javax.inject.Named

/**
 * Controller for the user settings
 */
class SettingsController : BaseController<SettingsBinding>() {

  @field:[Inject Named(PrefKeys.THEME)]
  lateinit var themePref: Pref<ThemeUtil.Theme>
  @field:[Inject Named(PrefKeys.RESUME_ON_REPLUG)]
  lateinit var resumeOnReplugPref: Pref<Boolean>
  @field:[Inject Named(PrefKeys.RESUME_AFTER_CALL)]
  lateinit var resumeAfterCallPref: Pref<Boolean>
  @field:[Inject Named(PrefKeys.AUTO_REWIND_AMOUNT)]
  lateinit var autoRewindAmountPref: Pref<Int>
  @field:[Inject Named(PrefKeys.SEEK_TIME)]
  lateinit var seekTimePref: Pref<Int>

  init {
    App.component.inject(this)
  }

  override val layoutRes = R.layout.settings

  override fun onBindingCreated(binding: SettingsBinding) {
    setupToolbar()

    // theme
    val themeDescription = setupTextSetting(
      binding = binding.theme!!,
      titleRes = R.string.pref_theme_title
    ) {
      ThemePickerDialogFragment().show(fragmentManager, ThemePickerDialogFragment.TAG)
    }
    themePref.stream
      .bindToLifeCycle()
      .subscribe { themeDescription.setText(it.nameId) }

    // resume on playback
    setupSwitchSetting(
      binding = binding.resumePlayback!!,
      titleRes = R.string.pref_resume_on_replug,
      contentRes = R.string.pref_resume_on_replug_hint,
      pref = resumeOnReplugPref
    )

    // resume on playback
    setupSwitchSetting(
      binding = binding.resumeAfterCall!!,
      titleRes = R.string.pref_resume_after_call,
      contentRes = R.string.pref_resume_after_call_hint,
      pref = resumeAfterCallPref
    )

    // skip amount
    val skipAmountDescription = setupTextSetting(
      binding = binding.skipAmount!!,
      titleRes = R.string.pref_seek_time
    ) {
      SeekDialogFragment().show(fragmentManager, SeekDialogFragment.TAG)
    }
    seekTimePref.stream
      .map { resources!!.getQuantityString(R.plurals.seconds, it, it) }
      .bindToLifeCycle()
      .subscribe { skipAmountDescription.text = it }

    // auto rewind
    val autoRewindDescription = setupTextSetting(
      binding = binding.autoRewind!!,
      titleRes = R.string.pref_auto_rewind_title
    ) {
      AutoRewindDialogFragment().show(fragmentManager, AutoRewindDialogFragment.TAG)
    }
    autoRewindAmountPref.stream
      .map { resources!!.getQuantityString(R.plurals.seconds, it, it) }
      .bindToLifeCycle()
      .subscribe { autoRewindDescription.text = it }
  }

  private fun setupToolbar() {
    binding.toolbar.setNavigationIcon(R.drawable.close)
    binding.toolbar.inflateMenu(R.menu.menu_settings)
    binding.toolbar.setOnMenuItemClickListener {
      if (it.itemId == R.id.action_contribute) {
        SupportDialogFragment().show(fragmentManager, SupportDialogFragment.TAG)
        true
      } else
        false
    }
    binding.toolbar.setNavigationOnClickListener {
      activity.onBackPressed()
    }
    binding.toolbar.title = getString(R.string.action_settings)
  }

  private inline fun setupTextSetting(
    binding: SettingRowDoubleBinding, @StringRes titleRes: Int, @StringRes contentRes: Int? = null,
    crossinline onClick: () -> Unit
  ): TextView {
    val title: TextView = binding.title
    val description: TextView = binding.description
    if (contentRes != null) description.setText(contentRes)
    title.setText(titleRes)
    binding.root.setOnClickListener {
      onClick()
    }
    return description
  }

  private fun setupSwitchSetting(
    binding: SettingRowSwitchBinding, @StringRes titleRes: Int, @StringRes contentRes: Int,
    pref: Pref<Boolean>
  ) {
    binding.switchTitle.setText(titleRes)
    binding.switchDescription.setText(contentRes)

    binding.switchSetting.setOnCheckedChangeListener { _, checked ->
      pref.value = checked
    }
    pref.stream
      .bindToLifeCycle()
      .subscribe { binding.switchSetting.isChecked = it }

    binding.root.setOnClickListener { binding.switchSetting.toggle() }
  }
}
