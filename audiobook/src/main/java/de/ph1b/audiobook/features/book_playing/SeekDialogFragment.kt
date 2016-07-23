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
import de.ph1b.audiobook.persistence.PrefsManager
import kotlinx.android.synthetic.main.dialog_amount_chooser.view.*
import javax.inject.Inject

class SeekDialogFragment : DialogFragment() {

    private val SEEK_BAR_MIN = 10
    private val SEEK_BAR_MAX = 60

    private lateinit var settingsSetListener: SettingsSetListener

    @Inject lateinit internal var prefs: PrefsManager

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        settingsSetListener = context as SettingsSetListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        App.component().inject(this)

        // find views
        val v = LayoutInflater.from(context).inflate(R.layout.dialog_amount_chooser, null)

        // init
        val oldSeekTime = prefs.seekTime.toBlocking().first()
        v.seekBar.max = SEEK_BAR_MAX - SEEK_BAR_MIN
        v.seekBar.onProgressChanged {
            val value = it + SEEK_BAR_MIN
            v.textView.text = context.resources.getQuantityString(R.plurals.seconds, value, value)
        }
        v.seekBar.progress = oldSeekTime - SEEK_BAR_MIN

        return MaterialDialog.Builder(context)
                .title(R.string.pref_seek_time)
                .customView(v, true)
                .positiveText(R.string.dialog_confirm)
                .negativeText(R.string.dialog_cancel)
                .onPositive { materialDialog, dialogAction ->
                    val newSeekTime = v.seekBar.progress + SEEK_BAR_MIN
                    prefs.setSeekTime(newSeekTime)
                    settingsSetListener.onSettingsSet(oldSeekTime != newSeekTime)
                }.build()
    }


    companion object {

        val TAG: String = SeekDialogFragment::class.java.simpleName
    }
}
