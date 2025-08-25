package voice.navigation

import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializerOrNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import voice.navigation.Destination
import kotlin.reflect.KClass

@OptIn(InternalSerializationApi::class)
@RunWith(Parameterized::class)
class DestinationTest(val destinationClass: KClass<out Destination>) {

  /**
   * Navigation3 serializes the NavKeys, therefore all [voice.navigation.Destination.Compose]s must be
   * @Serializable.
   */
  @Test
  fun allDestinationsAreSerializable() {
    destinationClass.serializerOrNull().shouldNotBeNull()
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
        add(Destination.Dialog::class)
        addChildren(Destination.Dialog::class)
      }
    }
  }
}
