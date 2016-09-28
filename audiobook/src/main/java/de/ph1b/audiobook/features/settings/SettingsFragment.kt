package de.ph1b.audiobook.features.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.preference.Preference
import android.preference.PreferenceFragment
import android.support.v7.app.AppCompatDelegate
import android.view.*
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.BaseActivity
import de.ph1b.audiobook.features.book_playing.SeekDialogFragment
import de.ph1b.audiobook.features.folder_overview.FolderOverviewActivity
import de.ph1b.audiobook.features.settings.dialogs.AutoRewindDialogFragment
import de.ph1b.audiobook.features.settings.dialogs.SupportDialogFragment
import de.ph1b.audiobook.features.settings.dialogs.ThemePickerDialogFragment
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.persistence.PrefsManager
import rx.subscriptions.CompositeSubscription
import javax.inject.Inject

class SettingsFragment : PreferenceFragment(), SettingsSetListener {

    @Inject lateinit var prefs: PrefsManager

    private val handler = Handler()

    private lateinit var themePreference: Preference
    private lateinit var googlePreference: Preference
    private lateinit var seekPreference: Preference
    private lateinit var autoRewindPreference: Preference
    private var onStartSubscriptions: CompositeSubscription? = null
    private lateinit var hostingActivity: BaseActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        updateValues()

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.component().inject(this)

        addPreferencesFromResource(R.xml.preferences)

        setHasOptionsMenu(true)

        // seek pref
        seekPreference = findPreference(getString(R.string.pref_key_seek_time))
        seekPreference.setOnPreferenceClickListener {
            SeekDialogFragment().show(hostingActivity.supportFragmentManager, SeekDialogFragment.TAG)
            true
        }

        // auto rewind pref
        autoRewindPreference = findPreference(getString(R.string.pref_key_auto_rewind))
        autoRewindPreference.setOnPreferenceClickListener {
            AutoRewindDialogFragment().show(hostingActivity.supportFragmentManager, AutoRewindDialogFragment.TAG)
            true
        }

        // folder pref
        val folderPreference = findPreference(getString(R.string.pref_key_audiobook_folders))
        folderPreference.setOnPreferenceClickListener {
            startActivity(Intent(hostingActivity, FolderOverviewActivity::class.java))
            true
        }

        // theme pref
        themePreference = findPreference(getString(R.string.pref_key_theme))
        themePreference.setOnPreferenceClickListener {
            ThemePickerDialogFragment().show(hostingActivity.supportFragmentManager, ThemePickerDialogFragment.TAG)
            true
        }

        googlePreference = findPreference(getString(R.string.pref_key_google_account))
        googlePreference.setOnPreferenceClickListener {
//            ThemePickerDialogFragment().show(hostingActivity.supportFragmentManager, ThemePickerDialogFragment.TAG)
            true
        }
    }

    override fun onStart() {
        super.onStart()

        onStartSubscriptions = CompositeSubscription().apply {

            add(prefs.autoRewindAmount.asObservable()
                    .map { resources.getQuantityString(R.plurals.seconds, it, it) }
                    .subscribe { autoRewindPreference.summary = it })

            add(prefs.seekTime.asObservable()
                    .map { resources.getQuantityString(R.plurals.seconds, it, it) }
                    .subscribe { seekPreference.summary = it })
        }
    }

    override fun onStop() {
        onStartSubscriptions!!.unsubscribe()

        super.onStop()
    }

    private fun updateValues() {
        val theme = prefs.theme.get()!!
        themePreference.setSummary(theme.nameId)
    }

    @Suppress("OverridingDeprecatedMember")
    override fun onAttach(activity: Activity) {
        @Suppress("DEPRECATION")
        super.onAttach(activity)

        hostingActivity = activity as BaseActivity
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_settings, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_contribute -> {
                SupportDialogFragment().show(hostingActivity.supportFragmentManager, SupportDialogFragment.TAG)
                return true
            }
            else -> return false
        }
    }

    override fun onSettingsSet(settingsChanged: Boolean) {
        if (settingsChanged) {
            updateValues()
            AppCompatDelegate.setDefaultNightMode(prefs.theme.get()!!.nightMode)
            // must post so dialog can correctly destroy itself
            handler.post { hostingActivity.recreate() }
        }
    }

    companion object {
        val TAG: String = SettingsFragment::class.java.simpleName
    }
}