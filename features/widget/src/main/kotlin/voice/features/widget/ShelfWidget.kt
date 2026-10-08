package voice.features.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import kotlinx.coroutines.flow.first
import voice.core.strings.R as StringsR

class ShelfWidget : GlanceAppWidget() {

  override val sizeMode: SizeMode = SizeMode.Exact

  override val previewSizeMode = SizeMode.Responsive(
    setOf(DpSize(130.dp, 130.dp), DpSize(180.dp, 180.dp), DpSize(300.dp, 180.dp)),
  )

  override suspend fun provideGlance(
    context: Context,
    id: GlanceId,
  ) {
    val graph = widgetGraph
    val states = graph.widgetData.shelfState()
    val initial = states.first()
    provideContent {
      val state by states.collectAsState(initial)
      ShelfContent(state, graph.widgetImages, graph.widgetActions)
    }
  }

  override suspend fun providePreview(
    context: Context,
    widgetCategory: Int,
  ) {
    val graph = widgetGraph
    val state = graph.widgetData.shelfState().first()
    provideContent {
      ShelfContent(state, graph.widgetImages, graph.widgetActions)
    }
  }
}

/** Each slot keeps its shape, so covers change shape as they move along, like books on a shelf. */
private val SlotShapes = listOf(
  WidgetShape.Cookie9,
  WidgetShape.Clover,
  WidgetShape.Arch,
  WidgetShape.Sunny,
  WidgetShape.Pentagon,
)

@Composable
internal fun ShelfContent(
  state: ShelfState,
  images: WidgetImages,
  actions: WidgetActions,
) {
  WidgetColors(theme = state.theme) {
    val size = LocalSize.current
    Box(
      modifier = GlanceModifier
        .fillMaxSize()
        .appWidgetBackground()
        .widgetSurface(GlanceTheme.colors.widgetBackground),
    ) {
      when {
        state.books.isEmpty() -> ShelfEmpty(actions)
        size.width < 220.dp -> ShelfGrid(state.books.take(4), images, actions)
        else -> ShelfRow(state.books, slots = if (size.width >= 400.dp) SHELF_MAX_BOOKS else 4, images, actions)
      }
    }
  }
}

@Composable
private fun ShelfGrid(
  books: List<ShelfItem>,
  images: WidgetImages,
  actions: WidgetActions,
) {
  val size = LocalSize.current
  val padding = 10.dp
  val gap = 6.dp
  val columns = if (books.size == 1) 1 else 2
  val rows = if (books.size <= 2) 1 else 2
  val slot = min((size.width - padding * 2 - gap * (columns - 1)) / columns, (size.height - padding * 2 - gap * (rows - 1)) / rows)
  Column(
    modifier = GlanceModifier.fillMaxSize().padding(padding),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    repeat(rows) { row ->
      if (row > 0) Spacer(GlanceModifier.height(gap))
      Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(columns) { column ->
          if (column > 0) Spacer(GlanceModifier.width(gap))
          val index = row * columns + column
          val item = books.getOrNull(index)
          if (item != null) {
            ShelfCover(item, SlotShapes[index], slot, images, actions)
          } else {
            EmptySlot(SlotShapes[index], slot, images, actions)
          }
        }
      }
    }
  }
}

@Composable
private fun ShelfRow(
  books: List<ShelfItem>,
  slots: Int,
  images: WidgetImages,
  actions: WidgetActions,
) {
  val context = LocalContext.current
  val size = LocalSize.current
  val padding = 12.dp
  val gap = 10.dp
  val headerHeight = 28.dp
  val slotWidth = (size.width - padding * 2 - gap * (slots - 1)) / slots
  val withDetails = size.height >= 150.dp
  val detailsHeight = if (withDetails) 34.dp else 0.dp
  val coverSize = min(slotWidth, size.height - padding * 2 - headerHeight - 8.dp - detailsHeight).coerceAtMost(120.dp)
  Column(modifier = GlanceModifier.fillMaxSize().padding(padding)) {
    Row(
      modifier = GlanceModifier.fillMaxWidth().height(headerHeight).clickable(actions.openLibrary()),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TitleText(
        text = context.getString(StringsR.string.widget_shelf_title),
        modifier = GlanceModifier.defaultWeight(),
        fontSize = 15.sp,
      )
      WidgetIcon(icon = R.drawable.widget_ic_library, size = 20.dp, color = GlanceTheme.colors.onSurfaceVariant)
    }
    Spacer(GlanceModifier.defaultWeight())
    Row(modifier = GlanceModifier.fillMaxWidth()) {
      repeat(slots) { index ->
        if (index > 0) Spacer(GlanceModifier.width(gap))
        val item = books.getOrNull(index)
        Column(
          modifier = GlanceModifier.width(slotWidth),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          if (item == null) {
            EmptySlot(SlotShapes[index], coverSize, images, actions)
          } else {
            ShelfCover(item, SlotShapes[index], coverSize, images, actions)
          }
          if (withDetails && item != null) {
            Spacer(GlanceModifier.height(4.dp))
            SubtitleText(text = item.book.title, fontSize = 12.sp)
            Spacer(GlanceModifier.height(2.dp))
            ProgressWave(progress = item.book.progress, wavy = item.book.playing, width = coverSize, images = images)
          }
        }
      }
    }
    Spacer(GlanceModifier.defaultWeight())
  }
}

@Composable
private fun ShelfCover(
  item: ShelfItem,
  shape: WidgetShape,
  size: Dp,
  images: WidgetImages,
  actions: WidgetActions,
) {
  val description = LocalContext.current.getString(StringsR.string.widget_shelf_play, item.book.title)
  Box(
    modifier = GlanceModifier
      .width(size)
      .height(size)
      .clickable(actions.playBook(item.book.id))
      .semantics { contentDescription = description },
    contentAlignment = Alignment.TopEnd,
  ) {
    ShapedCover(cover = item.cover, shape = shape, size = size, images = images)
    if (item.book.playing) {
      ShapeButton(
        shape = WidgetShape.Circle,
        size = (size * 0.32F).coerceAtMost(28.dp),
        icon = R.drawable.widget_ic_equalizer,
        contentDescription = null,
        images = images,
        onClick = null,
        iconFraction = 0.6F,
      )
    }
  }
}

/** Keeps the shelf looking like a shelf while only a book or two are in progress. */
@Composable
private fun EmptySlot(
  shape: WidgetShape,
  size: Dp,
  images: WidgetImages,
  actions: WidgetActions,
) {
  Image(
    provider = ImageProvider(images.shape(shape, size)),
    contentDescription = LocalContext.current.getString(StringsR.string.widget_no_book_action),
    modifier = GlanceModifier.size(size).clickable(actions.openLibrary()),
    colorFilter = ColorFilter.tint(GlanceTheme.colors.secondaryContainer),
  )
}

@Composable
private fun ShelfEmpty(actions: WidgetActions) {
  val context = LocalContext.current
  Column(
    modifier = GlanceModifier.fillMaxSize().clickable(actions.openLibrary()).padding(14.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    WidgetIcon(icon = R.drawable.widget_ic_library, size = 28.dp, color = GlanceTheme.colors.primary)
    Spacer(GlanceModifier.height(8.dp))
    TitleText(text = context.getString(StringsR.string.widget_shelf_title), fontSize = 15.sp)
    SubtitleText(text = context.getString(StringsR.string.widget_shelf_empty), maxLines = 2)
    Spacer(GlanceModifier.height(10.dp))
    PillButton(text = context.getString(StringsR.string.widget_no_book_action), onClick = actions.openLibrary())
  }
}
