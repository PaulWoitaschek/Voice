package de.ph1b.audiobook.dialog.prefs

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


class AutoRewindDialogFragment : DialogFragment() {

    @Inject internal lateinit var prefs: PrefsManager

    private lateinit var textView: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var settingsSetListener: SettingsSetListener

    private val SEEK_BAR_MIN = 0
    private val SEEK_BAR_MAX = 20

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        settingsSetListener = context as SettingsSetListener
    }

    private fun setText(progress: Int) {
        val autoRewindSummary = context.resources.getQuantityString(R.plurals.pref_auto_rewind_summary, progress, progress)
        textView.text = autoRewindSummary
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        App.component().inject(this)

        // view binding
        val customView = LayoutInflater.from(context).inflate(R.layout.dialog_amount_chooser, null)
        textView = customView.findViewById(R.id.textView) as TextView
        seekBar = customView.findViewById(R.id.seekBar) as SeekBar

        val oldRewindAmount = prefs.autoRewindAmount
        seekBar.max = SEEK_BAR_MAX - SEEK_BAR_MIN
        seekBar.progress = oldRewindAmount - SEEK_BAR_MIN
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                setText(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })

        // text
        setText(seekBar.progress)

        return MaterialDialog.Builder(context)
                .title(R.string.pref_auto_rewind_title)
                .customView(customView, true)
                .positiveText(R.string.dialog_confirm)
                .negativeText(R.string.dialog_cancel)
                .onPositive { materialDialog, dialogAction ->
                    val newRewindAmount = seekBar.progress + SEEK_BAR_MIN
                    prefs.autoRewindAmount = newRewindAmount
                    settingsSetListener.onSettingsSet(oldRewindAmount != newRewindAmount)
                }
                .build()
    }

    companion object {
        val TAG = AutoRewindDialogFragment::class.java.simpleName
    }
}
