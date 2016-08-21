package de.ph1b.audiobook.features.settings.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.settings.SettingsSetListener
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.uitools.ThemeUtil
import java.util.*
import javax.inject.Inject

/**
 * Dialog for picking the UI theme.

 * @author Paul Woitaschek
 */
class ThemePickerDialogFragment : DialogFragment() {

    @Inject lateinit var prefsManager: PrefsManager

    private lateinit var settingsSetListener: SettingsSetListener

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        settingsSetListener = context as SettingsSetListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        App.component().inject(this)

        val oldTheme = prefsManager.theme.get()!!
        val existingThemes = ThemeUtil.Theme.values()
        val names = ArrayList<String>(existingThemes.size)
        for (t in existingThemes) {
            names.add(getString(t.nameId))
        }

        return MaterialDialog.Builder(context)
                .items(*names.toArray<CharSequence>(arrayOfNulls<CharSequence>(names.size)))
                .itemsCallbackSingleChoice(existingThemes.indexOf(oldTheme)) { materialDialog, view, i, charSequence ->
                    val newTheme = existingThemes[i]
                    prefsManager.theme.set(newTheme)
                    settingsSetListener.onSettingsSet(newTheme != oldTheme)
                    true
                }
                .positiveText(R.string.dialog_confirm)
                .negativeText(R.string.dialog_cancel)
                .title(R.string.pref_theme_title)
                .build()
    }

    companion object {
        val TAG: String = ThemePickerDialogFragment::class.java.simpleName
    }
}
