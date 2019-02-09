package de.ph1b.audiobook.features.settings.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.DialogController
import de.ph1b.audiobook.misc.DialogLayoutContainer
import de.ph1b.audiobook.misc.inflate
import de.ph1b.audiobook.misc.progressChangedStream
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.PlayerController
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.dialog_amount_chooser.*
import java.text.DecimalFormat
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

/**
 * Dialog for setting the playback speed of the current book.
 */
class PlaybackSpeedDialogController : DialogController() {

  @Inject
  lateinit var repo: BookRepository
  @field:[Inject Named(PrefKeys.CURRENT_BOOK)]
  lateinit var currentBookIdPref: Pref<UUID>
  @Inject
  lateinit var playerController: PlayerController

  @SuppressLint("InflateParams")
  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    appComponent.inject(this)

    // init views
    val container =
      DialogLayoutContainer(activity!!.layoutInflater.inflate(R.layout.dialog_amount_chooser))
    val seekBar = container.seekBar
    val textView = container.textView

    // setting current speed
    val book = repo.bookById(currentBookIdPref.value)
      ?: throw AssertionError("Cannot instantiate ${javaClass.name} without a current book")
    val speed = book.content.playbackSpeed
    seekBar.max = ((MAX - MIN) * FACTOR).toInt()
    seekBar.progress = ((speed - MIN) * FACTOR).toInt()

    // observable of seek bar, mapped to speed
    seekBar.progressChangedStream(initialNotification = true)
      .map { Book.SPEED_MIN + it.toFloat() / FACTOR }
      .doOnNext {
        // update speed text
        val text = "${activity!!.getString(R.string.playback_speed)}: ${speedFormatter.format(it)}"
        textView.text = text
      }
      .debounce(50, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
      .subscribe { playerController.setSpeed(it) } // update speed after debounce
      .disposeOnDestroyDialog()

    return MaterialDialog(activity!!).apply {
      title(R.string.playback_speed)
      customView(view = container.containerView, scrollable = true)
    }
  }

  companion object {
    private const val MAX = Book.SPEED_MAX
    private const val MIN = Book.SPEED_MIN
    private const val FACTOR = 100F
    private val speedFormatter = DecimalFormat("0.0 x")
  }
}
