package de.ph1b.audiobook.features.settings.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import dagger.android.support.AndroidSupportInjection
import de.ph1b.audiobook.R
import de.ph1b.audiobook.databinding.DialogAmountChooserBinding
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.misc.onProgressChanged
import de.ph1b.audiobook.persistence.pref.Pref
import javax.inject.Inject
import javax.inject.Named

class AutoRewindDialogFragment : DialogFragment() {

  @field:[Inject Named(PrefKeys.AUTO_REWIND_AMOUNT)]
  lateinit var autoRewindAmountPref: Pref<Int>

  @SuppressLint("InflateParams")
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    AndroidSupportInjection.inject(this)

    val binding = DialogAmountChooserBinding.inflate(activity!!.layoutInflater)!!

    val oldRewindAmount = autoRewindAmountPref.value
    binding.seekBar.max = (MAX - MIN) * FACTOR
    binding.seekBar.progress = (oldRewindAmount - MIN) * FACTOR
    binding.seekBar.onProgressChanged(initialNotification = true) {
      val progress = it / FACTOR
      val autoRewindSummary = context!!.resources.getQuantityString(
        R.plurals.pref_auto_rewind_summary,
        progress,
        progress
      )
      binding.textView.text = autoRewindSummary
    }

    return MaterialDialog.Builder(context!!)
      .title(R.string.pref_auto_rewind_title)
      .customView(binding.root, true)
      .positiveText(R.string.dialog_confirm)
      .negativeText(R.string.dialog_cancel)
      .onPositive { _, _ ->
        val newRewindAmount = binding.seekBar.progress / FACTOR + MIN
        autoRewindAmountPref.value = newRewindAmount
      }
      .build()
  }

  companion object {
    val TAG: String = AutoRewindDialogFragment::class.java.simpleName

    private val MIN = 0
    private val MAX = 20
    private val FACTOR = 10
  }
}
