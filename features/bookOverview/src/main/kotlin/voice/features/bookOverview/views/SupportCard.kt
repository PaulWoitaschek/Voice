@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import voice.core.strings.R as StringsR
import voice.core.ui.CoverTheme
import voice.core.ui.R as UiR
import voice.core.ui.ShapedIcon
import voice.core.ui.VoiceTheme
import voice.core.ui.bookText
import voice.core.ui.drawConfetti
import voice.core.ui.rememberConfettiState
import voice.core.ui.rememberCoverThumbnailRequest
import voice.features.bookOverview.overview.SupportCardViewState

@Composable
internal fun SupportCard(
  state: SupportCardViewState,
  onClick: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  when (state) {
    is SupportCardViewState.Letter -> LetterCard(state, onClick, onDismiss, modifier)
    is SupportCardViewState.Book -> BookCard(state, onClick, onDismiss, modifier)
  }
}

@Composable
private fun LetterCard(
  state: SupportCardViewState.Letter,
  onClick: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.extraLargeIncreased,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Column(Modifier.padding(24.dp)) {
      Icon(
        painter = painterResource(UiR.drawable.ic_format_quote),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(32.dp),
      )
      Spacer(Modifier.height(8.dp))
      Text(
        text =
          pluralStringResource(
            StringsR.plurals.review_prompt_milestone_title,
            state.hoursListened,
            state.hoursListened,
          ),
        style = MaterialTheme.typography.titleLargeEmphasized,
        modifier = Modifier.semantics { heading() },
      )
      Spacer(Modifier.height(8.dp))
      Text(
        text = stringResource(StringsR.string.support_card_letter_message),
        style = MaterialTheme.typography.bodyLarge,
      )
      Spacer(Modifier.height(8.dp))
      Text(
        text = stringResource(StringsR.string.support_note_signature),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.End,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(16.dp))
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(onClick = onClick, shapes = ButtonDefaults.shapes()) {
          Icon(
            painter = painterResource(UiR.drawable.ic_emoji_food_beverage),
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
          )
          Spacer(Modifier.width(ButtonDefaults.IconSpacing))
          Text(stringResource(StringsR.string.support_card_letter_action))
        }
        TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
          Text(stringResource(StringsR.string.library_survey_dismiss))
        }
      }
    }
  }
}

@Composable
private fun BookCard(
  state: SupportCardViewState.Book,
  onClick: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  CoverTheme(cover = state.cover) {
    val colors = MaterialTheme.colorScheme
    // the same tint as the Continue listening card, which stays legible on bright covers
    val tint = if (colors.surface.luminance() < 0.5F) 0.35F else 0.85F
    val confetti = rememberConfettiState()
    // one time, not each time the card scrolls back in
    var celebrated by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(confetti) {
      if (!celebrated) {
        celebrated = true
        confetti.burst()
      }
    }
    val confettiColors = listOf(colors.primary, colors.tertiary, colors.secondary)
    Surface(
      modifier = modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.extraLargeIncreased,
      color = colors.surfaceContainerHigh,
      contentColor = colors.onSurface,
    ) {
      Column(
        modifier =
          Modifier.background(
              Brush.verticalGradient(
                listOf(
                  colors.tertiaryContainer.copy(alpha = tint),
                  colors.primaryContainer.copy(alpha = tint),
                ),
              ),
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
          modifier =
            Modifier.padding(top = 8.dp).size(112.dp).drawWithContent {
              drawContent()
              drawConfetti(
                state = confetti,
                center = center,
                radius = size.minDimension / 2,
                colors = confettiColors,
              )
            },
        ) {
          AsyncImage(
            modifier = Modifier.fillMaxSize().rotate(-4F).clip(CoverShape),
            model = rememberCoverThumbnailRequest(state.cover),
            placeholder = ColorPainter(colors.surfaceContainerHighest),
            error = painterResource(id = UiR.drawable.album_art),
            contentScale = ContentScale.Crop,
            contentDescription = null,
          )
          ShapedIcon(
            icon = UiR.drawable.ic_favorite,
            shape = MaterialShapes.SoftBurst,
            containerColor = colors.tertiary,
            contentColor = colors.onTertiary,
            size = 44.dp,
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 14.dp, y = 10.dp),
          )
        }
        Spacer(Modifier.height(24.dp))
        Text(
          text = stringResource(StringsR.string.review_prompt_finished_title),
          style = MaterialTheme.typography.labelLargeEmphasized,
          color = colors.onPrimaryContainer,
          textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
          text = state.name,
          style =
            MaterialTheme.typography.headlineSmallEmphasized
              .bookText()
              .copy(hyphens = Hyphens.Auto),
          textAlign = TextAlign.Center,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(12.dp))
        Text(
          text = stringResource(StringsR.string.support_card_book_message),
          style = MaterialTheme.typography.bodyLarge,
          textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(
          onClick = onClick,
          shapes = ButtonDefaults.shapes(),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = stringResource(StringsR.string.settings_support_support_voice_title),
            textAlign = TextAlign.Center,
          )
        }
        TextButton(
          onClick = onDismiss,
          shapes = ButtonDefaults.shapes(),
          colors = ButtonDefaults.textButtonColors(contentColor = colors.onPrimaryContainer),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = stringResource(StringsR.string.library_survey_dismiss),
            textAlign = TextAlign.Center,
          )
        }
      }
    }
  }
}

@Composable
@PreviewLightDark
private fun LetterCardPreview() {
  VoiceTheme {
    SupportCard(SupportCardViewState.Letter(hoursListened = 42), onClick = {}, onDismiss = {})
  }
}

@Composable
@PreviewLightDark
private fun BookCardPreview() {
  VoiceTheme {
    SupportCard(
      SupportCardViewState.Book(name = "The Hitchhiker's Guide to the Galaxy", cover = null),
      onClick = {},
      onDismiss = {},
    )
  }
}
