package voice.baselineprofile

import android.os.SystemClock
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.regex.Pattern
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE = "de.ph1b.audiobook"
private const val BOOK = "Book"
private const val TIMEOUT = 10_000L

// Listeners start on their library, so the profile is recorded with a book added, not on
// onboarding.
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

  @get:Rule val rule = BaselineProfileRule()

  private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

  @Before
  fun copyBookToDevice() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val assets = instrumentation.context.assets
    val folder = "Audiobooks/$BOOK"
    device.executeShellCommand("mkdir -p /sdcard/$folder")
    assets.list(folder)!!.forEach { file ->
      // UiAutomation runs commands without a shell, so tee writes the file instead of a redirect.
      val (output, input) =
        instrumentation.uiAutomation.executeShellCommandRw("tee /sdcard/$folder/$file")
      FileOutputStream(input.fileDescriptor).use { assets.open("$folder/$file").copyTo(it) }
      input.close()
      FileInputStream(output.fileDescriptor).use { it.readBytes() }
      output.close()
    }
  }

  @Test
  fun generate() {
    rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
      pressHome()
      startActivityAndWait()
      addLibraryIfMissing()
      openBook()
    }
  }

  // The rule reinstalls the app before it records, which removes the library.
  private fun addLibraryIfMissing() {
    device.executeShellCommand("pm grant $PACKAGE android.permission.POST_NOTIFICATIONS")
    fun app(text: String) = By.pkg(PACKAGE).text(text(text))
    fun picker(text: String) = By.pkg(Pattern.compile(".*documentsui")).text(text(text))
    // The most advanced step comes first, as earlier screens can still show behind later ones.
    val buttons =
      listOf(
        app("Start Listening"),
        app("Add 1 book"),
        picker("Allow"),
        app("Choose a folder"),
        app("Continue without analytics"),
        app("Next"),
        app("Let's go"),
      )
    val deadline = SystemClock.uptimeMillis() + 120_000
    while (true) {
      if (SystemClock.uptimeMillis() > deadline) {
        val hierarchy = ByteArrayOutputStream().also { device.dumpWindowHierarchy(it) }
        error("The library setup is stuck on this screen:\n$hierarchy")
      }
      val button =
        buttons.firstNotNullOfOrNull { device.findObject(it) }
          ?: if (device.hasObject(picker(BOOK))) {
            device.findObject(picker("Use this folder"))
          } else {
            device.findObject(picker("Audiobooks"))
          }
      if (button == null) {
        if (device.hasObject(By.pkg(PACKAGE).text(BOOK))) return
        SystemClock.sleep(500)
        continue
      }
      // Screens animate in and out, so the button can be gone by the time it is clicked.
      try {
        button.click()
      } catch (_: StaleObjectException) {}
      device.waitForIdle()
    }
  }

  private fun openBook() {
    device.wait(Until.findObject(By.pkg(PACKAGE).text(BOOK)), TIMEOUT).click()
    device.wait(Until.hasObject(By.desc(Pattern.compile("Play|Pause"))), TIMEOUT)
    device.pressBack()
    device.wait(Until.hasObject(By.text(BOOK)), TIMEOUT)
  }

  private fun text(text: String): Pattern =
    Pattern.compile(Pattern.quote(text), Pattern.CASE_INSENSITIVE)
}
