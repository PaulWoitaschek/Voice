package de.ph1b.audiobook.features.settings

import android.content.Intent
import android.os.Bundle
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.BaseActivity
import de.ph1b.audiobook.misc.setupActionbar
import kotlinx.android.synthetic.main.toolbar.*

class SettingsActivity : BaseActivity(), SettingsSetListener {


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_settings)

        setupActionbar(toolbar = toolbar)

        if (savedInstanceState == null) {
            fragmentManager.beginTransaction()
                    .replace(R.id.container, SettingsFragment(), SettingsFragment.TAG)
                    .commit()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        val settingsFragment = fragmentManager.findFragmentByTag(SettingsFragment.TAG) as SettingsFragment
        settingsFragment.onActivityResult(requestCode, resultCode, data)
    }

    override fun onSettingsSet(settingsChanged: Boolean) {
        val settingsFragment = fragmentManager.findFragmentByTag(SettingsFragment.TAG) as SettingsFragment
        settingsFragment.onSettingsSet(settingsChanged)
    }
}
