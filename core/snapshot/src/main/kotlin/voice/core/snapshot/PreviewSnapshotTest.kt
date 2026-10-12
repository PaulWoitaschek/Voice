package voice.core.snapshot

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode
import sergio.sastre.composable.preview.scanner.android.AndroidComposablePreviewScanner
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.android.screenshotid.AndroidPreviewScreenshotIdBuilder
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview
import java.io.File

/**
 * Renders every `@Preview` of the module under test to a PNG, sized, scaled, themed and localized
 * the way its annotation says, plus a contact sheet of all of them.
 *
 * Each library module with Compose gets a subclass and a `snapshot` task from the `voice.compose`
 * plugin: `./gradlew :features:playbackScreen:snapshot` writes to `build/snapshots`.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class PreviewSnapshotTest {

  @get:Rule
  val compose = createEmptyComposeRule()

  @Test
  fun snapshot() {
    val outputDir = File(System.getProperty(OUTPUT_DIR_PROPERTY) ?: "build/snapshots").apply { mkdirs() }
    // Gradle runs the tests in the module's directory, and its classes come from its build directory
    val moduleBuildDir = File(System.getProperty("user.dir"), "build").absolutePath + "/"
    val previews = AndroidComposablePreviewScanner()
      .scanPackageTrees("voice")
      .includePrivatePreviews()
      .getPreviews()
      .filter { preview ->
        val classFile = javaClass.classLoader?.getResource(preview.declaringClass.replace('.', '/') + ".class")
        classFile?.path?.contains(moduleBuildDir) == true
      }
    val failures = mutableListOf<String>()
    val snapshots = previews.mapNotNull { preview ->
      val name = AndroidPreviewScreenshotIdBuilder(preview)
        .encodeUnsafeCharacters()
        .build()
        .removePrefix(preview.declaringClass.substringBeforeLast('.') + ".")
      try {
        File(outputDir, "$name.png").also { capture(preview, it) }
      } catch (error: Throwable) {
        failures += "$name: ${generateSequence(error) { it.cause }.last()}"
        null
      }
    }
    if (snapshots.isNotEmpty()) {
      writeContactSheet(snapshots, File(outputDir, CONTACT_SHEET))
    }
    if (failures.isNotEmpty()) {
      throw AssertionError("Previews that failed to render:\n" + failures.joinToString("\n"))
    }
  }

  private fun capture(
    preview: ComposablePreview<AndroidPreviewInfo>,
    file: File,
  ) {
    val info = preview.previewInfo
    val rtl = info.locale.substringBefore('-') in RTL_LANGUAGES
    RuntimeEnvironment.setQualifiers(qualifiers(info, rtl))
    RuntimeEnvironment.setFontScale(info.fontScale.takeIf { it > 0F } ?: 1F)
    ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
      // the marquee on long titles and the animation clocks never go idle
      compose.mainClock.autoAdvance = false
      scenario.onActivity { activity ->
        activity.setContent {
          // a library's test manifest doesn't declare RTL support, so the views would stay left to right
          CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
            // like Android Studio: a size from the annotation is exact, otherwise the preview wraps its content
            Box(Modifier.wrapContentSize(Alignment.TopStart)) {
              Box(
                Modifier
                  .then(if (info.widthDp > 0) Modifier.width(info.widthDp.dp) else Modifier)
                  .then(if (info.heightDp > 0) Modifier.height(info.heightDp.dp) else Modifier)
                  .then(if (info.showBackground) Modifier.background(backgroundColor(info)) else Modifier)
                  .testTag(PREVIEW_TAG),
              ) {
                preview()
              }
            }
          }
        }
      }
      compose.waitForIdle()
      // past the entrance animations, before the marquee starts scrolling
      compose.mainClock.advanceTimeBy(1_000)
      val bitmap = compose.onNodeWithTag(PREVIEW_TAG).captureToImage().asAndroidBitmap()
      file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
  }

  private fun qualifiers(
    info: AndroidPreviewInfo,
    rtl: Boolean,
  ): String {
    val locale = info.locale.takeIf { it.isNotEmpty() }
    val night = info.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    return listOfNotNull(
      locale,
      if (rtl) "ldrtl" else "ldltr",
      "w${info.widthDp.takeIf { it > 0 } ?: DEFAULT_WIDTH_DP}dp",
      "h${info.heightDp.takeIf { it > 0 } ?: DEFAULT_HEIGHT_DP}dp",
      if (night) "night" else "notnight",
      "xhdpi",
    ).joinToString("-")
  }

  private fun backgroundColor(info: AndroidPreviewInfo): Color {
    return if (info.backgroundColor == 0L) Color.White else Color(info.backgroundColor.toInt())
  }
}

private const val OUTPUT_DIR_PROPERTY = "voice.snapshot.dir"
private const val CONTACT_SHEET = "_contact-sheet.png"
private const val PREVIEW_TAG = "preview"

// the size of Android Studio's default preview device
private const val DEFAULT_WIDTH_DP = 411
private const val DEFAULT_HEIGHT_DP = 891

private val RTL_LANGUAGES = setOf("ar", "fa", "he", "iw", "ur")
