package de.ph1b.audiobook.features.settings

import android.support.annotation.StringRes
import android.widget.TextView
import com.f2prateek.rx.preferences.Preference
import de.ph1b.audiobook.R
import de.ph1b.audiobook.databinding.SettingRowDoubleBinding
import de.ph1b.audiobook.databinding.SettingRowSwitchBinding
import de.ph1b.audiobook.databinding.SettingsBinding
import de.ph1b.audiobook.features.BaseController
import de.ph1b.audiobook.features.bookPlaying.SeekDialogFragment
import de.ph1b.audiobook.features.folderOverview.FolderOverviewController
import de.ph1b.audiobook.features.settings.dialogs.AutoRewindDialogFragment
import de.ph1b.audiobook.features.settings.dialogs.SupportDialogFragment
import de.ph1b.audiobook.features.settings.dialogs.ThemePickerDialogFragment
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.asV2Observable
import de.ph1b.audiobook.misc.conductor.asTransaction
import de.ph1b.audiobook.persistence.PrefsManager
import javax.inject.Inject

/**
 * Controller for the user settings
 */
class SettingsController : BaseController<SettingsBinding>() {

  @Inject lateinit var prefs: PrefsManager

  init {
    App.component.inject(this)
  }

  override val layoutRes = R.layout.settings

  override fun onBindingCreated(binding: SettingsBinding) {
    setupToolbar()

    // audio book folders
    setupTextSetting(
        binding = binding.audiobookFolder,
        titleRes = R.string.pref_root_folder_title,
        contentRes = R.string.pref_root_folder_summary
    ) {
      val transaction = FolderOverviewController().asTransaction()
      router.pushController(transaction)
    }

    // theme
    val themeDescription = setupTextSetting(
        binding = binding.theme,
        titleRes = R.string.pref_theme_title
    ) {
      ThemePickerDialogFragment().show(fragmentManager, ThemePickerDialogFragment.TAG)
    }
    prefs.theme.asV2Observable()
        .bindToLifeCycle()
        .subscribe { themeDescription.setText(it.nameId) }

    // resume on playback
    setupSwitchSetting(
        binding = binding.resumePlayback,
        titleRes = R.string.pref_resume_on_replug,
        contentRes = R.string.pref_resume_on_replug_hint,
        pref = prefs.resumeOnReplug
    )

    // resume on playback
    setupSwitchSetting(
        binding = binding.resumeAfterCall,
        titleRes = R.string.pref_resume_after_call,
        contentRes = R.string.pref_resume_after_call_hint,
        pref = prefs.resumeAfterCall
    )

    // skip amount
    val skipAmountDescription = setupTextSetting(
        binding = binding.skipAmount,
        titleRes = R.string.pref_seek_time
    ) {
      SeekDialogFragment().show(fragmentManager, SeekDialogFragment.TAG)
    }
    prefs.seekTime.asV2Observable()
        .map { resources!!.getQuantityString(R.plurals.seconds, it, it) }
        .bindToLifeCycle()
        .subscribe { skipAmountDescription.text = it }

    // auto rewind
    val autoRewindDescription = setupTextSetting(
        binding = binding.autoRewind,
        titleRes = R.string.pref_auto_rewind_title
    ) {
      AutoRewindDialogFragment().show(fragmentManager, AutoRewindDialogFragment.TAG)
    }
    prefs.autoRewindAmount.asV2Observable()
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
      crossinline onClick: () -> Unit): TextView {
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
      pref: Preference<Boolean>) {
    binding.switchTitle.setText(titleRes)
    binding.switchDescription.setText(contentRes)

    binding.switchSetting.setOnCheckedChangeListener { _, checked ->
      pref.set(checked)
    }
    pref.asV2Observable()
        .bindToLifeCycle()
        .subscribe { binding.switchSetting.isChecked = it }

    binding.root.setOnClickListener { binding.switchSetting.toggle() }
  }
}
