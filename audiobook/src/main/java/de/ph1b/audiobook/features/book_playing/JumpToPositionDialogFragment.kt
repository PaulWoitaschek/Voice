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
 * along with Material Audiobook Player. If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.features.book_playing

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import android.widget.NumberPicker
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.persistence.BookChest
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.uitools.theme
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class JumpToPositionDialogFragment : DialogFragment() {

    init {
        App.component().inject(this)
    }

    @Inject internal lateinit var prefs: PrefsManager
    @Inject internal lateinit var db: BookChest
    @Inject internal lateinit var playerController: PlayerController

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // find views
        val v = activity.layoutInflater.inflate(R.layout.dialog_time_picker, null)
        val mPicker = v.findViewById(R.id.number_minute) as NumberPicker
        val hPicker = v.findViewById(R.id.number_hour) as NumberPicker
        val colon = v.findViewById(R.id.colon)

        // init
        val book = db.bookById(prefs.currentBookId.value)!!
        val duration = book.currentChapter().duration
        val position = book.time
        val biggestHour = TimeUnit.MILLISECONDS.toHours(duration.toLong()).toInt()
        val durationInMinutes = TimeUnit.MILLISECONDS.toMinutes(duration.toLong()).toInt()
        if (biggestHour == 0) {
            //sets visibility of hour related things to gone if max.hour is zero
            colon.visibility = View.GONE
            hPicker.visibility = View.GONE
        }

        //set maximum values
        hPicker.maxValue = biggestHour
        if (biggestHour == 0) {
            mPicker.maxValue = TimeUnit.MILLISECONDS.toMinutes(duration.toLong()).toInt()
        } else {
            mPicker.maxValue = 59
        }

        //set default values
        val defaultHour = TimeUnit.MILLISECONDS.toHours(position.toLong()).toInt()
        val defaultMinute = TimeUnit.MILLISECONDS.toMinutes(position.toLong()).toInt() % 60
        hPicker.value = defaultHour
        mPicker.value = defaultMinute

        hPicker.setOnValueChangedListener { picker, oldVal, newVal ->
            if (newVal == biggestHour) {
                mPicker.maxValue = (durationInMinutes - newVal * 60) % 60
            } else {
                mPicker.maxValue = 59
            }
        }

        mPicker.setOnValueChangedListener { picker, oldVal, newVal ->
            var hValue = hPicker.value

            //scrolling forward
            if (oldVal == 59 && newVal == 0) {
                hPicker.value = ++hValue
            }
            //scrolling backward
            if (oldVal == 0 && newVal == 59) {
                hPicker.value = --hValue
            }
        }

        mPicker.theme()
        hPicker.theme()

        return MaterialDialog.Builder(context)
                .customView(v, true)
                .title(R.string.action_time_change)
                .onPositive { materialDialog, dialogAction ->
                    val h = hPicker.value
                    val m = mPicker.value
                    val newPosition = (m + 60 * h) * 60 * 1000
                    playerController.changePosition(newPosition, book.currentChapter().file)
                }
                .positiveText(R.string.dialog_confirm)
                .negativeText(R.string.dialog_cancel)
                .build()
    }

    companion object {

        val TAG = JumpToPositionDialogFragment::class.java.simpleName
    }
}
