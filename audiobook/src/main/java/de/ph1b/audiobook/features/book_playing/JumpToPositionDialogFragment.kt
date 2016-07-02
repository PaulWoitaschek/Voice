package de.ph1b.audiobook.features.book_playing

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import de.ph1b.audiobook.R
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.layoutInflater
import de.ph1b.audiobook.persistence.BookChest
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.uitools.theme
import kotlinx.android.synthetic.main.dialog_time_picker.view.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class JumpToPositionDialogFragment : DialogFragment() {

    @Inject internal lateinit var prefs: PrefsManager
    @Inject internal lateinit var db: BookChest
    @Inject internal lateinit var playerController: PlayerController

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        App.component().inject(this)
        val v = context.layoutInflater().inflate(R.layout.dialog_time_picker, null)

        // init
        val book = db.bookById(prefs.currentBookId.value)!!
        val duration = book.currentChapter().duration
        val position = book.time
        val biggestHour = TimeUnit.MILLISECONDS.toHours(duration.toLong()).toInt()
        val durationInMinutes = TimeUnit.MILLISECONDS.toMinutes(duration.toLong()).toInt()
        if (biggestHour == 0) {
            //sets visibility of hour related things to gone if max.hour is zero
            v.colon.visibility = View.GONE
            v.numberHour.visibility = View.GONE
        }

        //set maximum values
        v.numberHour.maxValue = biggestHour
        if (biggestHour == 0) {
            v.numberMinute.maxValue = TimeUnit.MILLISECONDS.toMinutes(duration.toLong()).toInt()
        } else {
            v.numberMinute.maxValue = 59
        }

        //set default values
        val defaultHour = TimeUnit.MILLISECONDS.toHours(position.toLong()).toInt()
        val defaultMinute = TimeUnit.MILLISECONDS.toMinutes(position.toLong()).toInt() % 60
        v.numberHour.value = defaultHour
        v.numberMinute.value = defaultMinute

        v.numberHour.setOnValueChangedListener { picker, oldVal, newVal ->
            if (newVal == biggestHour) {
                v.numberMinute.maxValue = (durationInMinutes - newVal * 60) % 60
            } else {
                v.numberMinute.maxValue = 59
            }
        }

        v.numberMinute.setOnValueChangedListener { picker, oldVal, newVal ->
            var hValue = v.numberHour.value

            //scrolling forward
            if (oldVal == 59 && newVal == 0) {
                v.numberHour.value = ++hValue
            }
            //scrolling backward
            if (oldVal == 0 && newVal == 59) {
                v.numberHour.value = --hValue
            }
        }

        v.numberMinute.theme()
        v.numberHour.theme()

        return MaterialDialog.Builder(context)
                .customView(v, true)
                .title(R.string.action_time_change)
                .onPositive { materialDialog, dialogAction ->
                    val h = v.numberHour.value
                    val m = v.numberMinute.value
                    val newPosition = (m + 60 * h) * 60 * 1000
                    playerController.changePosition(newPosition, book.currentChapter().file)
                }
                .positiveText(R.string.dialog_confirm)
                .negativeText(R.string.dialog_cancel)
                .build()
    }

    companion object {

        val TAG: String = JumpToPositionDialogFragment::class.java.simpleName
    }
}
