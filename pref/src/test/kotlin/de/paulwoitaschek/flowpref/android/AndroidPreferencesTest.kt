package de.paulwoitaschek.flowpref.android

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import de.paulwoitaschek.flowpref.Pref
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

private const val DEFAULT_KEY = "key"

@RunWith(AndroidJUnit4::class)
class AndroidPreferencesTest {

  private val prefs: AndroidPreferences
  private val sharedPrefs: SharedPreferences

  init {
    val context = ApplicationProvider.getApplicationContext<Context>()
    sharedPrefs = context.getSharedPreferences("testPrefs", Context.MODE_PRIVATE)
    prefs = AndroidPreferences(sharedPrefs)
  }

  @Test
  fun clearWithCommit() {
    val pref = prefs.string(DEFAULT_KEY, "Bob")
    pref.setAndCommit("Alice")
    prefs.clear(commit = true)
    assertSharedPrefValue(null)
    pref.value shouldBe "Bob"
  }

  @Test
  fun clearWithoutCommit() {
    val pref = prefs.string(DEFAULT_KEY, "Bob")
    pref.setAndCommit("Alice")
    prefs.clear(commit = true)
    assertSharedPrefValue(null)
    pref.value shouldBe "Bob"
  }

  @Test
  fun flow() = runTest {
    val pref = prefs.string(DEFAULT_KEY, "Bob")
    pref.flow.test {
      awaitItem() shouldBe "Bob"

      pref.value = "1"
      awaitItem() shouldBe "1"

      pref.setAndCommit("2")
      awaitItem() shouldBe "2"

      pref.delete(commit = true)
      awaitItem() shouldBe "Bob"

      pref.setAndCommit("John")
      awaitItem() shouldBe "John"

      pref.delete(commit = false)
      awaitItem() shouldBe "Bob"

      pref.value = "Alice"
      awaitItem() shouldBe "Alice"

      prefs.clear(commit = false)
      awaitItem() shouldBe "Bob"
    }
  }

  @Test
  fun setWithCommit() {
    val pref = prefs.string(DEFAULT_KEY, "Bob")

    assertSharedPrefValue(null)
    pref.assertValue("Bob")

    pref.setAndCommit("Alice")
    pref.assertValue("Alice")
    assertSharedPrefValue("Alice")

    pref.setAndCommit("John")
    pref.assertValue("John")
    assertSharedPrefValue("John")
  }

  @Test
  fun setWithoutCommit() {
    val pref = prefs.string(DEFAULT_KEY, "Bob")

    assertSharedPrefValue(null)
    pref.assertValue("Bob")

    pref.value = "Alice"
    pref.assertValue("Alice")
    assertSharedPrefValue("Alice")

    pref.value = "John"
    pref.assertValue("John")
    assertSharedPrefValue("John")
  }

  @Test
  fun deleteWithCommit() {
    val pref = prefs.string(DEFAULT_KEY, "Bob")
    assertSharedPrefValue(null)
    pref.setAndCommit("Alice")
    pref.delete(commit = true)
    pref.assertValue("Bob")
    assertSharedPrefValue(null)
  }

  @Test
  fun deleteWithoutCommit() {
    val pref = prefs.string(DEFAULT_KEY, "Bob")
    assertSharedPrefValue(null)
    pref.setAndCommit("Alice")
    pref.delete(commit = false)
    pref.assertValue("Bob")
    assertSharedPrefValue(null)
  }

  private fun <T> Pref<T>.assertValue(value: T?) {
    this.value shouldBe value
  }

  private fun assertSharedPrefValue(value: String?) {
    sharedPrefs.getString(DEFAULT_KEY, null) shouldBe value
  }

  @Test
  fun onlyChangedFlowTriggers() = runTest {
    turbineScope {
      val prefA = prefs.int("preA", 42)
      val prefB = prefs.int("prefB", 42)

      val aTurbine = prefA.flow.testIn(this)
      val bTurbine = prefB.flow.testIn(this)

      aTurbine.awaitItem() shouldBe 42
      bTurbine.awaitItem() shouldBe 42

      prefA.setAndCommit(1)

      aTurbine.awaitItem() shouldBe 1

      prefA.setAndCommit(2)
      aTurbine.awaitItem() shouldBe 2

      prefB.setAndCommit(1)
      bTurbine.awaitItem() shouldBe 1

      aTurbine.expectNoEvents()
      bTurbine.expectNoEvents()

      aTurbine.cancel()
      bTurbine.cancel()
    }
  }
}
