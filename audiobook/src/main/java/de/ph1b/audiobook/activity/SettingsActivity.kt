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
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

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
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.Toolbar
import de.ph1b.audiobook.R
import de.ph1b.audiobook.fragment.SettingsFragment
import de.ph1b.audiobook.interfaces.SettingsSetListener

class SettingsActivity : BaseActivity(), SettingsSetListener {


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

    override fun onSettingsSet(settingsChanged: Boolean) {
        val settingsFragment = fragmentManager.findFragmentByTag(SettingsFragment.TAG) as SettingsFragment
        settingsFragment.onSettingsSet(settingsChanged)
    }
}
