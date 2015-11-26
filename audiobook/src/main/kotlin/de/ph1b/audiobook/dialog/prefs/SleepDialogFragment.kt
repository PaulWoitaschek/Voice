package de.ph1b.audiobook.dialog.prefs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.widget.NumberPicker
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.interfaces.SettingsSetListener
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.uitools.ThemeUtil
import javax.inject.Inject

/**
 * Dialog for setting the sleep time.

 * @author Paul Woitaschek
 */
class SleepDialogFragment : DialogFragment() {

    @Inject internal lateinit var prefs: PrefsManager

    private lateinit var timeView: TextView
    private lateinit var settingsSetListener: SettingsSetListener

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        settingsSetListener = context as SettingsSetListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        App.getComponent().inject(this)

        // init views
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_sleep_timer, null)
        timeView = view.findViewById(R.id.minute_text) as TextView
        val numberPicker = view.findViewById(R.id.minute) as NumberPicker
        ThemeUtil.theme(numberPicker)

        //init number picker
        val oldValue = prefs.sleepTime
        numberPicker.minValue = 1
        numberPicker.maxValue = 120
        numberPicker.value = oldValue
        numberPicker.setOnValueChangedListener { picker, oldVal, newVal -> updateText(newVal) }
        updateText(numberPicker.value)

        return MaterialDialog.Builder(context)
                .title(R.string.pref_sleep_time)
                .positiveText(R.string.dialog_confirm)
                .negativeText(R.string.dialog_cancel)
                .onPositive { materialDialog, dialogAction ->
                    val newValue = numberPicker.value
                    prefs.sleepTime = newValue
                    settingsSetListener.onSettingsSet(newValue != oldValue)
                }
                .customView(view, true)
                .build()
    }

    private fun updateText(newVal: Int) {
        timeView.text = context.resources.getQuantityString(R.plurals.pauses_after, newVal, newVal)
    }

    companion object {

        val TAG = SleepDialogFragment::class.java.simpleName
    }
}