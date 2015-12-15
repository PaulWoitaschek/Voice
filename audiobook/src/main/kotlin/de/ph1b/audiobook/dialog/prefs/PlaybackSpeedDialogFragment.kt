/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.dialog.prefs

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.widget.SeekBar
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.mediaplayer.MediaPlayerController
import de.ph1b.audiobook.model.Book
import de.ph1b.audiobook.persistence.BookChest
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.utils.BookVendor
import java.text.DecimalFormat
import javax.inject.Inject

/**
 * Dialog for setting the playback speed of the current book.

 * @author Paul Woitaschek
 */
class PlaybackSpeedDialogFragment : DialogFragment() {

    @Inject internal lateinit var prefs: PrefsManager
    @Inject internal lateinit var db: BookChest
    @Inject internal lateinit var bookVendor: BookVendor
    @Inject internal lateinit var mediaPlayerController: MediaPlayerController

    private val SPEED_DELTA = 0.02f
    private val MAX_STEPS = Math.round((Book.SPEED_MAX - Book.SPEED_MIN) / SPEED_DELTA)
    private val df = DecimalFormat("0.00")

    private fun speedStepValueToSpeed(step: Int): Float =
            (Book.SPEED_MIN + (step * SPEED_DELTA))

    private fun speedValueToSteps(speed: Float): Int =
            Math.round((speed - Book.SPEED_MIN) * (MAX_STEPS + 1) / (Book.SPEED_MAX - Book.SPEED_MIN))

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        App.component().inject(this)

        // init views
        val inflater = LayoutInflater.from(context)
        val v = inflater.inflate(R.layout.dialog_amount_chooser, null)
        val seekBar = v.findViewById(R.id.seekBar) as SeekBar
        val textView = v.findViewById(R.id.textView) as TextView

        // setting current speed
        val book = bookVendor.byId(prefs.currentBookId.value) ?: throw AssertionError("Cannot instantiate $TAG without a current book")
        val speed = book.playbackSpeed
        textView.text = formatTime(speed)
        seekBar.max = MAX_STEPS
        seekBar.progress = speedValueToSteps(speed)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, step: Int, fromUser: Boolean) {
                val newSpeed = speedStepValueToSpeed(step)
                textView.text = formatTime(newSpeed)
                mediaPlayerController.playbackSpeed = newSpeed
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })

        return MaterialDialog.Builder(activity)
                .title(R.string.playback_speed)
                .customView(v, true)
                .build()
    }

    private fun formatTime(time: Float): String = "${getString(R.string.playback_speed)}: ${df.format(time.toDouble())}x"

    companion object {
        val TAG = PlaybackSpeedDialogFragment::class.java.simpleName
    }
}
