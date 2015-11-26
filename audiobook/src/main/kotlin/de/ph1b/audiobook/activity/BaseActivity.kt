package de.ph1b.audiobook.activity

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.util.TypedValue
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.service.BookReaderService
import javax.inject.Inject

/**
 * Base class for all Activities which checks in onResume, if the storage
 * is mounted. Shuts down service if not.
 */
abstract class BaseActivity : AppCompatActivity() {

    @Inject internal lateinit var prefsManager: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        App.getComponent().inject(this)
        setTheme(prefsManager.theme.themeId)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (!storageMounted()) {
            val serviceIntent = Intent(this, BookReaderService::class.java)
            stopService(serviceIntent)

            val i = Intent(this, NoExternalStorageActivity::class.java)
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(Intent(i))
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
