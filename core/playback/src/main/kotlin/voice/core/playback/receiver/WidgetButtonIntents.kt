package voice.core.playback.receiver

import android.content.Intent
import voice.core.data.BookId

interface WidgetButtonIntents {
  fun playPause(): Intent
  fun rewind(): Intent
  fun fastForward(): Intent
  fun startOver(): Intent
  fun playBook(id: BookId): Intent
}
