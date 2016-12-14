package de.ph1b.audiobook.features.settings.dialogs

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.DialogFragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.uitools.ThemeUtil
import javax.inject.Inject

/**
 * Dialog for picking the UI theme.
 *
 * @author Paul Woitaschek
 */
class ThemePickerDialogFragment : DialogFragment() {

  @Inject lateinit var prefsManager: PrefsManager

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    App.component.inject(this)

    val oldTheme = prefsManager.theme.get()!!
    val existingThemes = ThemeUtil.Theme.values()
    val names = existingThemes.map { getString(it.nameId) }

    return MaterialDialog.Builder(context)
      .items(*names.toTypedArray())
      .itemsCallbackSingleChoice(existingThemes.indexOf(oldTheme)) { materialDialog, view, i, charSequence ->
        val newTheme = existingThemes[i]
        prefsManager.theme.set(newTheme)
        AppCompatDelegate.setDefaultNightMode(newTheme.nightMode)

        // use post so the dialog can close correctly
        Handler().post {
          (activity as AppCompatActivity).delegate.applyDayNight()
        }
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
