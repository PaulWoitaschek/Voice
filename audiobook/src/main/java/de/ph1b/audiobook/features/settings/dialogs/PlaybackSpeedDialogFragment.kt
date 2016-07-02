package de.ph1b.audiobook.features.settings.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.jakewharton.rxbinding.widget.RxSeekBar
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.layoutInflater
import de.ph1b.audiobook.persistence.BookChest
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayerController
import kotlinx.android.synthetic.main.dialog_amount_chooser.view.*
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Dialog for setting the playback speed of the current book.

 * @author Paul Woitaschek
 */
class PlaybackSpeedDialogFragment : DialogFragment() {

    @Inject internal lateinit var prefs: PrefsManager
    @Inject internal lateinit var db: BookChest
    @Inject internal lateinit var playerController: PlayerController

    private val SPEED_DELTA = 0.02f
    private val MAX_STEPS = Math.round((Book.SPEED_MAX - Book.SPEED_MIN) / SPEED_DELTA)
    private val df = DecimalFormat("0.00")

    private fun speedValueToSteps(speed: Float): Int =
            Math.round((speed - Book.SPEED_MIN) * (MAX_STEPS + 1) / (Book.SPEED_MAX - Book.SPEED_MIN))

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        App.component().inject(this)

        // init views
        val v = context.layoutInflater().inflate(R.layout.dialog_amount_chooser, null)

        // setting current speed
        val book = db.bookById(prefs.currentBookId.value) ?: throw AssertionError("Cannot instantiate $TAG without a current book")
        val speed = book.playbackSpeed
        v.seekBar.max = MAX_STEPS
        v.seekBar.progress = speedValueToSteps(speed)

        // observable of seek bar, mapped to speed
        val seekObservable = RxSeekBar.userChanges(v.seekBar)
                .map { Book.SPEED_MIN + it * SPEED_DELTA }
                .share()

        // update speed text
        seekObservable
                .map { formatTime(it) } // to text
                .subscribe { v.textView.text = it }

        // set new speed
        seekObservable.debounce(50, TimeUnit.MILLISECONDS) // debounce so we don't flood the player
                .subscribe { playerController.setSpeed(it) }

        return MaterialDialog.Builder(activity)
                .title(R.string.playback_speed)
                .customView(v, true)
                .build()
    }

    private fun formatTime(time: Float): String = "${getString(R.string.playback_speed)}: ${df.format(time.toDouble())}x"

    companion object {
        val TAG: String = PlaybackSpeedDialogFragment::class.java.simpleName
    }
}
