@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import voice.core.data.BookId
import voice.core.strings.R as StringsR
import voice.core.ui.R as UiR
import voice.core.ui.ShapedIcon
import voice.core.ui.bookText
import voice.core.ui.rememberAnimationClock

@Composable
internal fun NameTile(name: ListenerName) {
  val large = LocalDensity.current.fontScale >= 1.5F
  val colors = MaterialTheme.colorScheme
  // the tertiary container is a light tone in the dark theme, which glares as a whole tile
  val dark = colors.surface.luminance() < 0.5F
  val color =
    if (dark) {
      colors.tertiary.copy(alpha = 0.2F).compositeOver(colors.surfaceContainer)
    } else {
      colors.tertiaryContainer
    }
  Surface(
    shape = MaterialTheme.shapes.extraLargeIncreased,
    color = color,
    contentColor = if (dark) colors.onSurface else colors.onTertiaryContainer,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      Modifier.padding(start = 24.dp, end = 20.dp, top = 20.dp, bottom = 20.dp).semantics(
        mergeDescendants = true,
      ) {},
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(Modifier.weight(1F)) {
        val style =
          if (large) {
            MaterialTheme.typography.titleLargeEmphasized
          } else {
            MaterialTheme.typography.headlineMediumEmphasized
          }
        Text(
          text = name.text(),
          style = style.shrinkable().copy(hyphens = Hyphens.Auto),
          autoSize = ShrinkToFit(min = 16.sp, max = style.fontSize),
        )
        Spacer(Modifier.height(6.dp))
        Text(name.reason(), style = MaterialTheme.typography.bodyMedium)
      }
      // with large text, the sticker gives up its space first
      if (!large) {
        Spacer(Modifier.width(16.dp))
        NameSticker(
          name = name,
          color = if (dark) colors.tertiary else colors.onTertiaryContainer,
          contentColor = if (dark) colors.onTertiary else colors.tertiaryContainer,
        )
      }
    }
  }
}

// StepBased lets a word break. This shrinks until the text fits and no word breaks.
private data class ShrinkToFit(
  val min: TextUnit,
  val max: TextUnit,
) : TextAutoSize {
  override fun TextAutoSizeLayoutScope.getFontSize(
    constraints: Constraints,
    text: AnnotatedString,
  ): TextUnit {
    var size = max
    while (size > min) {
      val layout = performLayout(constraints, text, size)
      val wordBreaks =
        (0 until layout.lineCount - 1)
          .map { layout.getLineEnd(it) }
          .any { it in 1 until text.length && text[it - 1].inWord() && text[it].inWord() }
      if (!layout.hasVisualOverflow && !wordBreaks) break
      size = (size.value - 1).sp
    }
    return size
  }
}

// these scripts have no spaces, so a line can break between any two letters
private val scriptsWithoutSpaces =
  setOf(
    Character.UnicodeScript.HAN,
    Character.UnicodeScript.HIRAGANA,
    Character.UnicodeScript.KATAKANA,
    Character.UnicodeScript.THAI,
  )

private fun Char.inWord(): Boolean =
  isLetter() && Character.UnicodeScript.of(code) !in scriptsWithoutSpaces

// a line height in sp stays as it is when the text shrinks
private fun TextStyle.shrinkable(): TextStyle =
  copy(lineHeight = (lineHeight.value / fontSize.value).em)

@Composable
private fun NameSticker(
  name: ListenerName,
  color: Color,
  contentColor: Color,
) {
  val (icon, shape) =
    when (name) {
      is ListenerName.Audiomaniac -> UiR.drawable.ic_headphones to MaterialShapes.SoftBurst
      is ListenerName.MarathonListener -> UiR.drawable.ic_fast_forward to MaterialShapes.Pill
      is ListenerName.NightOwl -> UiR.drawable.ic_bedtime_filled to MaterialShapes.Cookie12Sided
      is ListenerName.EarlyBird -> UiR.drawable.ic_light_mode to MaterialShapes.Sunny
      ListenerName.Commuter -> UiR.drawable.ic_swap_horiz to MaterialShapes.Cookie4Sided
      is ListenerName.Juggler -> UiR.drawable.ic_collections_bookmark to MaterialShapes.Clover4Leaf
      is ListenerName.Speedster -> UiR.drawable.ic_speed to MaterialShapes.Flower
      is ListenerName.WeekendListener ->
        UiR.drawable.ic_emoji_food_beverage to MaterialShapes.Cookie7Sided
      ListenerName.CreatureOfHabit -> UiR.drawable.ic_schedule to MaterialShapes.Cookie6Sided
      is ListenerName.PartOfDay ->
        when (name.part) {
          DayPart.Afternoon -> UiR.drawable.ic_light_mode to MaterialShapes.Cookie9Sided
          DayPart.Evening -> UiR.drawable.ic_bedtime to MaterialShapes.Clover8Leaf
          DayPart.Morning,
          DayPart.Night -> UiR.drawable.ic_coffee to MaterialShapes.Cookie9Sided
        }
    }
  ShapedIcon(
    icon = icon,
    shape = shape,
    containerColor = color,
    contentColor = contentColor,
    size = 80.dp,
    shapeRotation = nameShapeRotation(),
  )
}

@Composable
internal fun nameShapeRotation(): Float {
  val clock = rememberAnimationClock(running = true)
  return clock.value * 9F
}

@Composable
internal fun WideFactTile(
  fact: WideFact,
  onBookClick: (BookId) -> Unit,
) {
  when (fact) {
    is WideFact.WhenYouListen -> WhenYouListenTile(fact)
    is WideFact.MostListened ->
      BookTile(
        book = fact.book,
        label = stringResource(StringsR.string.listening_stats_most_listened),
        line = listenedText(fact.listened),
        onBookClick = onBookClick,
      )
    is WideFact.Forecast ->
      BookTile(
        book = fact.book,
        label =
          stringResource(StringsR.string.listening_stats_forecast_left, listenedText(fact.left)),
        line = fact.dayText(),
        onBookClick = onBookClick,
      )
  }
}

private const val FIRST_HOUR = 5
private val PartGap = 8.dp
private val PartPadding = 4.dp
private val PartLabelMinSize = 11.sp

@Composable
private fun WhenYouListenTile(fact: WideFact.WhenYouListen) {
  val hours = List(24) { (it + FIRST_HOUR) % 24 }
  val top = fact.top
  Surface(
    shape = MaterialTheme.shapes.extraLargeIncreased,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
      Text(
        text = stringResource(StringsR.string.listening_stats_when),
        style = MaterialTheme.typography.titleLargeEmphasized,
        modifier = Modifier.semantics { heading() },
      )
      Spacer(Modifier.height(16.dp))
      Bars(
        bars = hours.map { fact.hours[it] },
        labels = hourLabels(hours),
        color = {
          if (hours[it] in top.hours) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3F)
          }
        },
        modifier = Modifier.height(88.dp),
      )
      Spacer(Modifier.height(16.dp))
      BoxWithConstraints {
        val measurer = rememberTextMeasurer()
        val style = MaterialTheme.typography.labelMedium.copy(fontSize = PartLabelMinSize)
        val widest =
          with(LocalDensity.current) {
            DayPart.entries.maxOf { measurer.measure(it.label(), style).size.width }.toDp()
          }
        // as many in a row as leave room for the longest label, like "Nachmittag"
        val inRow =
          listOf(4, 2).firstOrNull {
            widest <= (maxWidth - PartGap * (it - 1)) / it - PartPadding * 2
          } ?: 1
        Column(verticalArrangement = Arrangement.spacedBy(PartGap)) {
          DayPart.entries.chunked(inRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(PartGap)) {
              row.forEach { part ->
                DayPartShare(part, fact.percent(part), top = part == top, Modifier.weight(1F))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DayPartShare(
  part: DayPart,
  percent: Int,
  top: Boolean,
  modifier: Modifier = Modifier,
) {
  val color =
    if (top) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceContainerHighest
  Column(
    modifier
      .background(color, MaterialTheme.shapes.large)
      .padding(horizontal = PartPadding, vertical = 10.dp)
      .semantics(mergeDescendants = true) {},
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    val content =
      if (top) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Text(
      text = percentText(percent),
      style = MaterialTheme.typography.titleMediumEmphasized,
      color = content,
    )
    Text(
      text = part.label(),
      style = MaterialTheme.typography.labelMedium,
      color = content,
      maxLines = 1,
      autoSize =
        TextAutoSize.StepBased(
          minFontSize = PartLabelMinSize,
          maxFontSize = MaterialTheme.typography.labelMedium.fontSize,
        ),
    )
  }
}

@Composable
private fun BookTile(
  book: StatsBookViewState,
  label: String,
  line: String,
  onBookClick: (BookId) -> Unit,
) {
  val shape = MaterialTheme.shapes.extraLargeIncreased
  Surface(
    shape = shape,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier =
      Modifier.fillMaxWidth()
        .clip(shape)
        .then(
          if (book.inLibrary) {
            Modifier.clickable(role = Role.Button) { onBookClick(book.id) }
          } else {
            Modifier
          },
        ),
  ) {
    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
      BookCover(book, Modifier.size(80.dp))
      Spacer(Modifier.width(16.dp))
      Column(Modifier.weight(1F)) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = book.name,
          style = MaterialTheme.typography.titleLarge.bookText(),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = line,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
internal fun SmallFactTiles(
  facts: List<SmallFact>,
  kind: PeriodKind,
  current: Boolean,
) {
  if (facts.isEmpty()) return
  if (LocalDensity.current.fontScale >= 1.5F) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      facts.forEachIndexed { index, fact ->
        SmallFactTile(fact, kind, current, index, Modifier.fillMaxWidth())
      }
    }
  } else {
    Row(
      Modifier.height(IntrinsicSize.Min),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      facts.forEachIndexed { index, fact ->
        SmallFactTile(fact, kind, current, index, Modifier.weight(1F).fillMaxHeight())
      }
    }
  }
}

@Composable
private fun SmallFactTile(
  fact: SmallFact,
  kind: PeriodKind,
  current: Boolean,
  index: Int,
  modifier: Modifier = Modifier,
) {
  val (icon, shape) = fact.sticker()
  val color: Color
  val contentColor: Color
  if (index % 2 == 0) {
    color = MaterialTheme.colorScheme.secondaryContainer
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
  } else {
    color = MaterialTheme.colorScheme.primaryContainer
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
  }
  Surface(
    modifier = modifier,
    shape = MaterialTheme.shapes.extraLarge,
    color = color,
    contentColor = contentColor,
  ) {
    Column(Modifier.padding(20.dp).semantics(mergeDescendants = true) {}) {
      ShapedIcon(
        icon = icon,
        shape = shape,
        containerColor = contentColor,
        contentColor = color,
        size = 40.dp,
      )
      Spacer(Modifier.weight(1F).height(16.dp))
      val style = MaterialTheme.typography.headlineMediumEmphasized
      Text(
        text = fact.value(),
        style = style.shrinkable().copy(fontFamily = FontFamily.Default),
        autoSize = ShrinkToFit(min = 12.sp, max = style.fontSize),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = fact.label(kind, current),
        style = MaterialTheme.typography.bodyMedium,
      )
    }
  }
}

private fun SmallFact.sticker(): Pair<Int, RoundedPolygon> =
  when (this) {
    is SmallFact.Change -> UiR.drawable.ic_analytics to MaterialShapes.Cookie6Sided
    is SmallFact.LongestDay,
    is SmallFact.LongestMonth -> UiR.drawable.ic_star to MaterialShapes.Cookie9Sided
    is SmallFact.Saved -> UiR.drawable.ic_speed to MaterialShapes.Sunny
    is SmallFact.LongestStretch -> UiR.drawable.ic_hourglass_empty to MaterialShapes.Clover4Leaf
    is SmallFact.YourDay -> UiR.drawable.ic_favorite to MaterialShapes.Flower
    is SmallFact.Streak -> UiR.drawable.ic_auto_awesome to MaterialShapes.SoftBurst
  }
