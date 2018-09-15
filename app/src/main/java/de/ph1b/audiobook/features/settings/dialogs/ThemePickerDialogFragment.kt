package de.ph1b.audiobook.features.settings.dialogs

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.uitools.ThemeUtil
import org.koin.android.ext.android.inject

/**
 * Dialog for picking the UI theme.
 */
class ThemePickerDialogFragment : DialogFragment() {

  private val themePref: Pref<ThemeUtil.Theme> by inject(PrefKeys.THEME)

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val oldTheme = themePref.value
    val existingThemes = ThemeUtil.Theme.values()
    val names = existingThemes.map { getString(it.nameId) }

    return MaterialDialog.Builder(context!!)
      .items(*names.toTypedArray())
      .itemsCallbackSingleChoice(existingThemes.indexOf(oldTheme)) { _, _, i, _ ->
        val newTheme = existingThemes[i]
        themePref.value = newTheme
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
