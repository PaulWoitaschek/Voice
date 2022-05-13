package voice.migration.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
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
import java.time.Instant
import kotlin.random.Random


@Composable
internal fun Migration(viewState: MigrationViewState) {
  Scaffold(
    topBar = {
      SmallTopAppBar(
        title = {
          Text("Old Books")
        },
        navigationIcon = {
          IconButton(
            onClick = {}
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
    Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      RootLabeledValue("Name") {
        Text(text = item.name)
      }
      RootLabeledValue("Current Chapter") {
        Text(text = item.currentChapter)
      }
      RootLabeledValue("Position in Chapter") {
        Text(text = item.positionInChapterMs.toString())
      }
      RootLabeledValue("Root") {
        Text(text = item.root)
      }
      if (item.bookmarks.isNotEmpty()) {
        RootLabeledValue("Bookmarks") {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item.bookmarks.forEach { bookmark ->
              if (bookmark.title != null) {
                NestedLabeledValue("Bookmark Title") {
                  Text(bookmark.title)
                }
              }
              NestedLabeledValue("Chapter") {
                Text(bookmark.chapter)
              }
              NestedLabeledValue("Position") {
                Text(bookmark.positionMs.toString())
              }
              NestedLabeledValue("Added at") {
                Text(bookmark.addedAt.toString())
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun RootLabeledValue(label: String, value: @Composable () -> Unit) {
  Column {
    Text(text = label, style = MaterialTheme.typography.titleLarge)
    value()
  }
}

@Composable
private fun NestedLabeledValue(label: String, value: @Composable () -> Unit) {
  Column(Modifier.padding(horizontal = 16.dp)) {
    Text(text = label, style = MaterialTheme.typography.titleMedium)
    value()
  }
}


@Preview
@Composable
private fun MigrationPreview(
  @PreviewParameter(MigrationViewStatePreviewProvider::class)
  viewState: MigrationViewState
) {
  VoiceTheme {
    Migration(viewState)
  }
}

internal class MigrationViewStatePreviewProvider : PreviewParameterProvider<MigrationViewState> {

  override val values: Sequence<MigrationViewState>
    get() = sequence {
      fun item() = MigrationViewState.Item(
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
        root = "Root"
      )
      yield(MigrationViewState(listOf(item(), item())))
    }
}
