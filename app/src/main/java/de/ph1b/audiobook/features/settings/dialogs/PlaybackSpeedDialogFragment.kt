package de.ph1b.audiobook.features.settings.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import dagger.android.support.AndroidSupportInjection
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.databinding.DialogAmountChooserBinding
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.misc.progressChangedStream
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.PlayerController
import io.reactivex.android.schedulers.AndroidSchedulers
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

/**
 * Dialog for setting the playback speed of the current book.
 */
class PlaybackSpeedDialogFragment : DialogFragment() {

  @Inject
  lateinit var repo: BookRepository
  @field:[Inject Named(PrefKeys.CURRENT_BOOK)]
  lateinit var currentBookIdPref: Pref<Long>
  @Inject
  lateinit var playerController: PlayerController

  @SuppressLint("InflateParams")
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    AndroidSupportInjection.inject(this)

    // init views
    val binding = DialogAmountChooserBinding.inflate(activity!!.layoutInflater)

    // setting current speed
    val book = repo.bookById(currentBookIdPref.value)
        ?: throw AssertionError("Cannot instantiate $TAG without a current book")
    val speed = book.playbackSpeed
    binding.seekBar.max = ((MAX - MIN) * FACTOR).toInt()
    binding.seekBar.progress = ((speed - MIN) * FACTOR).toInt()

    // observable of seek bar, mapped to speed
    binding.seekBar.progressChangedStream(initialNotification = true)
      .map { Book.SPEED_MIN + it.toFloat() / FACTOR }
      .doOnNext {
        // update speed text
        val text = "${getString(R.string.playback_speed)}: ${speedFormatter.format(it)}"
        binding.textView.text = text
      }
      .debounce(50, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
      .subscribe { playerController.setSpeed(it) } // update speed after debounce

    return MaterialDialog.Builder(activity!!)
      .title(R.string.playback_speed)
      .customView(binding.root, true)
      .build()
  }

  companion object {
    val TAG: String = PlaybackSpeedDialogFragment::class.java.simpleName
    private val MAX = Book.SPEED_MAX
    private val MIN = Book.SPEED_MIN
    private val FACTOR = 100F
    private val speedFormatter = DecimalFormat("0.0 x")
  }
}
