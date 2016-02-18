/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Material Audiobook Player. If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.preference.Preference
import android.preference.PreferenceFragment
import android.view.*
import android.widget.Toast
import d
import de.ph1b.audiobook.R
import de.ph1b.audiobook.activity.BaseActivity
import de.ph1b.audiobook.dialog.DonationDialogFragment
import de.ph1b.audiobook.dialog.SeekDialogFragment
import de.ph1b.audiobook.dialog.SupportDialogFragment
import de.ph1b.audiobook.dialog.prefs.AutoRewindDialogFragment
import de.ph1b.audiobook.dialog.prefs.SleepDialogFragment
import de.ph1b.audiobook.dialog.prefs.ThemePickerDialogFragment
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.interfaces.SettingsSetListener
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.vendinghelper.IabHelper
import de.ph1b.audiobook.view.FolderOverviewActivity
import rx.subscriptions.CompositeSubscription

import javax.inject.Inject

class SettingsFragment : PreferenceFragment(), DonationDialogFragment.OnDonationClickedListener, SettingsSetListener {

    @Inject internal lateinit var prefs: PrefsManager

    private val handler = Handler()

    private lateinit var themePreference: Preference
    private lateinit var sleepPreference: Preference
    private lateinit var seekPreference: Preference
    private lateinit var autoRewindPreference: Preference
    private var onStartSubscriptions: CompositeSubscription? = null
    private var donationAvailable = false
    private lateinit var iabHelper: IabHelper
    private lateinit var hostingActivity: BaseActivity

    override fun onDestroy() {
        super.onDestroy()

        iabHelper.dispose()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        hostingActivity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        updateValues()

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.component().inject(this)

        addPreferencesFromResource(R.xml.preferences)

        setHasOptionsMenu(true)

        iabHelper = IabHelper(activity, "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApfo7lNYf9Mh" + "GiHAZO8iG/LX3SDGg7Gv7s41FEf08rxCuIuE+6QdQ0u+yZEoirislWV7jMqHY3XlyJMrH+/nKqrtYgw" + "qnFtwuwckS/5R+0dtSKL4F/aVm6a3p00BtCjqe7tXrEg90gpVk59p5qr1cOnOAAc/xmerFG9VCv8QHw" + "I9arlShCcXz7eTKemxjkHMO3dTkTKDjYZMIozr0t9qTvTxPz1aV6TWAGs5E6Dt7UF78pntgG9bMwmIgL" + "N6fOYuBaKd8IxA3iQ5IhWGVB8WG65Ax+u0RXsx0r8BC53JQq91lItka7b1OeBe6uPHeqk8IQWY0l57AW" + "fjZOFlNyWQB4QIDAQAB")
        iabHelper.startSetup { donationAvailable = it.isSuccess }

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

        // sleep pref
        sleepPreference = findPreference(getString(R.string.pref_key_sleep_time))
        sleepPreference.setOnPreferenceClickListener {
            SleepDialogFragment().show(hostingActivity.supportFragmentManager, SleepDialogFragment.TAG)
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
    }

    override fun onStart() {
        super.onStart()

        onStartSubscriptions = CompositeSubscription().apply {

            add(prefs.autoRewindAmount
                    .map { resources.getQuantityString(R.plurals.seconds, it, it) }
                    .subscribe { autoRewindPreference.summary = it })

            add(prefs.seekTime
                    .map { resources.getQuantityString(R.plurals.seconds, it, it) }
                    .subscribe { seekPreference.summary = it })
        }
    }

    override fun onStop() {
        onStartSubscriptions!!.unsubscribe()

        super.onStop()
    }

    private fun updateValues() {
        val theme = prefs.theme
        themePreference.setSummary(theme.nameId)

        val sleepAmount = prefs.sleepTime
        val sleepSummary = resources.getQuantityString(R.plurals.minutes, sleepAmount, sleepAmount)
        sleepPreference.summary = sleepSummary
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

    override fun onDonationClicked(item: String) {
        d { "onDonationClicked with item=$item and donationAvailable=$donationAvailable" }
        if (donationAvailable) {
            iabHelper.launchPurchaseFlow(hostingActivity, item
            ) { result ->
                val message: String
                if (result.isSuccess) {
                    message = getString(R.string.donation_worked_thanks)
                } else {
                    message = "${getString(R.string.donation_not_worked)}:\n${result.message}"
                }
                Toast.makeText(hostingActivity, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        iabHelper.handleActivityResult(requestCode, resultCode, data)
    }

    override fun onSettingsSet(settingsChanged: Boolean) {
        if (settingsChanged) {
            updateValues()
            handler.post { hostingActivity.recreateIfThemeChanged() }
        }
    }

    companion object {

        val TAG = SettingsFragment::class.java.simpleName
    }
}