@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.folderPicker.selectType

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.data.folders.FolderType
import voice.core.ui.AuroraBackground
import voice.core.ui.OnboardingButton
import voice.core.ui.OnboardingStep
import voice.core.ui.OnboardingTopBar
import voice.core.ui.VoiceTheme
import voice.core.ui.rememberAnimationClock
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.navigation.Origin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import voice.core.strings.R as StringsR

@ContributesTo(AppScope::class)
interface SelectFolderTypeGraph {
  val selectFolderTypeViewModelFactory: SelectFolderTypeViewModel.Factory
}

@BindingContainer
@ContributesTo(AppScope::class)
object SelectFolderTypeProvider {

  @Provides
  @IntoSet
  fun selectFolderTypeNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.SelectFolderType> { key ->
    NavEntry(key) {
      SelectFolderType(
        uri = key.uri,
        origin = key.origin,
        currentType = key.currentType,
      )
    }
  }
}

@Composable
fun SelectFolderType(
  uri: Uri,
  origin: Origin,
  currentType: FolderType?,
) {
  val context = LocalContext.current
  val viewModel = retain(uri.toString(), origin.name, currentType?.name) {
    rootGraphAs<SelectFolderTypeGraph>().selectFolderTypeViewModelFactory
      .create(
        uri = uri,
        origin = origin,
        documentFile = DocumentFile.fromTreeUri(context, uri)!!,
        currentType = currentType,
      )
  }
  SelectFolderType(
    viewState = viewModel.viewState(),
    onModeSelect = viewModel::selectMode,
    onAddClick = viewModel::add,
    onBackClick = viewModel::onCloseClick,
  )
}

@Composable
private fun SelectFolderType(
  viewState: SelectFolderTypeViewState,
  onModeSelect: (FolderMode) -> Unit,
  onAddClick: () -> Unit,
  onBackClick: () -> Unit,
) {
  var showModes by rememberSaveable { mutableStateOf(false) }
  val clock = rememberAnimationClock(running = true)
  Box(Modifier.fillMaxSize()) {
    AuroraBackground(
      clock = { clock.value },
      showStars = true,
      modifier = Modifier.fillMaxSize(),
    )
    Scaffold(
      containerColor = Color.Transparent,
      contentColor = MaterialTheme.colorScheme.onSurface,
      topBar = {
        OnboardingTopBar(
          step = if (viewState.onboarding) OnboardingStep.AddContent else null,
          onBack = onBackClick,
        )
      },
      bottomBar = {
        ReviewActions(
          viewState = viewState,
          onAddClick = onAddClick,
          onChangeClick = { showModes = true },
          onChooseAnotherFolder = onBackClick,
        )
      },
    ) { contentPadding ->
      ReviewContent(
        contentPadding = contentPadding,
        viewState = viewState,
        onBookClick = { showModes = true },
      )
    }
  }
  if (showModes) {
    FolderModeSheet(
      viewState = viewState,
      onModeSelect = {
        onModeSelect(it)
        showModes = false
      },
      onDismiss = { showModes = false },
    )
  }
}

@Composable
private fun ReviewContent(
  contentPadding: PaddingValues,
  viewState: SelectFolderTypeViewState,
  onBookClick: () -> Unit,
) {
  val itemModifier = Modifier
    .widthIn(max = 560.dp)
    .fillMaxWidth()
    .padding(horizontal = 16.dp)
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = contentPadding,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    item(key = "header") {
      ReviewHeader(viewState = viewState, modifier = itemModifier)
    }
    if (viewState.loading) {
      item(key = "loading") {
        LoadingIndicator(Modifier.size(64.dp))
      }
    } else {
      itemsIndexed(viewState.books) { index, book ->
        BookRow(
          book = book,
          index = index,
          count = viewState.books.size,
          onClick = onBookClick,
          modifier = itemModifier.padding(bottom = 2.dp),
        )
      }
    }
    item(key = "bottomSpace") {
      Spacer(Modifier.height(24.dp))
    }
  }
}

@Composable
private fun ReviewHeader(
  viewState: SelectFolderTypeViewState,
  modifier: Modifier = Modifier,
) {
  val empty = !viewState.loading && viewState.books.isEmpty()
  Column(
    modifier = modifier.padding(top = 8.dp, bottom = 24.dp, start = 8.dp, end = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = when {
        viewState.loading -> stringResource(StringsR.string.folder_review_loading, viewState.folderName)
        empty -> stringResource(StringsR.string.folder_review_empty_title, viewState.folderName)
        else -> pluralStringResource(StringsR.plurals.folder_review_title, viewState.books.size, viewState.books.size)
      },
      style = MaterialTheme.typography.displaySmallEmphasized,
      textAlign = TextAlign.Center,
    )
    if (!viewState.loading) {
      Spacer(Modifier.height(12.dp))
      Text(
        text = if (empty) {
          stringResource(StringsR.string.folder_review_empty_subtitle)
        } else {
          viewState.selectedMode.optionTitle(viewState.folderName)
        },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun BookRow(
  book: SelectFolderTypeViewState.Book,
  index: Int,
  count: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(
    topStart = if (index == 0) 24.dp else 6.dp,
    topEnd = if (index == 0) 24.dp else 6.dp,
    bottomStart = if (index == count - 1) 24.dp else 6.dp,
    bottomEnd = if (index == count - 1) 24.dp else 6.dp,
  )
  val content: @Composable () -> Unit = {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (book.analyzing) {
        LoadingIndicator(Modifier.size(44.dp))
      } else {
        BookInitial(name = book.name, index = index)
      }
      Spacer(Modifier.size(16.dp))
      Column(Modifier.weight(1F)) {
        Text(
          text = book.name,
          style = MaterialTheme.typography.titleMedium,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = book.details(),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (book.possibleBookCount > 0) {
        Spacer(Modifier.size(12.dp))
        PossibleBooksHint(book.possibleBookCount)
      }
    }
  }
  val color = MaterialTheme.colorScheme.surfaceContainerHigh
  if (book.possibleBookCount > 0) {
    Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, content = content)
  } else {
    Surface(modifier = modifier, shape = shape, color = color, content = content)
  }
}

@Composable
private fun SelectFolderTypeViewState.Book.details(): String {
  return listOfNotNull(
    author,
    duration?.let { formatDuration(it) },
    pluralStringResource(StringsR.plurals.folder_type_file_count, fileCount, fileCount),
    if (partCount > 1) pluralStringResource(StringsR.plurals.folder_review_folder_count, partCount, partCount) else null,
  ).joinToString(separator = " · ")
}

// "21 hr, 2 min" in the user's language, without own translations
@Composable
private fun formatDuration(duration: Duration): String {
  val locale = LocalConfiguration.current.locales[0]
  val format = remember(locale) { MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.SHORT) }
  val hours = duration.inWholeHours
  val minutes = duration.inWholeMinutes % 60
  val measures = when {
    hours > 0 && minutes > 0 -> listOf(Measure(hours, MeasureUnit.HOUR), Measure(minutes, MeasureUnit.MINUTE))
    hours > 0 -> listOf(Measure(hours, MeasureUnit.HOUR))
    minutes > 0 -> listOf(Measure(minutes, MeasureUnit.MINUTE))
    else -> listOf(Measure(duration.inWholeSeconds, MeasureUnit.SECOND))
  }
  return format.formatMeasures(*measures.toTypedArray())
}

private val initialShapes = listOf(
  MaterialShapes.Cookie9Sided,
  MaterialShapes.Clover4Leaf,
  MaterialShapes.Sunny,
  MaterialShapes.Cookie4Sided,
  MaterialShapes.Pill,
)

/** The first letter of a book on a playful shape, standing in for a cover we don't have yet. */
@Composable
private fun BookInitial(
  name: String,
  index: Int,
) {
  val colorScheme = MaterialTheme.colorScheme
  val (container, content) = when (index % 3) {
    0 -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
    1 -> colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer
    else -> colorScheme.secondaryContainer to colorScheme.onSecondaryContainer
  }
  Box(
    modifier = Modifier
      .size(44.dp)
      .background(container, initialShapes[index % initialShapes.size].toShape()),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?",
      style = MaterialTheme.typography.titleMediumEmphasized,
      color = content,
    )
  }
}

@Composable
private fun PossibleBooksHint(count: Int) {
  Surface(
    shape = CircleShape,
    color = MaterialTheme.colorScheme.tertiary,
    contentColor = MaterialTheme.colorScheme.onTertiary,
  ) {
    Text(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      text = pluralStringResource(StringsR.plurals.folder_review_possible_books, count, count),
      style = MaterialTheme.typography.labelLarge,
    )
  }
}

@Composable
private fun ReviewActions(
  viewState: SelectFolderTypeViewState,
  onAddClick: () -> Unit,
  onChangeClick: () -> Unit,
  onChooseAnotherFolder: () -> Unit,
) {
  val surface = MaterialTheme.colorScheme.surface
  Box(
    modifier = Modifier
      .fillMaxWidth()
      // keeps the books scrolling underneath readable
      .background(Brush.verticalGradient(0F to surface.copy(alpha = 0F), 0.3F to surface.copy(alpha = 0.9F)))
      .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
      .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 400.dp)
        .fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      if (!viewState.loading && viewState.books.isEmpty()) {
        OnboardingButton(
          text = stringResource(StringsR.string.folder_add_action_folder),
          onClick = onChooseAnotherFolder,
        )
      } else {
        OnboardingButton(
          text = if (viewState.editing) {
            stringResource(StringsR.string.common_action_save)
          } else {
            pluralStringResource(StringsR.plurals.folder_review_action_add, viewState.books.size, viewState.books.size)
          },
          onClick = onAddClick,
          enabled = !viewState.loading,
        )
        TextButton(
          modifier = Modifier.fillMaxWidth(),
          onClick = onChangeClick,
          enabled = !viewState.loading,
        ) {
          Text(stringResource(StringsR.string.folder_review_action_change))
        }
      }
    }
  }
}

@Preview
@Composable
private fun SelectFolderTypePreview() {
  val books = listOf(
    SelectFolderTypeViewState.Book(
      name = "Dune",
      author = "Frank Herbert",
      fileCount = 24,
      partCount = 0,
      possibleBookCount = 0,
      duration = 21.hours + 2.minutes,
    ),
    SelectFolderTypeViewState.Book(
      name = "Hyperion",
      author = null,
      fileCount = 31,
      partCount = 2,
      possibleBookCount = 0,
      analyzing = true,
    ),
    SelectFolderTypeViewState.Book("Discworld", author = null, fileCount = 312, partCount = 0, possibleBookCount = 41),
  )
  VoiceTheme {
    SelectFolderType(
      viewState = SelectFolderTypeViewState(
        folderName = "Audiobooks",
        loading = false,
        selectedMode = FolderMode.Audiobooks,
        guessedMode = FolderMode.Audiobooks,
        books = books,
        options = FolderMode.entries.map { SelectFolderTypeViewState.Option(it, books) },
        editing = false,
        onboarding = true,
      ),
      onModeSelect = {},
      onAddClick = {},
      onBackClick = {},
    )
  }
}
