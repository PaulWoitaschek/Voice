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
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.util.TypedValue
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.BookReaderService
import javax.inject.Inject

/**
 * Base class for all Activities which checks in onResume, if the storage
 * is mounted. Shuts down service if not.
 */
abstract class BaseActivity : AppCompatActivity() {

    @Inject internal lateinit var prefsManager: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        App.component().inject(this)
        setTheme(prefsManager.theme.themeId)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (!storageMounted()) {
            val serviceIntent = Intent(this, BookReaderService::class.java)
            stopService(serviceIntent)

            startActivity(Intent(this, NoExternalStorageActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            return
        }
        recreateIfThemeChanged()
    }

    fun recreateIfThemeChanged() {
        val outValue = TypedValue()
        theme.resolveAttribute(R.attr.theme_name, outValue, true)
        val oldThemeName = outValue.string.toString()
        val newName = getString(prefsManager.theme.nameId)

        if (newName != oldThemeName) {
            recreate()
        }
    }

    companion object {
        fun storageMounted(): Boolean {
            return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        }
    }
}
