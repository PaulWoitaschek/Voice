package voice.baselineprofile

import android.os.SystemClock
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.util.regex.Pattern
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE = "de.ph1b.audiobook"
private const val BOOK = "Book"
private const val TIMEOUT = 10_000L

// Listeners start on their library, so a book is added before the profile is recorded.
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

  @get:Rule val rule = BaselineProfileRule()

  private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

  @Before
  fun addLibrary() {
    copyBookToDevice()
    device.executeShellCommand("pm grant $PACKAGE android.permission.POST_NOTIFICATIONS")
    device.executeShellCommand("am start -W -n $PACKAGE/voice.app.MainActivity")
    completeOnboarding()
  }

  @Test
  fun generate() {
    rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
      pressHome()
      startActivityAndWait()
      openBook()
    }
  }

  private fun copyBookToDevice() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val assets = instrumentation.context.assets
    val folder = "Audiobooks/$BOOK"
    device.executeShellCommand("mkdir -p /sdcard/$folder")
    assets.list(folder)!!.forEach { file ->
      // UiAutomation runs commands without a shell, so dd writes the file instead of a redirect.
      val (output, input) =
        instrumentation.uiAutomation.executeShellCommandRw("dd of=/sdcard/$folder/$file")
      FileOutputStream(input.fileDescriptor).use { assets.open("$folder/$file").copyTo(it) }
      input.close()
      output.close()
    }
  }

  private fun completeOnboarding() {
    val startListening = app("Start Listening")
    // The most advanced step comes first, as earlier screens can still show behind later ones.
    val buttons =
      listOf(
        startListening,
        app("Add 1 book"),
        picker("Allow"),
        app("Choose a folder"),
        app("Continue without analytics"),
        app("Next"),
        app("Let's go"),
      )
    val deadline = SystemClock.uptimeMillis() + 120_000
    while (true) {
      check(SystemClock.uptimeMillis() < deadline) { "Onboarding is stuck on:\n${hierarchy()}" }
      val selector =
        buttons.firstOrNull { device.hasObject(it) }
          ?: if (device.hasObject(picker(BOOK))) {
            picker("Use this folder")
          } else {
            picker("Audiobooks")
          }
      val button = device.findObject(selector)
      if (button == null) {
        SystemClock.sleep(500)
        continue
      }
      // Screens animate in and out, so the button can be gone by the time it is clicked.
      try {
        button.click()
      } catch (_: StaleObjectException) {
        continue
      }
      device.waitForIdle()
      if (selector === startListening) break
    }
    check(device.wait(Until.hasObject(app(BOOK)), TIMEOUT)) {
      "The library does not show the book:\n${hierarchy()}"
    }
  }

  private fun openBook() {
    val book = device.wait(Until.findObject(app(BOOK)), TIMEOUT)
    checkNotNull(book) { "The library does not show the book:\n${hierarchy()}" }.click()
    val playButton = By.pkg(PACKAGE).desc(Pattern.compile("Play|Pause"))
    check(device.wait(Until.hasObject(playButton), TIMEOUT)) {
      "The playback screen did not open:\n${hierarchy()}"
    }
    device.pressBack()
    check(device.wait(Until.hasObject(app(BOOK)), TIMEOUT)) {
      "Back did not return to the library:\n${hierarchy()}"
    }
  }

  private fun app(text: String): BySelector = By.pkg(PACKAGE).text(ignoreCase(text))

  private fun picker(text: String): BySelector =
    By.pkg(Pattern.compile(".*documentsui")).text(ignoreCase(text))

  private fun ignoreCase(text: String): Pattern =
    Pattern.compile(Pattern.quote(text), Pattern.CASE_INSENSITIVE)

  private fun hierarchy(): String =
    ByteArrayOutputStream().also { device.dumpWindowHierarchy(it) }.toString()
}
