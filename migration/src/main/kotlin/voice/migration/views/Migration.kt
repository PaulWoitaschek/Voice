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
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import voice.common.compose.VoiceTheme
import voice.common.compose.plus
import voice.migration.R


@Composable
internal fun Migration(
  viewState: MigrationViewState,
  onCloseClicked: () -> Unit,
) {
  Scaffold(
    topBar = {
      SmallTopAppBar(
        title = {
          Text("Old Books")
        },
        navigationIcon = {
          IconButton(
            onClick = onCloseClicked
          ) {
            Icon(
              imageVector = Icons.Outlined.Close,
              contentDescription = stringResource(R.string.close)
            )
          }
        }
      )
    }
  ) { contentPadding ->
    LazyColumn(
      contentPadding = contentPadding + PaddingValues(horizontal = 16.dp, vertical = 24.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(viewState.items) { item ->
        MigrationItem(item)
      }
    }
  }
}

@Composable
private fun MigrationItem(item: MigrationViewState.Item) {
  Card(
    Modifier.fillMaxWidth()
  ) {
    Column(
      Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      LabeledValue("Name", item.name)
      LabeledValue("Root Folder", item.root)
      Column {
        Text("Position", style = MaterialTheme.typography.titleMedium)
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
          Position(item.position)
        }
      }
      if (item.bookmarks.isNotEmpty()) {
        Text(text = "Bookmarks", style = MaterialTheme.typography.titleMedium)
        Column(
          modifier = Modifier.padding(horizontal = 16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          item.bookmarks.forEach { bookmark ->
            if (bookmark.title != null) {
              LabeledValue("Title", bookmark.title)
            }
            Column(Modifier.padding(horizontal = 16.dp)) {
              LabeledValue("Added at", bookmark.addedAt.toString())
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
    LabeledValue("Current File", viewState.currentFile)
    LabeledValue("Position in File", viewState.positionInFile)
    LabeledValue("Current Chapter", viewState.currentChapter)
    LabeledValue("Position in Chapter", viewState.positionInChapter)
  }
}

@Composable
@Preview
private fun MigrationPreview(
  @PreviewParameter(MigrationViewStatePreviewProvider::class)
  viewState: MigrationViewState
) {
  VoiceTheme {
    Migration(viewState, {})
  }
}

internal class MigrationViewStatePreviewProvider : PreviewParameterProvider<MigrationViewState> {

  override val values: Sequence<MigrationViewState>
    get() = sequence {
      /*    fun item() = MigrationViewState.Item( todo
            name = "My Book",
            currentChapter = "Current Chapter",
            bookmarks = buildList {
              repeat(3) {
                add(
                  MigrationViewState.Item.Bookmark(
                    chapter = "Chapter $it",
                    addedAt = Instant.now(),
                    positionMs = 100,
                    title = "Bookmark $it".takeIf { Random.nextBoolean() }
                  ))
              }
            },
            positionInChapterMs = 500000,
            root = "Root",
            currentFile = "Current File",
            positionInFileMs = 1234L,
          )
          yield(MigrationViewState(listOf(item(), item())))*/
    }
}
