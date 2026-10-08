package voice.features.widget

import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.coerceIn
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
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.flow.first
import voice.core.strings.R as StringsR

class NowPlayingWidget : GlanceAppWidget() {

  override val sizeMode: SizeMode = SizeMode.Exact

  override val previewSizeMode = SizeMode.Responsive(
    setOf(
      DpSize(100.dp, 100.dp),
      DpSize(180.dp, 90.dp),
      DpSize(220.dp, 60.dp),
      DpSize(300.dp, 90.dp),
      DpSize(180.dp, 180.dp),
      DpSize(300.dp, 180.dp),
    ),
  )

  override suspend fun provideGlance(
    context: Context,
    id: GlanceId,
  ) {
    val graph = widgetGraph
    val states = graph.widgetData.nowPlayingState()
    val initial = states.first()
    provideContent {
      val state by states.collectAsState(initial)
      NowPlayingContent(state, graph.widgetImages, graph.widgetActions)
    }
  }

  override suspend fun providePreview(
    context: Context,
    widgetCategory: Int,
  ) {
    val graph = widgetGraph
    val state = graph.widgetData.nowPlayingState().first()
    provideContent {
      NowPlayingContent(state, graph.widgetImages, graph.widgetActions)
    }
  }
}

internal enum class NowPlayingLayout {
  Cover,
  Bar,
  Strip,
  Art,
  Hero,
  HeroTall,
}

internal fun nowPlayingLayout(size: DpSize): NowPlayingLayout {
  return when {
    size.width < 110.dp -> NowPlayingLayout.Cover
    size.height < 120.dp -> if (size.width < 220.dp) NowPlayingLayout.Bar else NowPlayingLayout.Strip
    size.width < 220.dp -> NowPlayingLayout.Art
    size.height < 240.dp -> NowPlayingLayout.Hero
    else -> NowPlayingLayout.HeroTall
  }
}

@Composable
internal fun NowPlayingContent(
  state: NowPlayingState,
  images: WidgetImages,
  actions: WidgetActions,
) {
  val night = state is NowPlayingState.Current && state.book.sleepTimerEnd != null && !state.book.finished
  WidgetColors(theme = state.theme, night = night) {
    val layout = nowPlayingLayout(LocalSize.current)
    when (state) {
      is NowPlayingState.Empty -> NowPlayingEmpty(state.libraryEmpty, layout, images, actions)
      is NowPlayingState.Current -> if (state.book.finished) {
        Finished(state.book, state.cover, layout, images, actions)
      } else {
        when (layout) {
          NowPlayingLayout.Cover -> CoverLayout(state.book, state.cover, images, actions)
          NowPlayingLayout.Bar -> BarLayout(state.book, images, actions)
          NowPlayingLayout.Strip -> StripLayout(state.book, state.cover, images, actions)
          NowPlayingLayout.Art -> ArtLayout(state.book, state.cover, images, actions)
          NowPlayingLayout.Hero -> HeroLayout(state.book, state.cover, images, actions, tall = false)
          NowPlayingLayout.HeroTall -> HeroLayout(state.book, state.cover, images, actions, tall = true)
        }
      }
    }
  }
}

@Composable
private fun CoverLayout(
  book: NowPlayingBook,
  cover: WidgetCover,
  images: WidgetImages,
  actions: WidgetActions,
) {
  val size = LocalSize.current
  val coverSize = min(size.width, size.height) - 8.dp
  val description = LocalContext.current.getString(
    if (book.playing) StringsR.string.playback_action_pause else StringsR.string.playback_action_play,
  )
  Box(
    modifier = GlanceModifier
      .fillMaxSize()
      .appWidgetBackground()
      .clickable(actions.playPause())
      .semantics { contentDescription = description },
    contentAlignment = Alignment.Center,
  ) {
    Box(modifier = GlanceModifier.width(coverSize).height(coverSize), contentAlignment = Alignment.BottomEnd) {
      ShapedCover(
        cover = cover,
        shape = if (book.playing) WidgetShape.Square else WidgetShape.Cookie9,
        size = coverSize,
        images = images,
      )
      PlayBadge(playing = book.playing, size = coverSize * 0.4F, images = images)
    }
  }
}

@Composable
private fun BarLayout(
  book: NowPlayingBook,
  images: WidgetImages,
  actions: WidgetActions,
) {
  val size = LocalSize.current
  Row(
    modifier = GlanceModifier
      .fillMaxSize()
      .appWidgetBackground()
      .widgetSurface(GlanceTheme.colors.widgetBackground, pill = true)
      .clickable(actions.openPlayer())
      .padding(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    PlayButton(
      playing = book.playing,
      size = (size.height - 16.dp).coerceIn(36.dp, 64.dp),
      images = images,
      onClick = actions.playPause(),
    )
    Spacer(GlanceModifier.width(10.dp))
    Column(modifier = GlanceModifier.defaultWeight()) {
      TitleText(text = book.title, fontSize = 15.sp)
      Subtitle(book, compact = true)
    }
    Spacer(GlanceModifier.width(8.dp))
  }
}

@Composable
private fun StripLayout(
  book: NowPlayingBook,
  cover: WidgetCover,
  images: WidgetImages,
  actions: WidgetActions,
) {
  val size = LocalSize.current
  val coverSize = (size.height - 16.dp).coerceIn(36.dp, 72.dp)
  val withSkip = size.width >= 290.dp
  val buttonSize = (size.height - 24.dp).coerceIn(36.dp, 52.dp)
  val controlsWidth = if (withSkip) buttonSize + (buttonSize * 0.8F) * 2 + 8.dp else buttonSize
  val textWidth = size.width - 16.dp - coverSize - 12.dp - 8.dp - controlsWidth - 12.dp
  Row(
    modifier = GlanceModifier
      .fillMaxSize()
      .appWidgetBackground()
      .widgetSurface(GlanceTheme.colors.widgetBackground, pill = true)
      .clickable(actions.openPlayer())
      .padding(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    ShapedCover(cover = cover, shape = WidgetShape.Cookie9, size = coverSize, images = images)
    Spacer(GlanceModifier.width(12.dp))
    Column(modifier = GlanceModifier.defaultWeight()) {
      TitleText(text = book.title, fontSize = 15.sp)
      Subtitle(book, compact = false)
      if (size.height >= 72.dp && textWidth >= 40.dp) {
        Spacer(GlanceModifier.height(4.dp))
        ProgressWave(progress = book.progress, wavy = book.playing, width = textWidth, images = images)
      }
    }
    Spacer(GlanceModifier.width(8.dp))
    Controls(book, buttonSize, withSkip, images, actions, gap = 4.dp)
    Spacer(GlanceModifier.width(4.dp))
  }
}

@Composable
private fun ArtLayout(
  book: NowPlayingBook,
  cover: WidgetCover,
  images: WidgetImages,
  actions: WidgetActions,
) {
  val size = LocalSize.current
  val context = LocalContext.current
  val playSize = (min(size.width, size.height) * 0.32F).coerceIn(40.dp, 64.dp)
  Box(
    modifier = GlanceModifier
      .fillMaxSize()
      .appWidgetBackground()
      .widgetSurface(GlanceTheme.colors.widgetBackground)
      .clickable(actions.openPlayer()),
  ) {
    Image(
      provider = ImageProvider(
        images.coverArt(
          cover = cover,
          width = size.width,
          height = size.height,
          cornerRadius = if (Build.VERSION.SDK_INT >= 31) 0.dp else WIDGET_RADIUS_BEFORE_S,
        ),
      ),
      contentDescription = null,
      modifier = GlanceModifier.fillMaxSize(),
      contentScale = ContentScale.FillBounds,
    )
    Image(
      provider = ImageProvider(R.drawable.widget_scrim),
      contentDescription = null,
      modifier = GlanceModifier.fillMaxSize(),
      contentScale = ContentScale.FillBounds,
    )
    Row(
      modifier = GlanceModifier.fillMaxSize().padding(12.dp),
      verticalAlignment = Alignment.Bottom,
    ) {
      Column(modifier = GlanceModifier.defaultWeight()) {
        TitleText(text = context.formatTimeLeft(book.remaining, compact = true), color = White, fontSize = 15.sp)
        Spacer(GlanceModifier.height(6.dp))
        ProgressWave(
          progress = book.progress,
          wavy = book.playing,
          width = size.width - 24.dp - playSize - 10.dp,
          images = images,
          active = White,
          track = WhiteTrack,
        )
      }
      Spacer(GlanceModifier.width(10.dp))
      PlayButton(playing = book.playing, size = playSize, images = images, onClick = actions.playPause())
    }
  }
}

@Composable
private fun HeroLayout(
  book: NowPlayingBook,
  cover: WidgetCover,
  images: WidgetImages,
  actions: WidgetActions,
  tall: Boolean,
) {
  val size = LocalSize.current
  val padding = 14.dp
  val contentWidth = size.width - padding * 2
  val coverSize = if (tall) {
    min(size.height - 190.dp, contentWidth * 0.45F).coerceIn(72.dp, 180.dp)
  } else {
    min(size.height - padding * 2, contentWidth * 0.32F).coerceIn(56.dp, 160.dp)
  }
  Box(
    modifier = GlanceModifier
      .fillMaxSize()
      .appWidgetBackground()
      .widgetSurface(GlanceTheme.colors.widgetBackground)
      .clickable(actions.openPlayer())
      .padding(padding),
  ) {
    if (tall) {
      Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          ShapedCover(cover = cover, shape = WidgetShape.Cookie9, size = coverSize, images = images)
          Spacer(GlanceModifier.width(14.dp))
          Column(modifier = GlanceModifier.defaultWeight()) {
            HeroTitle(book, maxLines = 3)
            Subtitle(book, compact = false, maxLines = 2)
          }
        }
        Spacer(GlanceModifier.defaultWeight())
        HeroProgress(book, contentWidth, images, showMeta = true)
        Spacer(GlanceModifier.height(10.dp))
        Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
          Controls(book, buttonSize = 64.dp, withSkip = true, images = images, actions = actions, gap = 16.dp)
        }
      }
    } else {
      val columnWidth = contentWidth - coverSize - 14.dp
      val showMeta = size.height >= 160.dp
      val buttonSize = if (showMeta) 48.dp else 40.dp
      Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        ShapedCover(cover = cover, shape = WidgetShape.Cookie9, size = coverSize, images = images)
        Spacer(GlanceModifier.width(14.dp))
        Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
          HeroTitle(book, maxLines = if (showMeta) 2 else 1)
          Subtitle(book, compact = !showMeta)
          Spacer(GlanceModifier.defaultWeight())
          HeroProgress(book, columnWidth, images, showMeta = showMeta)
          Spacer(GlanceModifier.height(6.dp))
          Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Controls(book, buttonSize, withSkip = columnWidth >= buttonSize * 2.6F + 20.dp, images, actions, gap = 10.dp)
          }
        }
      }
    }
  }
}

@Composable
private fun HeroTitle(
  book: NowPlayingBook,
  maxLines: Int,
) {
  Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    TitleText(text = book.title, modifier = GlanceModifier.defaultWeight(), maxLines = maxLines, fontSize = 17.sp)
    if (book.speed != 1F) {
      Spacer(GlanceModifier.width(6.dp))
      SpeedChip(book.speed)
    }
  }
}

@Composable
private fun HeroProgress(
  book: NowPlayingBook,
  width: Dp,
  images: WidgetImages,
  showMeta: Boolean,
) {
  val context = LocalContext.current
  Column {
    ProgressWave(progress = book.progress, wavy = book.playing, width = width, images = images)
    if (showMeta) {
      Spacer(GlanceModifier.height(2.dp))
      Row(modifier = GlanceModifier.width(width)) {
        if (book.chapterCount > 1) {
          SubtitleText(
            text = context.getString(StringsR.string.widget_chapter_counter, book.chapterNumber, book.chapterCount),
            modifier = GlanceModifier.defaultWeight(),
            fontSize = 12.sp,
          )
        } else {
          Spacer(GlanceModifier.defaultWeight())
        }
        SubtitleText(text = context.formatTimeLeft(book.remaining), fontSize = 12.sp)
      }
    }
  }
}

@Composable
private fun Controls(
  book: NowPlayingBook,
  buttonSize: Dp,
  withSkip: Boolean,
  images: WidgetImages,
  actions: WidgetActions,
  gap: Dp,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    if (withSkip) {
      SkipButton(forward = false, size = buttonSize * 0.8F, images = images, onClick = actions.rewind())
      Spacer(GlanceModifier.width(gap))
    }
    PlayButton(playing = book.playing, size = buttonSize, images = images, onClick = actions.playPause())
    if (withSkip) {
      Spacer(GlanceModifier.width(gap))
      SkipButton(forward = true, size = buttonSize * 0.8F, images = images, onClick = actions.fastForward())
    }
  }
}

@Composable
private fun Subtitle(
  book: NowPlayingBook,
  compact: Boolean,
  maxLines: Int = 1,
) {
  val context = LocalContext.current
  val sleepTimerEnd = book.sleepTimerEnd
  when {
    sleepTimerEnd != null -> IconSubtitle(
      icon = R.drawable.widget_ic_moon,
      text = when (sleepTimerEnd) {
        is SleepTimerEnd.At -> context.getString(
          StringsR.string.widget_sleep_timer_stops_at,
          context.formatClockTime(sleepTimerEnd.time),
        )
        is SleepTimerEnd.After -> context.formatTimeLeft(sleepTimerEnd.duration)
        SleepTimerEnd.EndOfChapter -> context.getString(StringsR.string.sleep_timer_end_of_chapter)
      },
    )
    compact || book.chapterName == null -> SubtitleText(text = context.formatTimeLeft(book.remaining, compact = compact))
    else -> SubtitleText(
      text = context.getString(StringsR.string.widget_chapter, book.chapterNumber, book.chapterName),
      maxLines = maxLines,
    )
  }
}

@Composable
private fun Finished(
  book: NowPlayingBook,
  cover: WidgetCover,
  layout: NowPlayingLayout,
  images: WidgetImages,
  actions: WidgetActions,
) {
  val context = LocalContext.current
  val size = LocalSize.current
  val title = context.getString(StringsR.string.widget_finished_title)
  val startOver = context.getString(StringsR.string.widget_finished_start_over)
  when (layout) {
    NowPlayingLayout.Cover -> Box(
      modifier = GlanceModifier.fillMaxSize().appWidgetBackground().clickable(actions.startOver()),
      contentAlignment = Alignment.Center,
    ) {
      val coverSize = min(size.width, size.height) - 8.dp
      Box(modifier = GlanceModifier.width(coverSize).height(coverSize), contentAlignment = Alignment.BottomEnd) {
        ShapedCover(cover = cover, shape = WidgetShape.Sunny, size = coverSize, images = images)
        ShapeButton(
          shape = WidgetShape.Circle,
          size = coverSize * 0.4F,
          icon = R.drawable.widget_ic_replay,
          contentDescription = startOver,
          images = images,
          onClick = null,
          container = GlanceTheme.colors.tertiary,
          content = GlanceTheme.colors.onTertiary,
          iconFraction = 0.55F,
        )
      }
    }
    NowPlayingLayout.Bar -> Row(
      modifier = GlanceModifier
        .fillMaxSize()
        .appWidgetBackground()
        .widgetSurface(GlanceTheme.colors.widgetBackground, pill = true)
        .clickable(actions.openPlayer())
        .padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ShapeButton(
        shape = WidgetShape.SoftBurst,
        size = (size.height - 16.dp).coerceIn(36.dp, 64.dp),
        icon = R.drawable.widget_ic_replay,
        contentDescription = startOver,
        images = images,
        onClick = actions.startOver(),
        container = GlanceTheme.colors.tertiaryContainer,
        content = GlanceTheme.colors.onTertiaryContainer,
      )
      Spacer(GlanceModifier.width(10.dp))
      Column(modifier = GlanceModifier.defaultWeight()) {
        TitleText(text = title, fontSize = 15.sp)
        SubtitleText(text = book.title)
      }
    }
    NowPlayingLayout.Strip -> Box(
      modifier = GlanceModifier
        .fillMaxSize()
        .appWidgetBackground()
        .widgetSurface(GlanceTheme.colors.widgetBackground, pill = true)
        .clickable(actions.openPlayer()),
    ) {
      Confetti()
      Row(modifier = GlanceModifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        CheckBurst((size.height - 16.dp).coerceIn(36.dp, 64.dp), images)
        Spacer(GlanceModifier.width(12.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
          TitleText(text = title, fontSize = 15.sp)
          SubtitleText(text = "${book.title} · ${context.formatDuration(book.duration)}")
        }
        Spacer(GlanceModifier.width(8.dp))
        PillButton(text = startOver, onClick = actions.startOver(), icon = R.drawable.widget_ic_replay)
        Spacer(GlanceModifier.width(4.dp))
      }
    }
    NowPlayingLayout.Art,
    NowPlayingLayout.Hero,
    NowPlayingLayout.HeroTall,
    -> Box(
      modifier = GlanceModifier
        .fillMaxSize()
        .appWidgetBackground()
        .widgetSurface(GlanceTheme.colors.widgetBackground)
        .clickable(actions.openPlayer()),
    ) {
      Confetti()
      Column(
        modifier = GlanceModifier.fillMaxSize().padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (layout == NowPlayingLayout.Art) {
          CheckBurst(48.dp, images)
        } else {
          Box(contentAlignment = Alignment.BottomEnd) {
            ShapedCover(cover = cover, shape = WidgetShape.Sunny, size = 72.dp, images = images)
            CheckBurst(30.dp, images)
          }
        }
        Spacer(GlanceModifier.height(8.dp))
        TitleText(text = title, fontSize = 17.sp)
        SubtitleText(
          text = if (layout == NowPlayingLayout.Art) {
            book.title
          } else {
            "${book.title} · ${context.formatDuration(book.duration)}"
          },
        )
        Spacer(GlanceModifier.height(10.dp))
        PillButton(text = startOver, onClick = actions.startOver(), icon = R.drawable.widget_ic_replay)
      }
    }
  }
}

@Composable
private fun CheckBurst(
  size: Dp,
  images: WidgetImages,
) {
  ShapeButton(
    shape = WidgetShape.SoftBurst,
    size = size,
    icon = R.drawable.widget_ic_check,
    contentDescription = null,
    images = images,
    onClick = null,
    container = GlanceTheme.colors.tertiaryContainer,
    content = GlanceTheme.colors.onTertiaryContainer,
  )
}

/** Three layers of confetti, so each can take a color of the theme. */
@Composable
private fun Confetti() {
  listOf(
    R.drawable.widget_confetti_1 to GlanceTheme.colors.primary,
    R.drawable.widget_confetti_2 to GlanceTheme.colors.tertiary,
    R.drawable.widget_confetti_3 to GlanceTheme.colors.secondary,
  ).forEach { (drawable, color) ->
    Image(
      provider = ImageProvider(drawable),
      contentDescription = null,
      modifier = GlanceModifier.fillMaxSize(),
      contentScale = ContentScale.FillBounds,
      colorFilter = ColorFilter.tint(color),
    )
  }
}

@Composable
private fun NowPlayingEmpty(
  libraryEmpty: Boolean,
  layout: NowPlayingLayout,
  images: WidgetImages,
  actions: WidgetActions,
) {
  val context = LocalContext.current
  val size = LocalSize.current
  val icon = if (libraryEmpty) R.drawable.widget_ic_folder else R.drawable.widget_ic_library
  val title = context.getString(if (libraryEmpty) StringsR.string.widget_empty_title else StringsR.string.widget_no_book_title)
  val message = context.getString(if (libraryEmpty) StringsR.string.widget_empty_message else StringsR.string.widget_no_book_message)
  val action = context.getString(if (libraryEmpty) StringsR.string.widget_empty_action else StringsR.string.widget_no_book_action)
  when (layout) {
    NowPlayingLayout.Cover -> Box(
      modifier = GlanceModifier.fillMaxSize().appWidgetBackground(),
      contentAlignment = Alignment.Center,
    ) {
      ShapeButton(
        shape = WidgetShape.Cookie9,
        size = min(size.width, size.height) - 8.dp,
        icon = icon,
        contentDescription = action,
        images = images,
        onClick = actions.openLibrary(),
        container = GlanceTheme.colors.primaryContainer,
        content = GlanceTheme.colors.onPrimaryContainer,
      )
    }
    NowPlayingLayout.Bar,
    NowPlayingLayout.Strip,
    -> Row(
      modifier = GlanceModifier
        .fillMaxSize()
        .appWidgetBackground()
        .widgetSurface(GlanceTheme.colors.widgetBackground, pill = true)
        .clickable(actions.openLibrary())
        .padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      EmptyIcon(icon, (size.height - 16.dp).coerceIn(36.dp, 56.dp))
      Spacer(GlanceModifier.width(12.dp))
      Column(modifier = GlanceModifier.defaultWeight()) {
        TitleText(text = title, fontSize = 15.sp)
        SubtitleText(text = if (layout == NowPlayingLayout.Bar) action else message)
      }
      if (layout == NowPlayingLayout.Strip) {
        Spacer(GlanceModifier.width(8.dp))
        PillButton(text = action, onClick = actions.openLibrary())
        Spacer(GlanceModifier.width(4.dp))
      }
    }
    NowPlayingLayout.Art,
    NowPlayingLayout.Hero,
    NowPlayingLayout.HeroTall,
    -> Column(
      modifier = GlanceModifier
        .fillMaxSize()
        .appWidgetBackground()
        .widgetSurface(GlanceTheme.colors.widgetBackground)
        .clickable(actions.openLibrary())
        .padding(14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      EmptyIcon(icon, 48.dp)
      Spacer(GlanceModifier.height(8.dp))
      TitleText(text = title, fontSize = 16.sp)
      if (layout != NowPlayingLayout.Art) {
        SubtitleText(text = message, maxLines = 2)
      }
      Spacer(GlanceModifier.height(10.dp))
      PillButton(text = action, onClick = actions.openLibrary())
    }
  }
}

@Composable
private fun EmptyIcon(
  @DrawableRes icon: Int,
  size: Dp,
) {
  Box(modifier = GlanceModifier.width(size).height(size), contentAlignment = Alignment.Center) {
    WidgetIcon(icon = R.drawable.widget_dashed_circle, size = size, color = GlanceTheme.colors.outline)
    WidgetIcon(icon = icon, size = size * 0.45F, color = GlanceTheme.colors.primary)
  }
}

internal val WIDGET_RADIUS_BEFORE_S = 16.dp
private val WhiteTrack = ColorProvider(Color.White.copy(alpha = 0.4F))
