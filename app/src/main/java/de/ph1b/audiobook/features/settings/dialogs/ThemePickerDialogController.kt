package de.ph1b.audiobook.features.settings.dialogs

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.misc.DialogController
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.uitools.ThemeUtil
import javax.inject.Inject
import javax.inject.Named

class ThemePickerDialogController : DialogController() {

  @field:[Inject Named(PrefKeys.THEME)]
  lateinit var themePref: Pref<ThemeUtil.Theme>

  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    App.component.inject(this)

    val oldTheme = themePref.value
    val existingThemes = ThemeUtil.Theme.values()
    val names = existingThemes.map { activity!!.getString(it.nameId) }

    return MaterialDialog.Builder(activity!!)
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
    val TAG: String = ThemePickerDialogController::class.java.simpleName
  }
}
