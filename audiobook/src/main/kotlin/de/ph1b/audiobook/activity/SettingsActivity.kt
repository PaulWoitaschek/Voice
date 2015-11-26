package de.ph1b.audiobook.activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.Toolbar
import de.ph1b.audiobook.R
import de.ph1b.audiobook.dialog.DonationDialogFragment
import de.ph1b.audiobook.fragment.SettingsFragment
import de.ph1b.audiobook.interfaces.SettingsSetListener

class SettingsActivity : BaseActivity(), DonationDialogFragment.OnDonationClickedListener, SettingsSetListener {


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_settings)

        val toolbar = findViewById(R.id.toolbar)  as Toolbar

        setSupportActionBar(toolbar)

        if (savedInstanceState == null) {
            fragmentManager.beginTransaction().replace(R.id.container, SettingsFragment(), SettingsFragment.TAG).commit()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        val settingsFragment = fragmentManager.findFragmentByTag(SettingsFragment.TAG) as SettingsFragment
        settingsFragment.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDonationClicked(item: String) {
        val settingsFragment = fragmentManager.findFragmentByTag(SettingsFragment.TAG) as SettingsFragment
        settingsFragment.onDonationClicked(item)
    }

    override fun onSettingsSet(settingsChanged: Boolean) {
        val settingsFragment = fragmentManager.findFragmentByTag(SettingsFragment.TAG) as SettingsFragment
        settingsFragment.onSettingsSet(settingsChanged)
    }
}
