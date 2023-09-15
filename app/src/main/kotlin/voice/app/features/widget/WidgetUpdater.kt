package voice.app.features.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import androidx.datastore.core.DataStore
import coil.imageLoader
import coil.request.ImageRequest
import dagger.Reusable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import voice.app.R
import voice.app.features.MainActivity
import voice.common.BookId
import voice.common.dpToPxRounded
import voice.common.pref.CurrentBook
import voice.data.Book
import voice.data.repo.BookRepository
import voice.playback.playstate.PlayStateManager
import voice.playback.receiver.WidgetButtonReceiver
import javax.inject.Inject
import voice.common.R as CommonR

@Reusable
class WidgetUpdater
@Inject constructor(
  private val context: Context,
  private val repo: BookRepository,
  @CurrentBook
  private val currentBook: DataStore<BookId?>,
  private val playStateManager: PlayStateManager,
) {

  private val appWidgetManager = AppWidgetManager.getInstance(context)

  private val scope = CoroutineScope(Dispatchers.IO)

  fun update() {
    scope.launch {
      val book = currentBook.data.first()?.let {
        repo.get(it)
      }
      val componentName = ComponentName(this@WidgetUpdater.context, BaseWidgetProvider::class.java)
      val ids = appWidgetManager.getAppWidgetIds(componentName)

      for (widgetId in ids) {
        updateWidgetForId(book, widgetId)
      }
    }
  }

  private suspend fun updateWidgetForId(
    book: Book?,
    widgetId: Int,
  ) {
    if (book != null) {
      initWidgetForPresentBook(widgetId, book)
    } else {
      initWidgetForAbsentBook(widgetId)
    }
  }

  private suspend fun initWidgetForPresentBook(
    widgetId: Int,
    book: Book,
  ) {
    val opts = appWidgetManager.getAppWidgetOptions(widgetId)
    val useWidth = widgetWidth(opts)
    val useHeight = widgetHeight(opts)

    val remoteViews = RemoteViews(context.packageName, R.layout.widget)
    initElements(remoteViews = remoteViews, book = book, coverSize = useHeight)

    if (useWidth > 0 && useHeight > 0) {
      setVisibilities(remoteViews, useWidth, useHeight, book.content.chapters.size == 1)
    }
    appWidgetManager.updateAppWidget(widgetId, remoteViews)
  }

  private fun widgetWidth(opts: Bundle): Int {
    val key = if (isPortrait) {
      AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
    } else {
      AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
    }
    val dp = opts.getInt(key)
    return context.dpToPxRounded(dp.toFloat())
  }

  private fun widgetHeight(opts: Bundle): Int {
    val key = if (isPortrait) {
      AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
    } else {
      AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
    }
    val dp = opts.getInt(key)
    return context.dpToPxRounded(dp.toFloat())
  }

  private fun initWidgetForAbsentBook(widgetId: Int) {
    val remoteViews = RemoteViews(context.packageName, R.layout.widget)
    // directly going back to bookChoose
    val wholeWidgetClickI = Intent(context, MainActivity::class.java)
    wholeWidgetClickI.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
    val wholeWidgetClickPI = PendingIntent.getActivity(
      context,
      System.currentTimeMillis().toInt(),
      wholeWidgetClickI,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    remoteViews.setImageViewResource(R.id.imageView, CommonR.drawable.album_art)
    remoteViews.setOnClickPendingIntent(R.id.wholeWidget, wholeWidgetClickPI)
    appWidgetManager.updateAppWidget(widgetId, remoteViews)
  }

  private val isPortrait: Boolean
    get() {
      val orientation = context.resources.configuration.orientation
      return orientation == Configuration.ORIENTATION_PORTRAIT
    }

  private suspend fun initElements(
    remoteViews: RemoteViews,
    book: Book,
    coverSize: Int,
  ) {
    val playPausePI = WidgetButtonReceiver.pendingIntent(context, WidgetButtonReceiver.Action.PlayPause)
    remoteViews.setOnClickPendingIntent(R.id.playPause, playPausePI)

    val fastForwardPI = WidgetButtonReceiver.pendingIntent(context, WidgetButtonReceiver.Action.FastForward)
    remoteViews.setOnClickPendingIntent(R.id.fastForward, fastForwardPI)

    val rewindPI = WidgetButtonReceiver.pendingIntent(context, WidgetButtonReceiver.Action.Rewind)
    remoteViews.setOnClickPendingIntent(R.id.rewind, rewindPI)

    val playIcon = if (playStateManager.playState == PlayStateManager.PlayState.Playing) {
      CommonR.drawable.ic_pause_white_36dp
    } else {
      CommonR.drawable.ic_play_white_36dp
    }
    remoteViews.setImageViewResource(R.id.playPause, playIcon)

    // if we have any book, init the views and have a click on the whole widget start BookPlay.
    // if we have no book, simply have a click on the whole widget start BookChoose.
    remoteViews.setTextViewText(R.id.title, book.content.name)
    val name = book.currentChapter.name

    remoteViews.setTextViewText(R.id.summary, name)

    val wholeWidgetClickI = MainActivity.goToBookIntent(context)
    val wholeWidgetClickPI = PendingIntent.getActivity(
      context,
      System.currentTimeMillis().toInt(),
      wholeWidgetClickI,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val coverFile = book.content.cover
    if (coverFile != null && coverSize > 0) {
      val bitmap = context.imageLoader
        .execute(
          ImageRequest.Builder(context)
            .data(coverFile)
            .size(coverSize, coverSize)
            .fallback(CommonR.drawable.album_art)
            .error(CommonR.drawable.album_art)
            .allowHardware(false)
            .build(),
        )
        .drawable!!.toBitmap()
      remoteViews.setImageViewBitmap(R.id.imageView, bitmap)
    } else {
      remoteViews.setImageViewResource(R.id.imageView, CommonR.drawable.album_art)
    }

    remoteViews.setOnClickPendingIntent(R.id.wholeWidget, wholeWidgetClickPI)
  }

  private fun setVisibilities(
    remoteViews: RemoteViews,
    width: Int,
    height: Int,
    singleChapter: Boolean,
  ) {
    setHorizontalVisibility(remoteViews, width, height)
    setVerticalVisibility(remoteViews, height, singleChapter)
  }

  private fun setHorizontalVisibility(
    remoteViews: RemoteViews,
    widgetWidth: Int,
    coverSize: Int,
  ) {
    val singleButtonSize = context.dpToPxRounded(8F + 36F + 8F)
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

  private fun setVerticalVisibility(
    remoteViews: RemoteViews,
    widgetHeight: Int,
    singleChapter: Boolean,
  ) {
    val buttonSize = context.dpToPxRounded(8F + 36F + 8F)
    val titleSize = context.resources.getDimensionPixelSize(R.dimen.list_text_primary_size)
    val summarySize = context.resources.getDimensionPixelSize(R.dimen.list_text_secondary_size)

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
}
