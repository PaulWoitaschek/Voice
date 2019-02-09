package de.ph1b.audiobook.features.settings.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.DialogController
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.uitools.ThemeUtil
import javax.inject.Inject
import javax.inject.Named

class ThemePickerDialogController : DialogController() {

  @field:[Inject Named(PrefKeys.THEME)]
  lateinit var themePref: Pref<ThemeUtil.Theme>

  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    appComponent.inject(this)

    val oldTheme = themePref.value
    val existingThemes = ThemeUtil.Theme.values()
    val names = existingThemes.map { activity!!.getString(it.nameId) }

    return MaterialDialog(activity!!).apply {
      listItemsSingleChoice(items = names, initialSelection = existingThemes.indexOf(oldTheme)) { _, index, _ ->
        val newTheme = existingThemes[index]
        themePref.value = newTheme
        val delegate = (activity!! as AppCompatActivity).delegate
        delegate.setLocalNightMode(newTheme.nightMode)
      }
      positiveButton(R.string.dialog_confirm)
      negativeButton(R.string.dialog_cancel)
      title(R.string.pref_theme_title)
    }
  }
}
