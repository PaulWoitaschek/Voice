package de.ph1b.audiobook.features.settings

import android.support.annotation.IdRes
import android.support.annotation.StringRes
import android.support.v7.widget.SwitchCompat
import android.view.*
import android.widget.TextView
import com.f2prateek.rx.preferences.Preference
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.BaseController
import de.ph1b.audiobook.features.bookPlaying.SeekDialogFragment
import de.ph1b.audiobook.features.folderOverview.FolderOverviewController
import de.ph1b.audiobook.features.settings.dialogs.AutoRewindDialogFragment
import de.ph1b.audiobook.features.settings.dialogs.SupportDialogFragment
import de.ph1b.audiobook.features.settings.dialogs.ThemePickerDialogFragment
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.asTransaction
import de.ph1b.audiobook.misc.asV2Observable
import de.ph1b.audiobook.misc.find
import de.ph1b.audiobook.misc.setupActionbar
import de.ph1b.audiobook.persistence.PrefsManager
import javax.inject.Inject

/**
 * Controller for the user settings
 *
 * @author Paul Woitaschek
 */
class SettingsController : BaseController() {

  @Inject lateinit var prefs: PrefsManager

  init {
    App.component.inject(this)
    setHasOptionsMenu(true)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View = inflater.inflate(R.layout.settings, container, false)

  override fun onAttach(view: View) {
    setupActionbar(
        toolbar = view.find(R.id.toolbar),
        upIndicator = R.drawable.close,
        title = activity.getString(R.string.action_settings)
    )

    // audio book folders
    setupTextSetting(R.id.audiobookFolder, R.string.pref_root_folder_title, R.string.pref_root_folder_summary) {
      val transaction = FolderOverviewController().asTransaction()
      router.pushController(transaction)
    }

    // theme
    val themeDescription = setupTextSetting(R.id.theme, R.string.pref_theme_title) {
      ThemePickerDialogFragment().show(fragmentManager, ThemePickerDialogFragment.TAG)
    }
    prefs.theme.asV2Observable()
        .bindToLifeCycle()
        .subscribe { themeDescription.setText(it.nameId) }

    // show cover art on lockscreen
    setupSwitchSetting(
        id = R.id.showCoverArtOnLockscreen,
        titleRes = R.string.pref_show_cover_art_on_lockscreen,
        contentRes = R.string.pref_show_cover_art_on_lockscreen_hint,
        pref = prefs.showCoverArtOnLockscreen
    )

    // resume on playback
    setupSwitchSetting(
        id = R.id.resumePlayback,
        titleRes = R.string.pref_resume_on_replug,
        contentRes = R.string.pref_resume_on_replug_hint,
        pref = prefs.resumeOnReplug
    )

    // resume on playback
    setupSwitchSetting(
        id = R.id.resumeAfterCall,
        titleRes = R.string.pref_resume_after_call,
        contentRes = R.string.pref_resume_after_call_hint,
        pref = prefs.resumeAfterCall
    )

    // pause on interruption
    setupSwitchSetting(
        id = R.id.pauseOnInterruption,
        titleRes = R.string.pref_pause_on_can_duck_title,
        contentRes = R.string.pref_pause_on_can_duck_summary,
        pref = prefs.pauseOnTempFocusLoss
    )

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

  private fun setupSwitchSetting(@IdRes id: Int, @StringRes titleRes: Int, @StringRes contentRes: Int, pref: Preference<Boolean>) {
    val root = view!!.find<View>(id)
    val title = root.find<TextView>(R.id.switchTitle)
    val content = root.find<TextView>(R.id.switchDescription)
    val switch: SwitchCompat = root.find(R.id.switchSetting)

    title.setText(titleRes)
    content.setText(contentRes)

    switch.setOnCheckedChangeListener { _, checked ->
      pref.set(checked)
    }
    pref.asV2Observable()
        .bindToLifeCycle()
        .subscribe { switch.isChecked = it }

    root.setOnClickListener { switch.toggle() }
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) = inflater.inflate(R.menu.menu_settings, menu)

  override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
    R.id.action_contribute -> {
      SupportDialogFragment().show(fragmentManager, SupportDialogFragment.TAG)
      true
    }
    android.R.id.home -> {
      router.popCurrentController()
      true
    }
    else -> false
  }
}