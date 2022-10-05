package voice.app.misc

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.services.storage.TestStorage
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import voice.bookOverview.overview.BookOverviewCategory
import voice.bookOverview.overview.BookOverviewItemViewState
import voice.bookOverview.overview.BookOverviewLayoutMode
import voice.bookOverview.overview.BookOverviewViewState
import voice.bookOverview.views.BookOverviewPreview
import voice.common.BookId
import voice.common.compose.ImmutableFile
import voice.common.formatTime
import voice.folderPicker.folderPicker.FolderPickerPreview
import voice.logging.core.Logger
import voice.settings.views.Settings
import java.io.File
import kotlin.random.Random
import kotlin.time.Duration.Companion.hours

class ScreenshotCapture {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun screenshots() {
    val currentScreenshot = mutableStateOf<Screenshot?>(null)
    var slotId = 0
    composeRule.setContent {
      val screenshot = currentScreenshot.value ?: return@setContent
      SubcomposeLayout { constraints ->
        // use an increasing slotId to prevent recompositions
        val placeable = subcompose(slotId++) {
          Box(Modifier.testTag(screenshot.name)) {
            screenshot.composable()
          }
        }.map { it.measure(constraints) }
          .first()
        layout(placeable.width, placeable.height) {
          placeable.placeRelative(0, 0)
        }
      }
    }

    screenshotData().forEach { screenshot ->
      currentScreenshot.value = screenshot

      // wait for the covers to load
      composeRule.waitForIdle()
      composeRule.waitUntil {
        composeRule.onNodeWithTag(screenshot.name).fetchSemanticsNode().children.size == 1
      }
      composeRule.waitForIdle()
      Thread.sleep(500)
      composeRule.waitForIdle()

      val targetFile = File(ApplicationProvider.getApplicationContext<Application>().filesDir, "screenshot.png")
      UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        .takeScreenshot(targetFile)
      TestStorage().openOutputFile(screenshot.name + ".png").use { out ->
        targetFile.inputStream().use {
          it.copyTo(out)
        }
      }
    }
  }

  private fun copyAssets(
    @Suppress("SameParameterValue")
    name: String,
    target: File,
  ) {
    target.deleteRecursively()
    target.mkdirs()

    val assets = InstrumentationRegistry.getInstrumentation().context.resources.assets

    fun copyFile(name: String) {
      Logger.d("copyFile $name")
      assets.open(name).use { input ->
        File(target.absolutePath + "/" + name)
          .also {
            it.parentFile!!.mkdirs()
            Logger.d("copy $name to $it")
          }
          .outputStream().use { output ->
            input.copyTo(output)
          }
      }
    }

    fun copyAsset(name: String) {
      Logger.d("copyAsset $name")
      val contents = assets.list(name) ?: return
      if (contents.isEmpty()) {
        copyFile(name)
      } else {
        contents.forEach { child ->
          copyAsset("$name/$child")
        }
      }
    }

    copyAsset(name)
  }

  data class Screenshot(
    val name: String,
    val composable: @Composable () -> Unit,
  )

  private fun screenshotData(): List<Screenshot> {
    val bookOverviewViewState = bookOverviewViewState()
    val bookOverviewList = bookOverviewViewState.copy(
      layoutMode = BookOverviewLayoutMode.List,
    )
    val bookOverviewGrid = bookOverviewViewState.copy(
      layoutMode = BookOverviewLayoutMode.Grid,
    )
    return listOf(
      Screenshot("book_overview_list") {
        BookOverviewPreview(
          viewState = bookOverviewList,
        )
      },
      Screenshot("book_overview_grid") {
        BookOverviewPreview(
          viewState = bookOverviewGrid,
        )
      },
      Screenshot("settings") { Settings() },
      Screenshot("folder_picker") { FolderPickerPreview() },
    )
  }

  private fun bookOverviewViewState(): BookOverviewViewState.Content {
    val foreNames = mutableListOf(
      "Aysha", "Bonnie", "Tianna", "Fleur", "Imogen", "Sienna", "Kimberley", "Elizabeth", "Priya", "Claudia",
    ).also { it.shuffle() }
    val sureNames = mutableListOf(
      "Jennings", "Dorsey", "Chandler", "Robinson", "Zuniga", "Petty", "Donaldson", "Haines", "Glenn",
    ).also { it.shuffle() }
    val filesDir = ApplicationProvider.getApplicationContext<Application>()
      .filesDir
    val target = File(filesDir, "assets")
    target.deleteRecursively()
    copyAssets("covers", target)

    val testData = File(target, "covers").listFiles()!!.map {
      val coverFile = File(it, "cover.jpg")
      it.name to coverFile
    }

    val books = testData.mapIndexed { index, (name, cover) ->
      BookOverviewItemViewState(
        name = name,
        author = foreNames.removeFirst() + " " + sureNames.removeFirst(),
        cover = ImmutableFile(cover),
        progress = Random.nextFloat(),
        id = BookId("$index"),
        remainingTime = formatTime(5.hours.inWholeMilliseconds + Random.nextLong(10.hours.inWholeMilliseconds)),
      )
    }
    return BookOverviewViewState.Content(
      books = mapOf(
        BookOverviewCategory.CURRENT to books.take(3),
        BookOverviewCategory.NOT_STARTED to books.drop(3).map { it.copy(progress = 0F) },
      ),
      layoutMode = BookOverviewLayoutMode.List,
      playButtonState = BookOverviewViewState.PlayButtonState.Paused,
      showAddBookHint = false,
      showMigrateHint = false,
      showMigrateIcon = false,
      showSearchIcon = true,
    )
  }
}
