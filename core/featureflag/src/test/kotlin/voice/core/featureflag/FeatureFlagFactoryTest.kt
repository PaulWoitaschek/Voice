package voice.core.featureflag

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import voice.core.remoteconfig.api.RemoteConfig
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executors
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class FeatureFlagFactoryTest {

  private enum class Style { Classic, Calm }

  private val remoteConfig = mockk<RemoteConfig>()
  private val overridesStore = MemoryDataStore<Map<String, FeatureFlagOverride>>(emptyMap())

  private fun TestScope.styleFlag(remoteValue: String): FeatureFlag<Style> {
    every { remoteConfig.string("style", "Classic") } returns remoteValue
    return FeatureFlagFactory(remoteConfig, FeatureFlagOverridesStore(overridesStore), backgroundScope)
      .enum(key = "style", description = "", defaultValue = Style.Classic, entries = Style.entries)
  }

  @Test
  fun `enum flag reads the remote config entry by name`() = runTest {
    assertEquals(Style.Calm, styleFlag(remoteValue = "Calm").get())
  }

  @Test
  fun `enum flag falls back to the default for an unknown remote value`() = runTest {
    assertEquals(Style.Classic, styleFlag(remoteValue = "Unknown").get())
  }

  @Test
  fun `enum flag override wins over the remote config`() = runTest {
    val flag = styleFlag(remoteValue = "Classic")

    flag.overwrite(Style.Calm)
    runCurrent()

    assertEquals(Style.Calm, flag.get())
    assertEquals(FeatureFlagValue(Style.Calm, isOverridden = true), flag.flow.first())
  }

  @Test
  fun `enum flag ignores an override of an entry that no longer exists`() = runTest {
    overridesStore.updateData { mapOf("style" to FeatureFlagOverride.StringValue("Removed")) }

    val flag = styleFlag(remoteValue = "Calm")

    assertEquals(FeatureFlagValue(Style.Calm, isOverridden = false), flag.flow.first())
  }

  @Test(timeout = 10_000)
  fun `get does not wait for a read that resumes on the calling thread`() {
    every { remoteConfig.boolean("flag", false) } returns true
    val readStarted = CompletableDeferred<Unit>()
    val file = createTempDirectory().resolve("overrides.json").toFile().apply { writeText("{}") }
    val store = DataStoreFactory.create(ReadSignallingSerializer(readStarted)) { file }
    val mainThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    runBlocking(mainThread) {
      val flag = FeatureFlagFactory(remoteConfig, FeatureFlagOverridesStore(store), CoroutineScope(mainThread))
        .boolean(key = "flag", description = "")
      readStarted.await()

      assertEquals(true, flag.get())
    }
  }

  private class ReadSignallingSerializer(private val readStarted: CompletableDeferred<Unit>) :
    Serializer<Map<String, FeatureFlagOverride>> {

    override val defaultValue: Map<String, FeatureFlagOverride> = emptyMap()

    override suspend fun readFrom(input: InputStream): Map<String, FeatureFlagOverride> {
      readStarted.complete(Unit)
      return defaultValue
    }

    override suspend fun writeTo(
      t: Map<String, FeatureFlagOverride>,
      output: OutputStream,
    ) = Unit
  }
}
