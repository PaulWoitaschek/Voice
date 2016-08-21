package de.ph1b.audiobook.features.settings.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.settings.SettingsSetListener
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.uitools.theme
import kotlinx.android.synthetic.main.dialog_sleep_timer.view.*
import javax.inject.Inject

/**
 * Dialog for setting the sleep time.

 * @author Paul Woitaschek
 */
class SleepDialogFragment : DialogFragment() {

    @Inject lateinit var prefs: PrefsManager

    private lateinit var settingsSetListener: SettingsSetListener

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        settingsSetListener = context as SettingsSetListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        App.component().inject(this)

        // init views
        val v = LayoutInflater.from(context).inflate(R.layout.dialog_sleep_timer, null)
        v.numberPicker.theme()

        val updateText = fun(newVal: Int) {
            v.minuteText.text = context.resources.getQuantityString(R.plurals.pauses_after, newVal, newVal)
        }

        //init number picker
        val oldValue = prefs.sleepTime
        v.numberPicker.minValue = 1
        v.numberPicker.maxValue = 120
        v.numberPicker.value = oldValue
        v.numberPicker.setOnValueChangedListener { picker, oldVal, newVal -> updateText(newVal) }
        updateText(v.numberPicker.value)

        return MaterialDialog.Builder(context)
                .title(R.string.pref_sleep_time)
                .positiveText(R.string.dialog_confirm)
                .negativeText(R.string.dialog_cancel)
                .onPositive { materialDialog, dialogAction ->
                    val newValue = v.numberPicker.value
                    prefs.sleepTime = newValue
                    settingsSetListener.onSettingsSet(newValue != oldValue)
                }
                .customView(v, true)
                .build()
    }


    companion object {

        val TAG: String = SleepDialogFragment::class.java.simpleName
    }
}