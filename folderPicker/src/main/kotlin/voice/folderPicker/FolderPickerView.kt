package voice.folderPicker


import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ListItem
import androidx.compose.material.contentColorFor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPickerView(
  viewState: FolderPickerViewState,
  onAddClick: () -> Unit,
  onDeleteClick: (Uri) -> Unit,
  onDismissExplanationCardClick: () -> Unit
) {
  Scaffold(
    topBar = {
      MediumTopAppBar(
        title = {
          Text(text = "Audioboook Folders")
        },
        navigationIcon = {
          IconButton(onClick = { /*TODO*/ }) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Close")
          }
        }
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        text = {
          Text(text = "Add")
        },
        onClick = onAddClick,
        icon = {
          Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
        }
      )
    }
  ) {
    LazyColumn {
      viewState.explanationCard?.let { text ->
        item {
          Material3Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp),
          ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
              Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
              )
              Spacer(modifier = Modifier.size(16.dp))
              Button(
                modifier = Modifier.align(End),
                onClick = onDismissExplanationCardClick
              ) {
                Text(text = "Got it")
              }
            }
          }
        }
      }
      item { Spacer(modifier = Modifier.size(16.dp)) }
      items(viewState.items) { item ->
        ListItem(
          trailing = {
            IconButton(
              onClick = {
                onDeleteClick(item.id)
              },
              content = {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
              }
            )
          }) {
          Text(text = item.name, style = MaterialTheme.typography.bodyLarge)
        }
      }
    }
  }
}

@Composable
fun Material3Card(
  modifier: Modifier = Modifier,
  backgroundColor: Color = MaterialTheme.colorScheme.surface,
  contentColor: Color = contentColorFor(backgroundColor),
  content: @Composable () -> Unit,
) {
  Surface(
    tonalElevation = 1.dp,
    shadowElevation = 1.dp,
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    color = backgroundColor,
    contentColor = contentColor,
    content = content
  )
}
