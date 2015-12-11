package de.ph1b.audiobook.activity

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.support.annotation.CallSuper
import android.util.TypedValue
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.BookReaderService
import nucleus.presenter.Presenter
import nucleus.view.NucleusAppCompatActivity
import rx.subscriptions.CompositeSubscription

/**
 * Base class for all MVP Activities to inherit from.
 *
 * @author Paul Woitaschek
 */
abstract class NucleusBaseActivity <P : Presenter<out Any>> : NucleusAppCompatActivity<P> () {

    private lateinit var prefsManager: PrefsManager
    private var onResumeSubscriptions: CompositeSubscription? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        prefsManager = App.component().prefsManager
        setTheme(prefsManager.theme.themeId)
        super.onCreate(savedInstanceState)
    }


    @CallSuper
  open  fun onResume(subscription: CompositeSubscription) {

    }

    override fun onPause() {
        super.onPause()

        onResumeSubscriptions!!.unsubscribe()
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

        onResumeSubscriptions = CompositeSubscription()
        onResume(onResumeSubscriptions!!)
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