package voice.folderPicker


import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ListItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPickerView(
  viewState: FolderPickerViewState,
  onAddClick: () -> Unit,
  onDeleteClick: (Uri) -> Unit,
  onDismissExplanationCardClick: () -> Unit,
  onCloseClick: () -> Unit
) {
  Scaffold(
    topBar = {
      MediumTopAppBar(
        title = {
          Text(text = stringResource(R.string.audiobook_folders_title))
        },
        navigationIcon = {
          IconButton(onClick = onCloseClick) {
            Icon(
              imageVector = Icons.Default.ArrowBack,
              contentDescription = stringResource(R.string.close)
            )
          }
        }
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        text = {
          Text(stringResource(R.string.add))
        },
        onClick = onAddClick,
        icon = {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(R.string.add)
          )
        }
      )
    }
  ) {
    LazyColumn {
      viewState.explanationCard?.let { text ->
        item {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp),
          ) {
            Column(Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 16.dp)) {
              Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge
              )
              Spacer(modifier = Modifier.size(16.dp))
              Button(
                modifier = Modifier.align(End),
                onClick = onDismissExplanationCardClick
              ) {
                Text(text = stringResource(R.string.got_audiobook_folder_card_action))
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
                Icon(
                  imageVector = Icons.Default.Delete,
                  contentDescription = stringResource(R.string.delete)
                )
              }
            )
          }) {
          Text(text = item.name, style = MaterialTheme.typography.bodyLarge)
        }
      }
    }
  }
}
