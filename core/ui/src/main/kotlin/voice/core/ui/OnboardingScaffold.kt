@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import voice.core.ui.icons.VoiceIcons
import kotlin.math.PI
import kotlin.math.sin
import voice.core.strings.R as StringsR

/** The steps of the first start. The progress wave on top of each of them counts them. */
enum class OnboardingStep {
  Welcome,
  Explanation,
  AddContent,
  Completion,
}

/**
 * The frame all onboarding steps share: the drifting aurora with its sparkles, a progress wave that
 * fills up step by step, a playful [hero] taking up the free space and below it the [title],
 * [subtitle], optional [details] and the [actions]. Everything floats in one after another, once.
 * The [hero] gets the seconds of the clock driving the aurora, so it can move along with it.
 *
 * The hero gives up space for large fonts. When even its minimum size doesn't fit, the screen
 * scrolls. On short, wide screens the hero sits beside the text.
 */
@Composable
fun OnboardingScaffold(
  step: OnboardingStep?,
  onBack: (() -> Unit)?,
  title: String,
  subtitle: String,
  hero: @Composable (clock: () -> Float) -> Unit,
  actions: @Composable ColumnScope.() -> Unit,
  modifier: Modifier = Modifier,
  details: @Composable ColumnScope.() -> Unit = {},
) {
  val clock = rememberAnimationClock(running = true)
  val seconds = { clock.value }
  var entered by rememberSaveable { mutableStateOf(false) }
  val entrance = rememberEntranceState(animate = !entered)
  LaunchedEffect(Unit) { entered = true }
  Box(modifier.fillMaxSize()) {
    AuroraBackground(
      clock = seconds,
      showStars = true,
      modifier = Modifier.fillMaxSize(),
    )
    Scaffold(
      containerColor = Color.Transparent,
      contentColor = MaterialTheme.colorScheme.onSurface,
      topBar = { OnboardingTopBar(step = step, onBack = onBack) },
    ) { contentPadding ->
      val text: @Composable () -> Unit = {
        OnboardingText(
          title = title,
          subtitle = subtitle,
          details = details,
          actions = actions,
          entrance = entrance,
        )
      }
      val entranceHero: @Composable () -> Unit = {
        Box(Modifier.entrance(entrance, 0), contentAlignment = Alignment.Center) {
          hero(seconds)
        }
      }
      BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth > maxHeight && maxHeight < 600.dp) {
          LandscapeLayout(contentPadding = contentPadding, hero = entranceHero, text = text)
        } else {
          PortraitLayout(contentPadding = contentPadding, hero = entranceHero, text = text)
        }
      }
    }
  }
}

/** A big, squishy button for the onboarding [OnboardingScaffold.actions]. */
@Composable
fun OnboardingButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  primary: Boolean = true,
  trailingArrow: Boolean = false,
) {
  val height = ButtonDefaults.MediumContainerHeight
  val content: @Composable RowScope.() -> Unit = {
    Text(text = text, style = ButtonDefaults.textStyleFor(height), textAlign = TextAlign.Center)
    if (trailingArrow) {
      Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(height)))
      NudgingArrow(Modifier.size(ButtonDefaults.iconSizeFor(height)))
    }
  }
  val buttonModifier = modifier
    .fillMaxWidth()
    .heightIn(min = height)
  val contentPadding = ButtonDefaults.contentPaddingFor(height, hasEndIcon = trailingArrow)
  if (primary) {
    Button(
      onClick = onClick,
      shapes = ButtonDefaults.shapes(),
      modifier = buttonModifier,
      contentPadding = contentPadding,
      content = content,
    )
  } else {
    OutlinedButton(
      onClick = onClick,
      shapes = ButtonDefaults.shapes(),
      modifier = buttonModifier,
      contentPadding = contentPadding,
      content = content,
    )
  }
}

/** An arrow that keeps nudging forward, as if it can't wait to get going. */
@Composable
private fun NudgingArrow(modifier: Modifier = Modifier) {
  val clock = rememberAnimationClock(running = true)
  val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
  Icon(
    modifier = modifier.graphicsLayer {
      // two quick hops, then a rest
      val phase = (clock.value % 1.6F) / 1.6F
      val hop = if (phase < 0.5F) sin(phase * 4 * PI.toFloat()).coerceAtLeast(0F) else 0F
      translationX = hop * 4.dp.toPx() * if (rtl) -1 else 1
      scaleX = if (rtl) -1F else 1F
    },
    imageVector = VoiceIcons.ArrowForward,
    contentDescription = null,
  )
}

@Composable
private fun OnboardingTopBar(
  step: OnboardingStep?,
  onBack: (() -> Unit)?,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
      .height(64.dp)
      .padding(horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
      if (onBack != null) {
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        IconButton(onClick = onBack) {
          Icon(
            modifier = Modifier.graphicsLayer { scaleX = if (rtl) -1F else 1F },
            imageVector = VoiceIcons.ArrowBack,
            contentDescription = stringResource(StringsR.string.common_action_close),
          )
        }
      }
    }
    Box(
      modifier = Modifier
        .weight(1F)
        .padding(horizontal = 16.dp),
      contentAlignment = Alignment.Center,
    ) {
      if (step != null) {
        OnboardingProgress(
          step = step,
          modifier = Modifier
            .widthIn(max = 220.dp)
            .fillMaxWidth(),
        )
      }
    }
    Spacer(Modifier.size(48.dp))
  }
}

/**
 * A thick progress wave. Entering a step, it springs on from the previous step's fill, so moving
 * forward feels like filling up a little more. Only once per step, not when coming back to it.
 */
@Composable
private fun OnboardingProgress(
  step: OnboardingStep,
  modifier: Modifier = Modifier,
) {
  val stepCount = OnboardingStep.entries.size
  val target = (step.ordinal + 1F) / stepCount
  var filled by rememberSaveable { mutableStateOf(false) }
  val progress = remember { Animatable(if (filled) target else step.ordinal.toFloat() / stepCount) }
  LaunchedEffect(progress) {
    if (!filled) {
      // lets the screen slide in first, so the fill doesn't happen off screen
      delay(250)
      progress.animateTo(target, spring(dampingRatio = 0.45F, stiffness = Spring.StiffnessVeryLow))
      filled = true
    }
  }
  val strokeWidth = with(LocalDensity.current) { 6.dp.toPx() }
  val stroke = remember(strokeWidth) { Stroke(width = strokeWidth, cap = StrokeCap.Round) }
  LinearWavyProgressIndicator(
    progress = { progress.value },
    modifier = modifier.height(14.dp),
    color = MaterialTheme.colorScheme.primary,
    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18F),
    stroke = stroke,
    trackStroke = stroke,
    amplitude = { 1F },
  )
}

@Composable
private fun OnboardingText(
  title: String,
  subtitle: String,
  details: @Composable ColumnScope.() -> Unit,
  actions: @Composable ColumnScope.() -> Unit,
  entrance: EntranceState,
) {
  Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier = Modifier
        .widthIn(max = 520.dp)
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .padding(top = 8.dp, bottom = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        modifier = Modifier.entrance(entrance, 1),
        text = title,
        style = MaterialTheme.typography.displaySmallEmphasized,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(12.dp))
      Text(
        modifier = Modifier.entrance(entrance, 2),
        text = subtitle,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Column(
        modifier = Modifier.entrance(entrance, 3),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = details,
      )
      Spacer(Modifier.height(32.dp))
      Column(
        modifier = Modifier
          .entrance(entrance, 4)
          .widthIn(max = 400.dp)
          .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = actions,
      )
    }
  }
}

@Composable
private fun PortraitLayout(
  contentPadding: PaddingValues,
  hero: @Composable () -> Unit,
  text: @Composable () -> Unit,
) {
  val layoutDirection = LocalLayoutDirection.current
  val scrollState = rememberScrollState()
  val scrollable = scrollState.maxValue > 0
  BoxWithConstraints(Modifier.fillMaxSize()) {
    val viewportHeight = constraints.maxHeight
    Layout(
      contents = listOf(hero, text),
      modifier = Modifier
        // without anything to scroll, dragging shouldn't stretch the screen
        .verticalScroll(scrollState, enabled = scrollable)
        .padding(
          start = contentPadding.calculateStartPadding(layoutDirection),
          end = contentPadding.calculateEndPadding(layoutDirection),
        ),
    ) { (heroMeasurables, textMeasurables), constraints ->
      val top = contentPadding.calculateTopPadding().roundToPx()
      val bottom = contentPadding.calculateBottomPadding().roundToPx()
      val width = constraints.maxWidth
      val textPlaceables = textMeasurables.map { it.measure(Constraints.fixedWidth(width)) }
      val textHeight = textPlaceables.sumOf { it.height }
      val heroHeight = (viewportHeight - top - bottom - textHeight)
        .coerceAtLeast(MIN_HERO_HEIGHT.roundToPx())
      val heroPlaceables = heroMeasurables.map { it.measure(Constraints.fixed(width, heroHeight)) }
      layout(width, top + heroHeight + textHeight + bottom) {
        heroPlaceables.forEach { it.place(0, top) }
        var y = top + heroHeight
        textPlaceables.forEach {
          it.place(0, y)
          y += it.height
        }
      }
    }
  }
}

@Composable
private fun LandscapeLayout(
  contentPadding: PaddingValues,
  hero: @Composable () -> Unit,
  text: @Composable () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxSize()
      .padding(contentPadding),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .weight(1F)
        .fillMaxHeight(),
      contentAlignment = Alignment.Center,
    ) {
      hero()
    }
    Column(
      modifier = Modifier
        .weight(1F)
        .fillMaxHeight()
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.Center,
    ) {
      text()
    }
  }
}

private val MIN_HERO_HEIGHT = 200.dp
