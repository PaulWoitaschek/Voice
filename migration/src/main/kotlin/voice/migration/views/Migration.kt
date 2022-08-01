package voice.migration.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.squareup.anvil.annotations.ContributesTo
import voice.common.AppScope
import voice.common.compose.VoiceTheme
import voice.common.compose.plus
import voice.common.compose.rememberScoped
import voice.common.formatTime
import voice.common.rootComponentAs
import voice.migration.MigrationViewModel
import voice.migration.R
import java.time.Instant
import kotlin.random.Random

@ContributesTo(AppScope::class)
interface MigrationComponent {
  val migrationViewModel: MigrationViewModel
}

@Composable
fun Migration() {
  val viewModel = rememberScoped { rootComponentAs<MigrationComponent>().migrationViewModel }
  val viewState = viewModel.viewState()
  Migration(
    viewState = viewState,
    onCloseClicked = {
      viewModel.onCloseClick()
    },
  )
}

@Composable
internal fun Migration(
  viewState: MigrationViewState,
  onCloseClicked: () -> Unit,
) {
  Scaffold(
    topBar = {
      SmallTopAppBar(
        title = {
          Text(stringResource(id = R.string.migration_detail_title))
        },
        navigationIcon = {
          IconButton(
            onClick = onCloseClicked,
          ) {
            Icon(
              imageVector = Icons.Outlined.Close,
              contentDescription = stringResource(R.string.close),
            )
          }
        },
      )
    },
    floatingActionButton = {
      FloatingActionButton(onClick = viewState.onDeleteClicked) {
        Icon(
          imageVector = Icons.Outlined.Delete,
          contentDescription = stringResource(id = R.string.delete),
        )
      }
    },
  ) { contentPadding ->
    LazyColumn(
      contentPadding = contentPadding + PaddingValues(horizontal = 16.dp, vertical = 24.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(viewState.items) { item ->
        MigrationItem(item)
      }
    }
    if (viewState.showDeletionConfirmationDialog) {
      DeletionConfirmationDialog(
        onCancel = viewState.onDeletionAborted,
        onConfirm = viewState.onDeletionConfirmed,
      )
    }
  }
}

@Composable
private fun DeletionConfirmationDialog(
  onCancel: () -> Unit,
  onConfirm: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onCancel,
    title = {
      Text(stringResource(R.string.migration_delete_dialog_title))
    },
    text = {
      Text(stringResource(R.string.migration_delete_dialog_content))
    },
    confirmButton = {
      Button(onConfirm) {
        Text(stringResource(R.string.migration_delete_dialog_action_delete))
      }
    },
    dismissButton = {
      TextButton(onConfirm) {
        Text(stringResource(R.string.migration_delete_dialog_action_keep))
      }
    },
  )
}

@Composable
private fun MigrationItem(item: MigrationViewState.Item) {
  Card(
    Modifier.fillMaxWidth(),
  ) {
    Column(
      Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      LabeledValue(
        label = stringResource(id = R.string.migration_detail_content_name),
        value = item.name,
      )
      LabeledValue(
        label = stringResource(id = R.string.migration_detail_content_root),
        value = item.root,
      )
      Column {
        Text(
          text = stringResource(id = R.string.migration_detail_content_position),
          style = MaterialTheme.typography.titleMedium,
        )
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
          Position(item.position)
        }
      }
      if (item.bookmarks.isNotEmpty()) {
        Text(
          text = stringResource(id = R.string.migration_detail_content_bookmarks),
          style = MaterialTheme.typography.titleMedium,
        )
        Column(
          modifier = Modifier.padding(horizontal = 16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          item.bookmarks.forEach { bookmark ->
            if (bookmark.title != null) {
              LabeledValue(
                label = stringResource(id = R.string.migration_detail_title),
                value = bookmark.title,
              )
            }
            Column(Modifier.padding(horizontal = 16.dp)) {
              LabeledValue(
                label = stringResource(id = R.string.migration_detail_content_added_at),
                value = bookmark.addedAt.toString(),
              )
              Spacer(modifier = Modifier.height(16.dp))
              Position(bookmark.position)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LabeledValue(label: String, value: String) {
  Column {
    Text(text = label, style = MaterialTheme.typography.titleMedium)
    Text(value)
  }
}

@Composable
private fun Position(viewState: MigrationViewState.Position) {
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    LabeledValue(
      label = stringResource(id = R.string.migration_detail_content_position_current_file_title),
      value = viewState.currentFile,
    )
    LabeledValue(
      label = stringResource(id = R.string.migration_detail_content_position_current_file_position),
      value = viewState.positionInFile,
    )
    LabeledValue(
      label = stringResource(id = R.string.migration_detail_content_position_current_chapter_title),
      value = viewState.currentChapter,
    )
    LabeledValue(
      label = stringResource(id = R.string.migration_detail_content_position_current_chapter_position),
      value = viewState.positionInChapter,
    )
  }
}

@Composable
@Preview
private fun MigrationPreview(
  @PreviewParameter(MigrationViewStatePreviewProvider::class)
  viewState: MigrationViewState,
) {
  VoiceTheme {
    Migration(viewState, onCloseClicked = {})
  }
}

internal class MigrationViewStatePreviewProvider : PreviewParameterProvider<MigrationViewState> {

  override val values: Sequence<MigrationViewState>
    get() = sequence {
      fun position(): MigrationViewState.Position {
        return MigrationViewState.Position(
          currentChapter = "Current Chapter",
          positionInChapter = formatTime(50000),
          currentFile = "audiobooks/file.mp3",
          positionInFile = formatTime(70000),
        )
      }

      fun item() = MigrationViewState.Item(
        name = "My Book",
        bookmarks = buildList {
          repeat(3) {
            add(
              MigrationViewState.Item.Bookmark(
                position = position(),
                addedAt = Instant.now(),
                title = "Bookmark $it".takeIf { Random.nextBoolean() },
              ),
            )
          }
        },
        root = "Root",
        position = position(),
      )
      yield(
        MigrationViewState(
          items = listOf(item(), item()),
          onDeleteClicked = {},
          showDeletionConfirmationDialog = false,
          onDeletionConfirmed = {},
          onDeletionAborted = {},
        ),
      )
    }
}
