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
import java.net.URI
import java.nio.file.Path
import java.util.Locale

/** Subclassed in every library module with Compose by the `voice.compose` plugin, run with `./gradlew :<module>:snapshot`. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class PreviewSnapshotTest {

  @get:Rule
  val compose = createEmptyComposeRule()

  @Test
  fun snapshot() {
    val outputDir = File(System.getProperty(OUTPUT_DIR_PROPERTY) ?: "build/snapshots").apply { mkdirs() }
    // Gradle runs the tests in the module's directory, and the module's classes come from its build directory
    val moduleBuildDir = File(System.getProperty("user.dir"), "build").toPath()
    val previews = AndroidComposablePreviewScanner()
      .scanPackageTrees("voice")
      .includePrivatePreviews()
      .getPreviews()
      .filter { classLocation(it.declaringClass)?.startsWith(moduleBuildDir) == true }
    val ids = previews.map { AndroidPreviewScreenshotIdBuilder(it).encodeUnsafeCharacters().build() }
    val shortNames = previews.zip(ids) { preview, id -> id.removePrefix(preview.declaringClass.substringBeforeLast('.') + ".") }
    val failures = mutableListOf<String>()
    val snapshots = mutableListOf<File>()

    @IgnorableReturnValue
    fun render(
      preview: ComposablePreview<AndroidPreviewInfo>,
      info: AndroidPreviewInfo,
      name: String,
    ): Bitmap? {
      return try {
        capture(preview, info).also { bitmap ->
          val file = File(outputDir, "$name.png")
          file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
          snapshots += file
        }
      } catch (error: Throwable) {
        failures += "$name: ${generateSequence(error) { it.cause }.last()}"
        null
      }
    }

    previews.forEachIndexed { index, preview ->
      val name = shortNames[index].takeIf { shortNames.count { it == shortNames[index] } == 1 } ?: ids[index]
      val info = preview.previewInfo
      val bitmap = render(preview, info, name) ?: return@forEachIndexed
      if (info.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_UNDEFINED) {
        render(preview, info.copy(uiMode = info.uiMode or Configuration.UI_MODE_NIGHT_YES), "${name}_dark")
      }
      // filling the whole window makes it a screen, and screens change their layout in other windows
      val screen = info.widthDp <= 0 && info.heightDp <= 0 &&
        bitmap.width == DEFAULT_WIDTH_DP * DENSITY && bitmap.height == DEFAULT_HEIGHT_DP * DENSITY
      if (screen) {
        render(preview, info.copy(widthDp = DEFAULT_HEIGHT_DP, heightDp = DEFAULT_WIDTH_DP), "${name}_landscape")
        render(preview, info.copy(widthDp = TABLET_WIDTH_DP, heightDp = TABLET_HEIGHT_DP), "${name}_tablet")
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
    info: AndroidPreviewInfo,
  ): Bitmap {
    val rtl = info.locale.substringBefore('-') in RTL_LANGUAGES
    val night = info.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    RuntimeEnvironment.setQualifiers(qualifiers(info, rtl, night))
    RuntimeEnvironment.setFontScale(info.fontScale.takeIf { it > 0F } ?: 1F)
    // the qualifiers localize resources, but number and date formatting read the default locale
    val defaultLocale = Locale.getDefault()
    if (info.locale.isNotEmpty()) {
      Locale.setDefault(Locale.forLanguageTag(info.locale.replace("-r", "-")))
    }
    try {
      return ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
        // the marquee on long titles and the animation clocks never go idle
        compose.mainClock.autoAdvance = false
        scenario.onActivity { activity ->
          // the test activity's window is always light, which would hide light text in night mode
          activity.window.setBackgroundDrawableResource(
            if (night) android.R.color.background_dark else android.R.color.background_light,
          )
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
        compose.onNodeWithTag(PREVIEW_TAG).captureToImage().asAndroidBitmap()
      }
    } finally {
      Locale.setDefault(defaultLocale)
    }
  }

  private fun classLocation(className: String): Path? {
    val url = javaClass.classLoader?.getResource(className.replace('.', '/') + ".class") ?: return null
    val location = if (url.protocol == "jar") URI(url.path.substringBefore("!/")) else url.toURI()
    return File(location).toPath()
  }

  private fun qualifiers(
    info: AndroidPreviewInfo,
    rtl: Boolean,
    night: Boolean,
  ): String {
    val locale = info.locale.takeIf { it.isNotEmpty() }
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
private const val TABLET_WIDTH_DP = 1280
private const val TABLET_HEIGHT_DP = 800

// the xhdpi qualifier
private const val DENSITY = 2

private val RTL_LANGUAGES = setOf("ar", "fa", "he", "iw", "ur")
