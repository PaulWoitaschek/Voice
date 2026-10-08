@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.cover.crop

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.DialogSceneStrategy
import coil.imageLoader
import coil.request.ImageRequest
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.launch
import voice.core.common.rootGraphAs
import voice.core.data.BookId
import voice.core.scanner.CoverSaver
import voice.core.ui.ShapedIcon
import voice.core.ui.icons.VoiceIcons
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.navigation.Navigator
import voice.core.strings.R as StringsR

@ContributesTo(AppScope::class)
interface EditCoverComponent {
  val coverSaver: CoverSaver
}

@BindingContainer
@ContributesTo(AppScope::class)
object EditCoverDialogProvider {

  @Provides
  @IntoSet
  fun editCoverDialogNavEntryProvider(navigator: Navigator): NavEntryProvider<*> = NavEntryProvider<Destination.EditCover> { key ->
    NavEntry(key, metadata = DialogSceneStrategy.dialog()) {
      EditCoverDialog(coverUri = key.cover, bookId = key.bookId, onDismiss = navigator::goBack)
    }
  }
}

@Composable
fun EditCoverDialog(
  coverUri: Uri,
  bookId: BookId,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var crop: Rect? by remember { mutableStateOf(null) }
  var saving by remember { mutableStateOf(false) }
  val colors = MaterialTheme.colorScheme
  Surface(
    modifier = modifier,
    shape = MaterialTheme.shapes.extraLarge,
    color = colors.surfaceContainerHigh,
  ) {
    Column(Modifier.padding(24.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        ShapedIcon(
          icon = VoiceIcons.Crop,
          shape = MaterialShapes.Cookie4Sided,
          containerColor = colors.primaryContainer,
          contentColor = colors.onPrimaryContainer,
        )
        Spacer(Modifier.width(16.dp))
        Column {
          Text(
            text = stringResource(StringsR.string.cover_crop_title),
            style = MaterialTheme.typography.headlineSmallEmphasized,
          )
          Text(
            text = stringResource(StringsR.string.cover_crop_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
          )
        }
      }
      Spacer(Modifier.size(20.dp))
      CoverCropper(
        cover = coverUri,
        onCropChange = { crop = it },
        // shrinks to leave room for the buttons on short screens
        modifier = Modifier
          .weight(1F, fill = false)
          .heightIn(max = 420.dp),
      )
      Spacer(Modifier.size(24.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.weight(1F))
        TextButton(
          onClick = onDismiss,
          enabled = !saving,
        ) {
          Text(stringResource(StringsR.string.common_dialog_cancel))
        }
        Spacer(Modifier.width(8.dp))
        Button(
          onClick = {
            val selected = crop ?: return@Button
            saving = true
            scope.launch {
              val bitmap = context.imageLoader
                .execute(
                  ImageRequest.Builder(context)
                    .data(coverUri)
                    .transformations(CropTransformation(selected))
                    .build(),
                )
                .drawable?.toBitmap()
              if (bitmap != null) {
                rootGraphAs<EditCoverComponent>().coverSaver.save(bookId, bitmap)
              }
              onDismiss()
            }
          },
          enabled = crop != null && !saving,
          shapes = ButtonDefaults.shapes(),
        ) {
          if (saving) {
            LoadingIndicator(Modifier.size(ButtonDefaults.IconSize))
          } else {
            Icon(VoiceIcons.Check, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
          }
          Spacer(Modifier.width(ButtonDefaults.IconSpacing))
          Text(stringResource(StringsR.string.common_action_save))
        }
      }
    }
  }
}
