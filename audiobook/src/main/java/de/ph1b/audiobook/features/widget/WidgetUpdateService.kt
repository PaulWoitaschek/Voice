package de.ph1b.audiobook.features.widget

import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.IBinder
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.RemoteViews
import com.squareup.picasso.Picasso
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.MainActivity
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.asV2Observable
import de.ph1b.audiobook.misc.dpToPxRounded
import de.ph1b.audiobook.misc.drawable
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.utils.ServiceController
import de.ph1b.audiobook.uitools.CoverReplacement
import de.ph1b.audiobook.uitools.ImageHelper
import de.ph1b.audiobook.uitools.maxImageSize
import e
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WidgetUpdateService : Service() {
  private val executor = ThreadPoolExecutor(
      1, 1, // single thread
      5, TimeUnit.SECONDS,
      LinkedBlockingQueue<Runnable>(2), // queue capacity
      ThreadPoolExecutor.DiscardOldestPolicy())
  private val disposables = CompositeDisposable()
  @Inject lateinit var prefs: PrefsManager
  @Inject lateinit var repo: BookRepository
  @Inject lateinit var playStateManager: PlayStateManager
  @Inject lateinit var imageHelper: ImageHelper
  @Inject lateinit var serviceController: ServiceController

  override fun onCreate() {
    super.onCreate()
    App.component.inject(this)

    // update widget if current book, current book id or playState have changed.
    disposables.add(Observable.merge(
        repo.updateObservable().filter { it.id == prefs.currentBookId.value },
        playStateManager.playStateStream(),
        prefs.currentBookId.asV2Observable())
        .subscribe { updateWidget() }
    )

  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    updateWidget()
    return Service.START_STICKY
  }

  /**
   * Asynchronously updates the widget
   */
  private fun updateWidget() {
    executor.execute {
      val appWidgetManager = AppWidgetManager.getInstance(this@WidgetUpdateService)
      val book = repo.bookById(prefs.currentBookId.value)
      val isPortrait = isPortrait
      val ids = appWidgetManager.getAppWidgetIds(ComponentName(
          this@WidgetUpdateService, BaseWidgetProvider::class.java))

      for (widgetId in ids) {
        val remoteViews = RemoteViews(packageName, R.layout.widget)

        if (book != null) {

          val opts = appWidgetManager.getAppWidgetOptions(widgetId)
          val minHeightDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
          val minHeight = dpToPxRounded(minHeightDp.toFloat())
          val maxHeightDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
          val maxHeight = dpToPxRounded(maxHeightDp.toFloat())
          val minWidthDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
          val minWidth = dpToPxRounded(minWidthDp.toFloat())
          val maxWidthDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
          val maxWidth = dpToPxRounded(maxWidthDp.toFloat())

          val useWidth: Int
          val useHeight: Int

          if (isPortrait) {
            useWidth = minWidth
            useHeight = maxHeight
          } else {
            useWidth = maxWidth
            useHeight = minHeight
          }

          initElements(remoteViews = remoteViews, book = book, coverSize = useHeight)


          if (useWidth > 0 && useHeight > 0) {
            setVisibilities(remoteViews, useWidth, useHeight, book.chapters.size == 1)
          }
        } else {
          // directly going back to bookChoose
          val wholeWidgetClickI = Intent(this@WidgetUpdateService, MainActivity::class.java)
          wholeWidgetClickI.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
          val wholeWidgetClickPI = PendingIntent.getActivity(this@WidgetUpdateService, System.currentTimeMillis().toInt(),
              wholeWidgetClickI, PendingIntent.FLAG_UPDATE_CURRENT)
          val cover = imageHelper.drawableToBitmap(
              drawable = drawable(R.drawable.icon_108dp),
              width = imageHelper.smallerScreenSize,
              height = imageHelper.smallerScreenSize
          )
          remoteViews.setImageViewBitmap(R.id.imageView, cover)
          remoteViews.setOnClickPendingIntent(R.id.wholeWidget, wholeWidgetClickPI)
        }

        appWidgetManager.updateAppWidget(widgetId, remoteViews)
      }
    }
  }

  /**
   * Returning if the current orientation is portrait. If it is unknown, measure the display-spec
   * and return accordingly.

   * @return true if the current orientation is portrait
   */
  private val isPortrait: Boolean
    get() {
      val orientation = resources.configuration.orientation
      val window = getSystemService(Context.WINDOW_SERVICE) as WindowManager
      val display = window.defaultDisplay

      @Suppress("DEPRECATION")
      val displayWidth = display.width
      @Suppress("DEPRECATION")
      val displayHeight = display.height

      return orientation != Configuration.ORIENTATION_LANDSCAPE && (orientation == Configuration.ORIENTATION_PORTRAIT || displayWidth == displayHeight || displayWidth < displayHeight)
    }

  private fun initElements(remoteViews: RemoteViews, book: Book, coverSize: Int) {
    val playPauseI = serviceController.getPlayPauseIntent()
    val playPausePI = PendingIntent.getService(this,
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, playPauseI, PendingIntent.FLAG_UPDATE_CURRENT)
    remoteViews.setOnClickPendingIntent(R.id.playPause, playPausePI)

    val fastForwardI = serviceController.getFastForwardIntent()
    val fastForwardPI = PendingIntent.getService(this,
        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, fastForwardI,
        PendingIntent.FLAG_UPDATE_CURRENT)
    remoteViews.setOnClickPendingIntent(R.id.fastForward, fastForwardPI)

    val rewindI = serviceController.getRewindIntent()
    val rewindPI = PendingIntent.getService(this, KeyEvent.KEYCODE_MEDIA_REWIND,
        rewindI, PendingIntent.FLAG_UPDATE_CURRENT)
    remoteViews.setOnClickPendingIntent(R.id.rewind, rewindPI)

    if (playStateManager.playState === PlayStateManager.PlayState.PLAYING) {
      remoteViews.setImageViewResource(R.id.playPause, R.drawable.ic_pause_white_36dp)
    } else {
      remoteViews.setImageViewResource(R.id.playPause, R.drawable.ic_play_white_36dp)
    }

    // if we have any book, init the views and have a click on the whole widget start BookPlay.
    // if we have no book, simply have a click on the whole widget start BookChoose.
    remoteViews.setTextViewText(R.id.title, book.name)
    val name = book.currentChapter().name

    remoteViews.setTextViewText(R.id.summary, name)

    val wholeWidgetClickI = MainActivity.goToBookIntent(this, book.id)
    val wholeWidgetClickPI = PendingIntent.getActivity(this@WidgetUpdateService, System.currentTimeMillis().toInt(), wholeWidgetClickI,
        PendingIntent.FLAG_UPDATE_CURRENT)

    var cover: Bitmap? = null
    try {
      val coverFile = book.coverFile()
      if (coverFile.canRead() && coverFile.length() < maxImageSize) {
        cover = Picasso.with(this@WidgetUpdateService)
            .load(coverFile)
            .resize(coverSize, coverSize)
            .get()
      }
    } catch (ex: IOException) {
      e(ex) { "Error when retrieving cover for book $book" }
    }

    if (cover == null) {
      cover = imageHelper.drawableToBitmap(CoverReplacement(book.name, this@WidgetUpdateService),
          imageHelper.smallerScreenSize,
          imageHelper.smallerScreenSize)
    }
    remoteViews.setImageViewBitmap(R.id.imageView, cover)
    remoteViews.setOnClickPendingIntent(R.id.wholeWidget, wholeWidgetClickPI)
  }

  /**
   * Sets visibilities on widgets element, depending on the size of the widget

   * @param remoteViews   the widget the widget RemoteViews
   * *
   * @param width         the width of the widget
   * *
   * @param height        the height of the widget
   * *
   * @param singleChapter if true if the book has only one chapter
   */
  private fun setVisibilities(remoteViews: RemoteViews, width: Int,
                              height: Int, singleChapter: Boolean) {
    setXVisibility(remoteViews, width, height)
    setYVisibility(remoteViews, height, singleChapter)
  }

  /**
   * Set visibilities dependent on widget width.

   * @param remoteViews the widget RemoteViews
   * *
   * @param widgetWidth The widget width
   * *
   * @param coverSize   The cover size
   */
  private fun setXVisibility(remoteViews: RemoteViews, widgetWidth: Int,
                             coverSize: Int) {
    val singleButtonSize = dpToPxRounded(8F + 36F + 8F)
    // widget height because cover is square
    var summarizedItemWidth = 3 * singleButtonSize + coverSize

    // set all views visible
    remoteViews.setViewVisibility(R.id.imageView, View.VISIBLE)
    remoteViews.setViewVisibility(R.id.rewind, View.VISIBLE)
    remoteViews.setViewVisibility(R.id.fastForward, View.VISIBLE)

    // hide cover if we need space
    if (summarizedItemWidth > widgetWidth) {
      remoteViews.setViewVisibility(R.id.imageView, View.GONE)
      summarizedItemWidth -= coverSize
    }

    // hide fast forward if we need space
    if (summarizedItemWidth > widgetWidth) {
      remoteViews.setViewVisibility(R.id.fastForward, View.GONE)
      summarizedItemWidth -= singleButtonSize
    }

    // hide rewind if we need space
    if (summarizedItemWidth > widgetWidth) {
      remoteViews.setViewVisibility(R.id.rewind, View.GONE)
    }
  }

  /**
   * Sets visibilities dependent on widget height.

   * @param remoteViews   The Widget RemoteViews
   * *
   * @param widgetHeight  The widget height
   * *
   * @param singleChapter true if the book has only one chapter
   */
  private fun setYVisibility(remoteViews: RemoteViews, widgetHeight: Int,
                             singleChapter: Boolean) {
    val buttonSize = dpToPxRounded(8F + 36F + 8F)
    val titleSize = resources.getDimensionPixelSize(R.dimen.list_text_primary_size)
    val summarySize = resources.getDimensionPixelSize(R.dimen.list_text_secondary_size)

    var summarizedItemsHeight = buttonSize + titleSize + summarySize

    // first setting all views visible
    remoteViews.setViewVisibility(R.id.summary, View.VISIBLE)
    remoteViews.setViewVisibility(R.id.title, View.VISIBLE)

    // when we are in a single chapter or we are to high, hide summary
    if (singleChapter || widgetHeight < summarizedItemsHeight) {
      remoteViews.setViewVisibility(R.id.summary, View.GONE)
      summarizedItemsHeight -= summarySize
    }

    // if we ar still to high, hide title
    if (summarizedItemsHeight > widgetHeight) {
      remoteViews.setViewVisibility(R.id.title, View.GONE)
    }
  }

  override fun onDestroy() {
    super.onDestroy()

    disposables.dispose()

    executor.shutdown()
  }

  override fun onConfigurationChanged(newCfg: Configuration) {
    val oldOrientation = this.resources.configuration.orientation
    val newOrientation = newCfg.orientation

    if (newOrientation != oldOrientation) {
      updateWidget()
    }
  }

  override fun onBind(intent: Intent): IBinder? {
    return null
  }
}
