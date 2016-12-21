package de.ph1b.audiobook.features.settings

import android.support.annotation.IdRes
import android.support.annotation.StringRes
import android.support.v7.widget.SwitchCompat
import android.view.*
import android.widget.TextView
import com.bluelinelabs.conductor.RouterTransaction
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.BaseController
import de.ph1b.audiobook.features.bookPlaying.SeekDialogFragment
import de.ph1b.audiobook.features.folderOverview.FolderOverviewController
import de.ph1b.audiobook.features.settings.dialogs.AutoRewindDialogFragment
import de.ph1b.audiobook.features.settings.dialogs.SupportDialogFragment
import de.ph1b.audiobook.features.settings.dialogs.ThemePickerDialogFragment
import de.ph1b.audiobook.features.tracking.Tracker
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.asV2Observable
import de.ph1b.audiobook.misc.find
import de.ph1b.audiobook.misc.setupActionbar
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.uitools.VerticalChangeHandler
import javax.inject.Inject

/**
 * Controller for the user settings
 *
 * @author Paul Woitaschek
 */
class SettingsController : BaseController() {

  @Inject lateinit var prefs: PrefsManager
  @Inject lateinit var tracker: Tracker

  init {
    App.component.inject(this)
    setHasOptionsMenu(true)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View = inflater.inflate(R.layout.settings, container, false)

  override fun onAttach(view: View) {
    setupActionbar(toolbar = view.find(R.id.toolbar),
      upIndicator = R.drawable.close,
      title = activity.getString(R.string.action_settings))

    // audio book folders
    setupTextSetting(R.id.audiobookFolder, R.string.pref_root_folder_title, R.string.pref_root_folder_summary) {
      val transaction = RouterTransaction.with(FolderOverviewController())
        .pushChangeHandler(VerticalChangeHandler())
        .popChangeHandler(VerticalChangeHandler())
      router.pushController(transaction)
    }

    // theme
    val themeDescription = setupTextSetting(R.id.theme, R.string.pref_theme_title) {
      ThemePickerDialogFragment().show(fragmentManager, ThemePickerDialogFragment.TAG)
    }
    prefs.theme.asV2Observable()
      .bindToLifeCycle()
      .subscribe { themeDescription.setText(it.nameId) }

    // resume on playback
    val resumePlaybackSwitch = setupSwitchSetting(R.id.resumePlayback, R.string.pref_resume_on_replug, R.string.pref_resume_on_replug_hint) {
      if (prefs.resumeOnReplug.get() != it) tracker.resumePlaybackOnHeadset(it)
      prefs.resumeOnReplug.set(it)
    }
    prefs.resumeOnReplug.asV2Observable()
      .bindToLifeCycle()
      .subscribe { resumePlaybackSwitch.isChecked = it }

    // pause on interruption
    val pauseOnInterruptionSwitch = setupSwitchSetting(R.id.pauseOnInterruption, R.string.pref_pause_on_can_duck_title, R.string.pref_pause_on_can_duck_summary) {
      if (prefs.pauseOnTempFocusLoss.get() != it) tracker.pauseOnInterruption(it)
      prefs.pauseOnTempFocusLoss.set(it)
    }
    prefs.pauseOnTempFocusLoss.asV2Observable()
      .bindToLifeCycle()
      .subscribe { pauseOnInterruptionSwitch.isChecked = it }

    // skip amount
    val skipAmountDescription = setupTextSetting(R.id.skipAmount, R.string.pref_seek_time) {
      SeekDialogFragment().show(fragmentManager, SeekDialogFragment.TAG)
    }
    prefs.seekTime.asV2Observable()
      .map { resources!!.getQuantityString(R.plurals.seconds, it, it) }
      .bindToLifeCycle()
      .subscribe { skipAmountDescription.text = it }

    // auto rewind
    val autoRewindDescription = setupTextSetting(R.id.autoRewind, R.string.pref_auto_rewind_title) {
      AutoRewindDialogFragment().show(fragmentManager, AutoRewindDialogFragment.TAG)
    }
    prefs.autoRewindAmount.asV2Observable()
      .map { resources!!.getQuantityString(R.plurals.seconds, it, it) }
      .bindToLifeCycle()
      .subscribe { autoRewindDescription.text = it }

    // analytics
    val analyticsSwitch = setupSwitchSetting(R.id.analytics, R.string.pref_analytic_title, R.string.pref_analytic_content) {
      prefs.analytics.set(it)
      tracker.setEnabled(it)
    }
    prefs.analytics.asV2Observable()
      .bindToLifeCycle()
      .subscribe { analyticsSwitch.isChecked = it }
  }

  private inline fun setupTextSetting(@IdRes id: Int, @StringRes titleRes: Int, @StringRes contentRes: Int? = null, crossinline onClick: () -> Unit): TextView {
    val theme: View = view!!.find(id)
    val title: TextView = theme.find(R.id.title)
    val description: TextView = theme.find(R.id.description)
    if (contentRes != null) description.setText(contentRes)
    title.setText(titleRes)
    theme.setOnClickListener {
      onClick()
    }
    return description
  }

  private inline fun setupSwitchSetting(@IdRes id: Int, @StringRes titleRes: Int, @StringRes contentRes: Int, crossinline onChecked: (Boolean) -> Unit): SwitchCompat {
    val root = view!!.find<View>(id)
    val title = root.find<TextView>(R.id.switchTitle)
    val content = root.find<TextView>(R.id.switchDescription)
    val switch: SwitchCompat = root.find(R.id.switchSetting)
    title.setText(titleRes)
    content.setText(contentRes)
    switch.setOnCheckedChangeListener { compoundButton, checked ->
      onChecked(checked)
    }
    root.setOnClickListener { switch.toggle() }
    return switch
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) = inflater.inflate(R.menu.menu_settings, menu)

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.action_contribute -> {
        SupportDialogFragment().show(fragmentManager, SupportDialogFragment.TAG)
        return true
      }
      android.R.id.home -> {
        router.popCurrentController()
        return true
      }
      else -> return false
    }
  }
}