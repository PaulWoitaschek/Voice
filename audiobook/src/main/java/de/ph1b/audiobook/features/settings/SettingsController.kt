package de.ph1b.audiobook.features.settings

import android.view.*
import com.bluelinelabs.conductor.RouterTransaction
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.BaseController
import de.ph1b.audiobook.features.book_playing.SeekDialogFragment
import de.ph1b.audiobook.features.folder_overview.FolderOverviewController
import de.ph1b.audiobook.features.settings.dialogs.AutoRewindDialogFragment
import de.ph1b.audiobook.features.settings.dialogs.SupportDialogFragment
import de.ph1b.audiobook.features.settings.dialogs.ThemePickerDialogFragment
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.setupActionbar
import de.ph1b.audiobook.persistence.PrefsManager
import kotlinx.android.synthetic.main.setting_row_double.view.*
import kotlinx.android.synthetic.main.setting_row_switch.view.*
import kotlinx.android.synthetic.main.settings.view.*
import javax.inject.Inject

/**
 * TODO
 *
 * @author Paul Woitaschek
 */
class SettingsController : BaseController() {

    @Inject lateinit var prefs: PrefsManager

    init {
        App.component().inject(this)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.settings, container, false)
    }

    override fun onAttach(view: View) {
        setupActionbar(toolbar = view.toolbar,
                upIndicator = R.drawable.close,
                title = activity.getString(R.string.action_settings))

        // audiobook folders
        view.audiobookFolder.title.setText(R.string.pref_root_folder_title)
        view.audiobookFolder.description.setText(R.string.pref_root_folder_summary)
        view.audiobookFolder.setOnClickListener {
            router.pushController(RouterTransaction.with(FolderOverviewController()))
        }

        // theme
        view.theme.title.setText(R.string.pref_theme_title)
        view.theme.setOnClickListener {
            ThemePickerDialogFragment().show(fragmentManager, ThemePickerDialogFragment.TAG)
        }
        prefs.theme.asObservable()
                .bindToLifeCycle()
                .subscribe { view.theme.description.setText(it.nameId) }

        // resume on playback
        view.resumePlayback.switchTitle.setText(R.string.pref_resume_on_replug)
        view.resumePlayback.switchDescription.setText(R.string.pref_resume_on_replug_hint)
        view.resumePlayback.setOnClickListener { it.switchSetting.toggle() }
        prefs.resumeOnReplug.asObservable()
                .bindToLifeCycle()
                .subscribe { view.resumePlayback.switchSetting.isChecked = it }
        view.resumePlayback.switchSetting.setOnCheckedChangeListener { compoundButton, checked ->
            prefs.resumeOnReplug.set(checked)
        }

        // pause on interruption
        view.pauseOnInterruption.switchTitle.setText(R.string.pref_pause_on_can_duck_title)
        view.pauseOnInterruption.switchDescription.setText(R.string.pref_pause_on_can_duck_summary)
        prefs.pauseOnTempFocusLoss.asObservable()
                .bindToLifeCycle()
                .subscribe { view.pauseOnInterruption.switchSetting.isChecked = it }
        view.pauseOnInterruption.switchSetting.setOnCheckedChangeListener { compoundButton, checked ->
            prefs.pauseOnTempFocusLoss.set(checked)
        }
        view.pauseOnInterruption.setOnClickListener { it.switchSetting.toggle() }

        // skip amount
        view.skipAmount.title.setText(R.string.pref_seek_time)
        view.skipAmount.setOnClickListener {
            SeekDialogFragment().show(fragmentManager, SeekDialogFragment.TAG)
        }
        prefs.seekTime.asObservable()
                .map { resources.getQuantityString(R.plurals.seconds, it, it) }
                .bindToLifeCycle()
                .subscribe { view.skipAmount.description.text = it }

        // auto rewind
        view.autoRewind.title.setText(R.string.pref_auto_rewind_title)
        view.autoRewind.setOnClickListener {
            AutoRewindDialogFragment().show(fragmentManager, AutoRewindDialogFragment.TAG)
        }
        prefs.autoRewindAmount.asObservable()
                .map { resources.getQuantityString(R.plurals.seconds, it, it) }
                .bindToLifeCycle()
                .subscribe { view.autoRewind.description.text = it }
    }


    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_settings, menu)
    }

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