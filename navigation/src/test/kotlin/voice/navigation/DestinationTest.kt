package voice.navigation

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializerOrNull
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertNotNull

@OptIn(InternalSerializationApi::class)
@RunWith(Parameterized::class)
class DestinationTest(val destinationClass: KClass<out Destination>) {

  /**
   * Navigation3 serializes the NavKeys, therefore all [Destination.Compose]s must be
   * @Serializable.
   */
  @Test
  fun allDestinationsAreSerializable() {
    assertNotNull(destinationClass.serializerOrNull())
  }

  companion object {

    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data(): List<KClass<out Destination>> {
      return buildList {
        fun addChildren(from: KClass<out Destination>) {
          from.sealedSubclasses.forEach {
            add(it)
            addChildren(it)
          }
        }
        add(Destination.Compose::class)
        addChildren(Destination.Compose::class)
      }
    }
  }
}
