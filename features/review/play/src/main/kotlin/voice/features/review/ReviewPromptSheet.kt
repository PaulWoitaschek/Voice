@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.review

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import voice.core.common.FeedbackLinks
import voice.core.ui.ConfettiState
import voice.core.ui.EntranceState
import voice.core.ui.MorphShape
import voice.core.ui.ShapedIcon
import voice.core.ui.VoiceTheme
import voice.core.ui.drawConfetti
import voice.core.ui.entrance
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.rememberAnimationClock
import voice.core.ui.rememberConfettiState
import voice.core.ui.rememberEntranceState
import voice.core.ui.rememberFullSizeCoverRequest
import voice.core.ui.segmentedShape
import java.text.NumberFormat
import kotlin.math.roundToInt
import kotlin.math.sin
import voice.core.strings.R as StringsR
import voice.core.ui.R as UiR

internal enum class ReviewStep {
  Ask,
  Feedback,
  Thanks,
}

/**
 * Celebrates the listener's win and asks for a rating. Everyone gets the same choices: rating Voice,
 * sharing feedback or leaving it for now.
 *
 * [onAnswer] is called once, after the sheet is gone.
 */
@Composable
internal fun ReviewPromptSheet(
  prompt: ReviewPrompt,
  onAnswer: (ReviewAnswer) -> Unit,
) {
  val sheetState = rememberBottomSheetState(
    initialValue = Hidden,
    enabledValues = setOf(Hidden, Expanded),
  )
  val scope = rememberCoroutineScope()
  var step by rememberSaveable { mutableStateOf(ReviewStep.Ask) }
  var answered by remember { mutableStateOf(false) }
  val answer: (ReviewAnswer) -> Unit = { answer ->
    if (!answered) {
      answered = true
      scope.launch {
        try {
          sheetState.hide()
        } finally {
          onAnswer(answer)
        }
      }
    }
  }
  if (step == ReviewStep.Thanks) {
    LaunchedEffect(Unit) {
      // a moment for the confetti before Google Play takes over
      delay(1400)
      answer(ReviewAnswer.Rate)
    }
  }
  ModalBottomSheet(
    onDismissRequest = {
      if (!answered) {
        answered = true
        // once thanked, the rating is on its way
        onAnswer(if (step == ReviewStep.Thanks) ReviewAnswer.Rate else ReviewAnswer.Later)
      }
    },
    sheetState = sheetState,
  ) {
    ReviewPromptContent(
      prompt = prompt,
      step = step,
      onStep = { step = it },
      onAnswer = answer,
    )
  }
}

@Composable
private fun ReviewPromptContent(
  prompt: ReviewPrompt,
  step: ReviewStep,
  onStep: (ReviewStep) -> Unit,
  onAnswer: (ReviewAnswer) -> Unit,
  modifier: Modifier = Modifier,
) {
  val entrance = rememberEntranceState()
  val confetti = rememberConfettiState()
  val colors = MaterialTheme.colorScheme
  val confettiColors = listOf(
    colors.primary,
    colors.secondary,
    colors.tertiary,
    colors.primaryContainer,
    colors.secondaryContainer,
    colors.tertiaryContainer,
  )
  var contentCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
  var heroCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
  // The confetti is drawn out here, as the scrolling column would cut it off right above the hero.
  Box(
    modifier = modifier
      .onPlaced { contentCoordinates = it }
      .drawWithContent {
        drawContent()
        val content = contentCoordinates?.takeIf { it.isAttached } ?: return@drawWithContent
        val hero = heroCoordinates?.takeIf { it.isAttached } ?: return@drawWithContent
        drawConfetti(
          state = confetti,
          center = content.localPositionOf(hero, hero.size.toSize().center),
          radius = BadgeSize.toPx() / 2,
          colors = confettiColors,
        )
      },
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp)
        .padding(bottom = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      ReviewHero(
        headline = prompt.headline,
        thanked = step == ReviewStep.Thanks,
        confetti = confetti,
        modifier = Modifier
          .entrance(entrance, index = 0)
          .fillMaxWidth()
          .height(196.dp)
          .onPlaced { heroCoordinates = it },
      )
      Spacer(Modifier.height(8.dp))
      AnimatedContent(
        targetState = step,
        transitionSpec = {
          val enter = fadeIn(tween(durationMillis = 220, delayMillis = 90)) +
            scaleIn(tween(durationMillis = 220, delayMillis = 90), initialScale = 0.92F)
          (enter togetherWith fadeOut(tween(durationMillis = 90)))
            .using(SizeTransform(clip = false))
        },
        label = "reviewStep",
      ) { step ->
        when (step) {
          ReviewStep.Ask -> AskStep(
            prompt = prompt,
            entrance = entrance,
            onRate = { onStep(ReviewStep.Thanks) },
            onFeedback = { onStep(ReviewStep.Feedback) },
            onLater = { onAnswer(ReviewAnswer.Later) },
          )
          ReviewStep.Feedback -> FeedbackStep(
            onBack = { onStep(ReviewStep.Ask) },
            onAnswer = onAnswer,
          )
          ReviewStep.Thanks -> ThanksStep()
        }
      }
    }
  }
}

/**
 * A badge with the finished book's cover, or headphones for a milestone, that pops in on a turning
 * halo and throws confetti. Once thanked, it turns into a heart. Tapping it (just for fun, so it's
 * hidden from accessibility services) throws another handful.
 */
@Composable
private fun ReviewHero(
  headline: ReviewPrompt.Headline,
  thanked: Boolean,
  confetti: ConfettiState,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val haptics = LocalHapticFeedback.current
  val clock = rememberAnimationClock(running = true)
  val pop = remember { Animatable(0F) }
  LaunchedEffect(pop) {
    delay(150)
    launch { pop.animateTo(1F, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow)) }
    delay(250)
    confetti.burst()
  }
  LaunchedEffect(thanked) {
    if (thanked) {
      haptics.performHapticFeedback(HapticFeedbackType.Confirm)
      confetti.burst()
    }
  }
  val morph = remember { Morph(MaterialShapes.SoftBurst, MaterialShapes.Heart) }
  val heart by animateFloatAsState(
    targetValue = if (thanked) 1F else 0F,
    animationSpec = spring(dampingRatio = 0.45F, stiffness = Spring.StiffnessMediumLow),
    label = "heroHeart",
  )
  var pressed by remember { mutableStateOf(false) }
  val squish by animateFloatAsState(
    targetValue = if (pressed) 0.86F else 1F,
    animationSpec = spring(dampingRatio = 0.35F, stiffness = Spring.StiffnessMedium),
    label = "heroSquish",
  )
  Box(
    modifier = modifier
      .clearAndSetSemantics {}
      .pointerInput(Unit) {
        detectTapGestures(
          onPress = {
            pressed = true
            tryAwaitRelease()
            pressed = false
          },
          onTap = {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            confetti.burst()
          },
        )
      },
    contentAlignment = Alignment.Center,
  ) {
    Box(
      Modifier
        .size(HaloSize)
        .graphicsLayer {
          scaleX = pop.value
          scaleY = pop.value
          rotationZ = -clock.value * 6F
        }
        .background(colors.tertiaryContainer, MaterialShapes.Cookie12Sided.toShape()),
    )
    Box(
      modifier = Modifier
        .size(BadgeSize)
        .graphicsLayer {
          val scale = pop.value * squish * (1F + 0.025F * sin(clock.value * 2.4F))
          scaleX = scale
          scaleY = scale
          // spins in while popping up
          rotationZ = (1F - pop.value) * -90F
        }
        .clip(MorphShape(morph, heart))
        .background(colors.primary),
      contentAlignment = Alignment.Center,
    ) {
      when (headline) {
        is ReviewPrompt.Headline.BookFinished -> AsyncImage(
          modifier = Modifier.fillMaxSize(),
          model = rememberFullSizeCoverRequest(headline.cover),
          error = painterResource(UiR.drawable.album_art),
          contentScale = ContentScale.Crop,
          contentDescription = null,
        )
        ReviewPrompt.Headline.Milestone -> Icon(
          modifier = Modifier.size(BadgeSize * 0.42F),
          imageVector = VoiceIcons.Headphones,
          contentDescription = null,
          tint = colors.onPrimary,
        )
      }
    }
  }
}

@Composable
private fun AskStep(
  prompt: ReviewPrompt,
  entrance: EntranceState,
  onRate: () -> Unit,
  onFeedback: () -> Unit,
  onLater: () -> Unit,
) {
  val headline = prompt.headline
  val stats = prompt.stats.tiles(headline)
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
      modifier = Modifier.entrance(entrance, index = 1),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      if (headline is ReviewPrompt.Headline.BookFinished) {
        Text(
          text = headline.name,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
          textAlign = TextAlign.Center,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
      }
      Text(
        text = when (headline) {
          is ReviewPrompt.Headline.BookFinished -> stringResource(StringsR.string.review_prompt_finished_title)
          ReviewPrompt.Headline.Milestone -> pluralStringResource(
            StringsR.plurals.review_prompt_milestone_title,
            prompt.stats.listenedHours,
            prompt.stats.listenedHours,
          )
        },
        style = MaterialTheme.typography.headlineSmallEmphasized,
        textAlign = TextAlign.Center,
      )
    }
    Spacer(Modifier.height(8.dp))
    Text(
      modifier = Modifier.entrance(entrance, index = 2),
      text = stringResource(StringsR.string.review_prompt_message),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    if (stats.isNotEmpty()) {
      Spacer(Modifier.height(24.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        stats.forEachIndexed { index, stat ->
          StatTile(
            stat = stat,
            style = StatStyles[index % StatStyles.size],
            modifier = Modifier
              .weight(1F)
              .entrance(entrance, index = 3 + index),
          )
        }
      }
    }
    Spacer(Modifier.height(28.dp))
    Column(
      modifier = Modifier.entrance(entrance, index = 3 + stats.size),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      val size = ButtonDefaults.MediumContainerHeight
      Button(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(size),
        onClick = onRate,
        shapes = ButtonDefaults.shapes(),
        contentPadding = ButtonDefaults.contentPaddingFor(size, hasStartIcon = true),
      ) {
        Icon(
          imageVector = VoiceIcons.Star,
          contentDescription = null,
          modifier = Modifier.size(ButtonDefaults.iconSizeFor(size)),
        )
        Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
        Text(
          text = stringResource(StringsR.string.review_prompt_action_rate),
          style = ButtonDefaults.textStyleFor(size),
        )
      }
      Spacer(Modifier.height(8.dp))
      FilledTonalButton(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(size),
        onClick = onFeedback,
        shapes = ButtonDefaults.shapes(),
        contentPadding = ButtonDefaults.contentPaddingFor(size, hasStartIcon = true),
      ) {
        Icon(
          imageVector = VoiceIcons.Forum,
          contentDescription = null,
          modifier = Modifier.size(ButtonDefaults.iconSizeFor(size)),
        )
        Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
        Text(
          text = stringResource(StringsR.string.review_prompt_action_feedback),
          style = ButtonDefaults.textStyleFor(size),
        )
      }
      Spacer(Modifier.height(4.dp))
      TextButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = onLater,
        shapes = ButtonDefaults.shapes(),
      ) {
        Text(stringResource(StringsR.string.review_prompt_action_later))
      }
    }
  }
}

private data class Stat(
  val value: Int,
  @StringRes val label: Int,
)

/**
 * A milestone already has the hours in its title, and a zero is nothing to show off.
 */
private fun ReviewPrompt.Stats.tiles(headline: ReviewPrompt.Headline): List<Stat> = buildList {
  if (headline is ReviewPrompt.Headline.BookFinished) {
    add(Stat(listenedHours, StringsR.string.review_prompt_stat_hours))
  }
  if (finishedBooks > 0) add(Stat(finishedBooks, StringsR.string.review_prompt_stat_finished))
  if (authors > 0) add(Stat(authors, StringsR.string.review_prompt_stat_authors))
}

private data class StatStyle(
  val shape: RoundedPolygon,
  val container: @Composable () -> Color,
  val content: @Composable () -> Color,
)

private val StatStyles = listOf(
  StatStyle(
    shape = MaterialShapes.Cookie6Sided,
    container = { MaterialTheme.colorScheme.primaryContainer },
    content = { MaterialTheme.colorScheme.onPrimaryContainer },
  ),
  StatStyle(
    shape = MaterialShapes.Sunny,
    container = { MaterialTheme.colorScheme.tertiaryContainer },
    content = { MaterialTheme.colorScheme.onTertiaryContainer },
  ),
  StatStyle(
    shape = MaterialShapes.Cookie9Sided,
    container = { MaterialTheme.colorScheme.secondaryContainer },
    content = { MaterialTheme.colorScheme.onSecondaryContainer },
  ),
)

/** A number in a shape that counts up, with what it counts below. */
@Composable
private fun StatTile(
  stat: Stat,
  style: StatStyle,
  modifier: Modifier = Modifier,
) {
  val numberFormat = remember { NumberFormat.getIntegerInstance() }
  val label = stringResource(stat.label)
  val count = remember { Animatable(0F) }
  LaunchedEffect(stat.value) {
    delay(300)
    count.animateTo(stat.value.toFloat(), tween(durationMillis = 1000, easing = FastOutSlowInEasing))
  }
  Column(
    // the count up is just for show, so it reads the final number
    modifier = modifier.clearAndSetSemantics {
      contentDescription = "${numberFormat.format(stat.value)} $label"
    },
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier = Modifier
        .size(80.dp)
        .background(style.container(), style.shape.toShape()),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = numberFormat.format(count.value.roundToInt()),
        style = MaterialTheme.typography.titleLargeEmphasized,
        color = style.content(),
        maxLines = 1,
        softWrap = false,
      )
    }
    Spacer(Modifier.height(8.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

private class FeedbackOption(
  val answer: ReviewAnswer,
  val icon: ImageVector,
  val shape: RoundedPolygon,
  @StringRes val title: Int,
  val subtitle: @Composable () -> String,
)

private val FeedbackOptions = listOf(
  FeedbackOption(
    answer = ReviewAnswer.Idea,
    icon = VoiceIcons.Lightbulb,
    shape = MaterialShapes.Sunny,
    title = StringsR.string.review_prompt_feedback_idea_title,
    subtitle = { stringResource(StringsR.string.review_prompt_feedback_idea_subtitle) },
  ),
  FeedbackOption(
    answer = ReviewAnswer.Bug,
    icon = VoiceIcons.BugReport,
    shape = MaterialShapes.Cookie6Sided,
    title = StringsR.string.review_prompt_feedback_bug_title,
    subtitle = { stringResource(StringsR.string.review_prompt_feedback_bug_subtitle) },
  ),
  FeedbackOption(
    answer = ReviewAnswer.Email,
    icon = VoiceIcons.Mail,
    shape = MaterialShapes.Cookie9Sided,
    title = StringsR.string.review_prompt_feedback_email_title,
    subtitle = { FeedbackLinks.EMAIL },
  ),
)

@Composable
private fun FeedbackStep(
  onBack: () -> Unit,
  onAnswer: (ReviewAnswer) -> Unit,
) {
  Column(Modifier.fillMaxWidth()) {
    Box(
      modifier = Modifier.fillMaxWidth(),
      contentAlignment = Alignment.Center,
    ) {
      IconButton(
        modifier = Modifier.align(Alignment.CenterStart),
        onClick = onBack,
      ) {
        Icon(
          imageVector = VoiceIcons.ArrowBack,
          contentDescription = stringResource(StringsR.string.common_action_back),
        )
      }
      Text(
        modifier = Modifier.padding(horizontal = 48.dp),
        text = stringResource(StringsR.string.review_prompt_feedback_title),
        style = MaterialTheme.typography.headlineSmallEmphasized,
        textAlign = TextAlign.Center,
      )
    }
    Spacer(Modifier.height(16.dp))
    FeedbackOptions.forEachIndexed { index, option ->
      FeedbackRow(
        option = option,
        modifier = Modifier.padding(bottom = 2.dp),
        shape = segmentedShape(index, FeedbackOptions.size),
        onClick = { onAnswer(option.answer) },
      )
    }
  }
}

@Composable
private fun FeedbackRow(
  option: FeedbackOption,
  shape: Shape,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Surface(
    modifier = modifier.fillMaxWidth(),
    onClick = onClick,
    shape = shape,
    color = colors.surfaceContainerHigh,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ShapedIcon(
        icon = option.icon,
        shape = option.shape,
        containerColor = colors.secondaryContainer,
        contentColor = colors.onSecondaryContainer,
        size = 44.dp,
      )
      Spacer(Modifier.size(16.dp))
      Column(Modifier.weight(1F)) {
        Text(
          text = stringResource(option.title),
          style = MaterialTheme.typography.titleMedium,
        )
        Text(
          text = option.subtitle(),
          style = MaterialTheme.typography.bodyMedium,
          color = colors.onSurfaceVariant,
        )
      }
      Icon(
        imageVector = VoiceIcons.ChevronRight,
        contentDescription = null,
        tint = colors.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun ThanksStep() {
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = stringResource(StringsR.string.review_prompt_thanks_title),
      style = MaterialTheme.typography.headlineSmallEmphasized,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
      text = stringResource(StringsR.string.review_prompt_thanks_message),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    LoadingIndicator()
    Spacer(Modifier.height(24.dp))
  }
}

private val BadgeSize = 136.dp
private val HaloSize = 184.dp

private val previewStats = ReviewPrompt.Stats(listenedHours = 312, finishedBooks = 27, authors = 14)

@Composable
private fun ReviewPromptPreview(
  prompt: ReviewPrompt,
  step: ReviewStep,
) {
  VoiceTheme {
    Surface {
      ReviewPromptContent(prompt = prompt, step = step, onStep = {}, onAnswer = {})
    }
  }
}

@Composable
@Preview
private fun ReviewPromptBookFinishedPreview() {
  ReviewPromptPreview(
    prompt = ReviewPrompt(
      headline = ReviewPrompt.Headline.BookFinished(name = "The Left Hand of Darkness", cover = null),
      stats = previewStats,
      forced = false,
    ),
    step = ReviewStep.Ask,
  )
}

@Composable
@Preview
private fun ReviewPromptMilestonePreview() {
  ReviewPromptPreview(
    prompt = ReviewPrompt(headline = ReviewPrompt.Headline.Milestone, stats = previewStats, forced = false),
    step = ReviewStep.Ask,
  )
}

@Composable
@Preview
private fun ReviewPromptFeedbackPreview() {
  ReviewPromptPreview(
    prompt = ReviewPrompt(headline = ReviewPrompt.Headline.Milestone, stats = previewStats, forced = false),
    step = ReviewStep.Feedback,
  )
}

@Composable
@Preview
private fun ReviewPromptThanksPreview() {
  ReviewPromptPreview(
    prompt = ReviewPrompt(headline = ReviewPrompt.Headline.Milestone, stats = previewStats, forced = false),
    step = ReviewStep.Thanks,
  )
}
