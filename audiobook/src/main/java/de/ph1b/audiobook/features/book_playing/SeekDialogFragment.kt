package de.ph1b.audiobook.features.book_playing

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.settings.SettingsSetListener
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.onProgressChanged
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.PrefsManager
import kotlinx.android.synthetic.main.dialog_amount_chooser.view.*
import javax.inject.Inject

class SeekDialogFragment : DialogFragment() {

    private lateinit var settingsSetListener: SettingsSetListener

    @Inject lateinit var prefs: PrefsManager

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        settingsSetListener = context as SettingsSetListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        App.component().inject(this)

        // find views
        val v = LayoutInflater.from(context).inflate(R.layout.dialog_amount_chooser, null)

        // init
        val oldSeekTime = prefs.seekTime.value()
        v.seekBar.max = (MAX - MIN) * FACTOR
        v.seekBar.onProgressChanged(initialNotification = true) {
            val value = it / FACTOR + MIN
            v.textView.text = context.resources.getQuantityString(R.plurals.seconds, value, value)
        }
        v.seekBar.progress = (oldSeekTime - MIN) * FACTOR

        return MaterialDialog.Builder(context)
                .title(R.string.pref_seek_time)
                .customView(v, true)
                .positiveText(R.string.dialog_confirm)
                .negativeText(R.string.dialog_cancel)
                .onPositive { materialDialog, dialogAction ->
                    val newSeekTime = v.seekBar.progress / FACTOR + MIN
                    prefs.seekTime.set(newSeekTime)
                    settingsSetListener.onSettingsSet(oldSeekTime != newSeekTime)
                }.build()
    }

    companion object {
        val TAG: String = SeekDialogFragment::class.java.simpleName

        private val FACTOR = 10
        private val MIN = 3
        private val MAX = 60
    }
}
