package de.ph1b.audiobook.features.settings.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.DialogController
import de.ph1b.audiobook.misc.DialogLayoutContainer
import de.ph1b.audiobook.misc.inflate
import de.ph1b.audiobook.misc.onProgressChanged
import de.ph1b.audiobook.prefs.Pref
import de.ph1b.audiobook.prefs.PrefKeys
import kotlinx.android.synthetic.main.dialog_amount_chooser.*
import javax.inject.Inject
import javax.inject.Named

class AutoRewindDialogController : DialogController() {

  @field:[Inject Named(PrefKeys.AUTO_REWIND_AMOUNT)]
  lateinit var autoRewindAmountPref: Pref<Int>

  @SuppressLint("InflateParams")
  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    appComponent.inject(this)

    val container =
      DialogLayoutContainer(activity!!.layoutInflater.inflate(R.layout.dialog_amount_chooser))

    val oldRewindAmount = autoRewindAmountPref.value
    container.seekBar.max = (MAX - MIN) * FACTOR
    container.seekBar.progress = (oldRewindAmount - MIN) * FACTOR
    container.seekBar.onProgressChanged(initialNotification = true) {
      val progress = it / FACTOR
      val autoRewindSummary = activity!!.resources.getQuantityString(
        R.plurals.pref_auto_rewind_summary,
        progress,
        progress
      )
      container.textView.text = autoRewindSummary
    }

    return MaterialDialog(activity!!).apply {
      title(R.string.pref_auto_rewind_title)
      customView(view = container.containerView, scrollable = true)
      positiveButton(R.string.dialog_confirm) {
        val newRewindAmount = container.seekBar.progress / FACTOR + MIN
        autoRewindAmountPref.value = newRewindAmount
      }
      negativeButton(R.string.dialog_cancel)
    }
  }

  companion object {
    private const val MIN = 0
    private const val MAX = 20
    private const val FACTOR = 10
  }
}
