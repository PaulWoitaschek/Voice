package de.ph1b.audiobook.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.widget.SeekBar
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.interfaces.SettingsSetListener
import de.ph1b.audiobook.persistence.PrefsManager
import javax.inject.Inject

class SeekDialogFragment : DialogFragment() {

    private val SEEK_BAR_MIN = 10
    private val SEEK_BAR_MAX = 60

    private lateinit var seekBar: SeekBar
    private lateinit var textView: TextView
    private lateinit var settingsSetListener: SettingsSetListener

    @Inject lateinit internal var prefs: PrefsManager


    override fun onAttach(context: Context?) {
        super.onAttach(context)

        settingsSetListener = context as SettingsSetListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        App.component().inject(this)

        // find views
        val customView = LayoutInflater.from(context).inflate(R.layout.dialog_amount_chooser, null)
        seekBar = customView.findViewById(R.id.seekBar) as SeekBar
        textView = customView.findViewById(R.id.textView) as TextView

        // init
        val oldSeekTime = prefs.seekTime
        seekBar.max = SEEK_BAR_MAX - SEEK_BAR_MIN
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val value = progress + SEEK_BAR_MIN
                textView.text = context.resources.getQuantityString(R.plurals.seconds, value, value)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })
        seekBar.progress = oldSeekTime - SEEK_BAR_MIN

        return MaterialDialog.Builder(context)
                .title(R.string.pref_seek_time)
                .customView(customView, true)
                .positiveText(R.string.dialog_confirm)
                .negativeText(R.string.dialog_cancel)
                .onPositive { materialDialog, dialogAction ->
                    val newSeekTime = seekBar.progress + SEEK_BAR_MIN
                    prefs.seekTime = newSeekTime
                    settingsSetListener.onSettingsSet(oldSeekTime != newSeekTime)
                }.build()
    }


    companion object {

        val TAG = SeekDialogFragment::class.java.simpleName
    }
}
