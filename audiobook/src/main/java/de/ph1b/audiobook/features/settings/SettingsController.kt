package de.ph1b.audiobook.features.settings

import android.support.v7.widget.SwitchCompat
import android.view.*
import android.widget.TextView
import com.bluelinelabs.conductor.RouterTransaction
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.BaseController
import de.ph1b.audiobook.features.book_playing.SeekDialogFragment
import de.ph1b.audiobook.features.folder_overview.FolderOverviewController
import de.ph1b.audiobook.features.settings.dialogs.AutoRewindDialogFragment
import de.ph1b.audiobook.features.settings.dialogs.SupportDialogFragment
import de.ph1b.audiobook.features.settings.dialogs.ThemePickerDialogFragment
import de.ph1b.audiobook.injection.App
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
        App.component().inject(this)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.settings, container, false)
    }

    override fun onAttach(view: View) {
        setupActionbar(toolbar = view.find(R.id.toolbar),
                upIndicator = R.drawable.close,
                title = activity.getString(R.string.action_settings))

        // audiobook folders
        val audioBookFolder: View = view.find(R.id.audiobookFolder)
        val audioBookFolderTitle: TextView = audioBookFolder.find(R.id.title)
        val audioBookFolderDescription: TextView = audioBookFolder.find(R.id.description)
        audioBookFolderTitle.setText(R.string.pref_root_folder_title)
        audioBookFolderDescription.setText(R.string.pref_root_folder_summary)
        audioBookFolder.setOnClickListener {
            router.pushController(RouterTransaction.with(FolderOverviewController()))
        }

        // theme
        val theme: View = view.find(R.id.theme)
        val themeTitle: TextView = theme.find(R.id.title)
        val themeDescription: TextView = theme.find(R.id.description)
        themeTitle.setText(R.string.pref_theme_title)
        theme.setOnClickListener {
            ThemePickerDialogFragment().show(fragmentManager, ThemePickerDialogFragment.TAG)
        }
        prefs.theme.asObservable()
                .bindToLifeCycle()
                .subscribe { themeDescription.setText(it.nameId) }

        // resume on playback
        val resumePlayback: View = view.find(R.id.resumePlayback)
        val resumePlaybackTitle: TextView = resumePlayback.find(R.id.switchTitle)
        val resumePlaybackDescription: TextView = resumePlayback.find(R.id.switchDescription)
        val resumePlaybackSwitch: SwitchCompat = resumePlayback.find(R.id.switchSetting)
        resumePlaybackTitle.setText(R.string.pref_resume_on_replug)
        resumePlaybackDescription.setText(R.string.pref_resume_on_replug_hint)
        resumePlayback.setOnClickListener { resumePlaybackSwitch.toggle() }
        prefs.resumeOnReplug.asObservable()
                .bindToLifeCycle()
                .subscribe { resumePlaybackSwitch.isChecked = it }
        resumePlaybackSwitch.setOnCheckedChangeListener { compoundButton, checked ->
            prefs.resumeOnReplug.set(checked)
        }

        // pause on interruption
        val pauseOnInterruption = view.find<View>(R.id.pauseOnInterruption)
        val pauseOnInterruptionTitle = pauseOnInterruption.find<TextView>(R.id.switchTitle)
        val pauseOnInterruptionDescription = pauseOnInterruption.find<TextView>(R.id.switchDescription)
        val pauseOnInterruptionSwitch: SwitchCompat = pauseOnInterruption.find(R.id.switchSetting)
        pauseOnInterruptionTitle.setText(R.string.pref_pause_on_can_duck_title)
        pauseOnInterruptionDescription.setText(R.string.pref_pause_on_can_duck_summary)
        prefs.pauseOnTempFocusLoss.asObservable()
                .bindToLifeCycle()
                .subscribe { pauseOnInterruptionSwitch.isChecked = it }
        pauseOnInterruptionSwitch.setOnCheckedChangeListener { compoundButton, checked ->
            prefs.pauseOnTempFocusLoss.set(checked)
        }
        pauseOnInterruption.setOnClickListener { pauseOnInterruptionSwitch.toggle() }

        // skip amount
        val skipAmount = view.find<View>(R.id.skipAmount)
        val skipAmountTitle = skipAmount.find<TextView>(R.id.title)
        val skipAmountDescription = skipAmount.find<TextView>(R.id.description)
        skipAmountTitle.setText(R.string.pref_seek_time)
        skipAmount.setOnClickListener {
            SeekDialogFragment().show(fragmentManager, SeekDialogFragment.TAG)
        }
        prefs.seekTime.asObservable()
                .map { resources.getQuantityString(R.plurals.seconds, it, it) }
                .bindToLifeCycle()
                .subscribe { skipAmountDescription.text = it }

        // auto rewind
        val autoRewind = view.find<View>(R.id.autoRewind)
        val autoRewindTitle: TextView = autoRewind.find(R.id.title)
        val autoRewindDescription: TextView = autoRewind.find(R.id.description)
        autoRewindTitle.setText(R.string.pref_auto_rewind_title)
        autoRewind.setOnClickListener {
            AutoRewindDialogFragment().show(fragmentManager, AutoRewindDialogFragment.TAG)
        }
        prefs.autoRewindAmount.asObservable()
                .map { resources.getQuantityString(R.plurals.seconds, it, it) }
                .bindToLifeCycle()
                .subscribe { autoRewindDescription.text = it }
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