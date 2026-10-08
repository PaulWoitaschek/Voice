@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.cover

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import coil.compose.AsyncImage
import voice.core.ui.icons.VoiceIcons
import voice.features.cover.SelectCoverFromInternetViewModel.ViewState
import voice.features.cover.api.SearchResponse
import voice.core.strings.R as StringsR

@Composable
internal fun CoverContents(
  viewState: ViewState,
  contentPadding: PaddingValues,
  onCoverClick: (SearchResponse.ImageResult) -> Unit,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AnimatedContent(
    targetState = viewState,
    modifier = modifier,
    contentKey = { it::class },
    transitionSpec = { fadeIn() togetherWith fadeOut() },
    label = "coverContent",
  ) { state ->
    when (state) {
      is ViewState.Content -> CoverGrid(
        viewState = state,
        contentPadding = contentPadding,
        onCoverClick = onCoverClick,
        onRetry = onRetry,
      )
      is ViewState.Loading -> Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(contentPadding),
        contentAlignment = Alignment.Center,
      ) {
        LoadingIndicator(Modifier.size(64.dp))
      }
      is ViewState.Idle -> Message(
        contentPadding = contentPadding,
        icon = VoiceIcons.ImageSearch,
        shape = MaterialShapes.Clover4Leaf,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        title = stringResource(StringsR.string.cover_search_hint),
      )
      is ViewState.Empty -> Message(
        contentPadding = contentPadding,
        icon = VoiceIcons.SearchOff,
        shape = MaterialShapes.Flower,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        title = stringResource(StringsR.string.cover_search_no_results_title),
        message = stringResource(StringsR.string.cover_search_no_results_message),
      )
      is ViewState.Error -> Message(
        contentPadding = contentPadding,
        icon = VoiceIcons.CloudOff,
        shape = MaterialShapes.SoftBurst,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        title = stringResource(StringsR.string.common_error_generic_message),
      ) {
        RetryButton(onRetry)
      }
    }
  }
}

@Composable
private fun CoverGrid(
  viewState: ViewState.Content,
  contentPadding: PaddingValues,
  onCoverClick: (SearchResponse.ImageResult) -> Unit,
  onRetry: () -> Unit,
) {
  val items = viewState.items
  LazyVerticalStaggeredGrid(
    columns = StaggeredGridCells.Adaptive(minSize = 150.dp),
    modifier = Modifier.fillMaxSize(),
    contentPadding = contentPadding,
    verticalItemSpacing = 8.dp,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items(count = items.itemCount) { index ->
      val item = items[index]
      if (item != null) {
        val downloading = viewState.downloading == item
        CoverTile(
          item = item,
          downloading = downloading,
          dimmed = viewState.downloading != null && !downloading,
          onClick = { onCoverClick(item) },
        )
      }
    }
    if (viewState.loadingMore || viewState.loadingMoreFailed) {
      item(span = StaggeredGridItemSpan.FullLine) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
          contentAlignment = Alignment.Center,
        ) {
          if (viewState.loadingMoreFailed) {
            RetryButton(onRetry)
          } else {
            LoadingIndicator()
          }
        }
      }
    }
  }
}

@Composable
private fun CoverTile(
  item: SearchResponse.ImageResult,
  downloading: Boolean,
  dimmed: Boolean,
  onClick: () -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (pressed || downloading) 0.94F else 1F,
    animationSpec = spring(dampingRatio = 0.5F, stiffness = Spring.StiffnessMedium),
    label = "coverScale",
  )
  val alpha by animateFloatAsState(
    targetValue = if (dimmed) 0.38F else 1F,
    label = "coverAlpha",
  )
  val aspectRatio = if (item.width > 0 && item.height > 0) {
    (item.width.toFloat() / item.height).coerceIn(0.5F, 2F)
  } else {
    1F
  }
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .aspectRatio(aspectRatio)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
      }
      .clip(RoundedCornerShape(20.dp))
      .background(colors.surfaceContainerHighest)
      .clickable(
        interactionSource = interactionSource,
        indication = ripple(),
        enabled = !dimmed && !downloading,
        role = Role.Button,
        onClick = onClick,
      ),
  ) {
    AsyncImage(
      model = item.thumbnail,
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.matchParentSize(),
    )
    if (item.width > 0 && item.height > 0) {
      Text(
        text = "${item.width} × ${item.height}",
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(8.dp)
          .background(colors.surfaceContainerHighest.copy(alpha = 0.85F), CircleShape)
          .padding(horizontal = 8.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        color = colors.onSurface,
      )
    }
    AnimatedVisibility(
      visible = downloading,
      modifier = Modifier.matchParentSize(),
      enter = fadeIn(),
      exit = fadeOut(),
    ) {
      Box(
        modifier = Modifier.background(Color.Black.copy(alpha = 0.32F)),
        contentAlignment = Alignment.Center,
      ) {
        ContainedLoadingIndicator()
      }
    }
  }
}

/** A shape that springs in and settles, with what there is to say about the search below it. */
@Composable
private fun Message(
  contentPadding: PaddingValues,
  icon: ImageVector,
  shape: RoundedPolygon,
  containerColor: Color,
  contentColor: Color,
  title: String,
  message: String? = null,
  action: (@Composable () -> Unit)? = null,
) {
  val appear = remember { Animatable(0F) }
  LaunchedEffect(Unit) {
    appear.animateTo(1F, spring(dampingRatio = 0.45F, stiffness = Spring.StiffnessLow))
  }
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(contentPadding)
      .padding(horizontal = 16.dp, vertical = 32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Box(
      modifier = Modifier
        .size(128.dp)
        .graphicsLayer {
          val scale = 0.6F + 0.4F * appear.value
          scaleX = scale
          scaleY = scale
        },
      contentAlignment = Alignment.Center,
    ) {
      Box(
        Modifier
          .matchParentSize()
          .graphicsLayer { rotationZ = (1F - appear.value) * -45F }
          .background(containerColor, shape.toShape()),
      )
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(52.dp),
        tint = contentColor,
      )
    }
    Text(
      text = title,
      modifier = Modifier.fillMaxWidth(),
      style = MaterialTheme.typography.headlineSmallEmphasized,
      textAlign = TextAlign.Center,
    )
    if (message != null) {
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
    if (action != null) {
      Spacer(Modifier.size(8.dp))
      action()
    }
  }
}

@Composable
private fun RetryButton(onClick: () -> Unit) {
  FilledTonalButton(
    onClick = onClick,
    shapes = ButtonDefaults.shapes(),
  ) {
    Icon(
      imageVector = VoiceIcons.Replay,
      contentDescription = null,
      modifier = Modifier.size(ButtonDefaults.IconSize),
    )
    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
    Text(stringResource(StringsR.string.common_error_generic_retry))
  }
}
