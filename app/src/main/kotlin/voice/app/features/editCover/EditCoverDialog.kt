package voice.app.features.editCover

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.DialogSceneStrategy
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.launch
import voice.core.common.rootGraphAs
import voice.core.data.BookId
import voice.core.scanner.CoverSaver
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.core.strings.R as StringsR

@ContributesTo(AppScope::class)
interface EditCoverComponent {
  val coverSaver: CoverSaver
}

@ContributesTo(AppScope::class)
interface EditCoverDialogProvider {

  @Provides
  @IntoSet
  fun navEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.EditCover> { key, backStack ->
    NavEntry(key, metadata = DialogSceneStrategy.dialog()) {
      EditCoverDialog(key.cover, key.bookId) {
        backStack.removeLastOrNull()
      }
    }
  }
}

@Composable
fun EditCoverDialog(
  coverUri: Uri,
  bookId: BookId,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var cropOverlay: CropOverlay? by remember { mutableStateOf(null) }
  var imageWidth by remember { mutableIntStateOf(0) }
  var imageHeight by remember { mutableIntStateOf(0) }

  AlertDialog(
    onDismissRequest = { onDismiss() },
    title = { Text(text = context.getString(StringsR.string.cover)) },
    text = {
      Box {
        AsyncImage(
          model = coverUri,
          contentDescription = context.getString(StringsR.string.content_cover),
          modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged {
              imageWidth = it.width
              imageHeight = it.height
            },
        )

        if (imageWidth > 0 && imageHeight > 0) {
          AndroidView(
            modifier = Modifier
              .width(with(LocalDensity.current) { imageWidth.toDp() })
              .height(with(LocalDensity.current) { imageHeight.toDp() }),
            factory = { ctx ->
              CropOverlay(ctx).apply {
                selectionOn = true
                cropOverlay = this
              }
            },
          )
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          val rect = cropOverlay?.selectedRect
          if (rect != null && !rect.isEmpty) {
            scope.launch {
              val bitmap = context.imageLoader
                .execute(
                  ImageRequest.Builder(context)
                    .data(coverUri)
                    .transformations(
                      CropTransformation(
                        cropOverlay = cropOverlay!!,
                        sourceWidth = imageWidth,
                        sourceHeight = imageHeight,
                      ),
                    )
                    .build(),
                )
                .drawable?.toBitmap()

              if (bitmap != null) {
                rootGraphAs<EditCoverComponent>()
                  .coverSaver.save(bookId, bitmap)
              }
              onDismiss()
            }
          } else {
            onDismiss()
          }
        },
      ) {
        Text(text = context.getString(StringsR.string.dialog_confirm))
      }
    },
    dismissButton = {
      TextButton(onClick = { onDismiss() }) {
        Text(text = context.getString(android.R.string.cancel))
      }
    },
  )
}
