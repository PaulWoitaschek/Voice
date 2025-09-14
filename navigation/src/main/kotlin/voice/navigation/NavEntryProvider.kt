package voice.navigation

import androidx.navigation3.runtime.NavEntry
import kotlin.reflect.KClass

class NavEntryProvider<T : Destination.Compose>(
  val key: KClass<T>,
  val create: (key: T) -> NavEntry<Destination.Compose>,
)

inline fun <reified T : Destination.Compose> NavEntryProvider(
  noinline create: (key: T) -> NavEntry<Destination.Compose>,
): NavEntryProvider<T> {
  return NavEntryProvider(T::class, create)
}
