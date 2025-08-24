package voice.common.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import kotlin.reflect.KClass

class NavEntryProvider<T : Destination.Compose>(
  val key: KClass<T>,
  val create: (key: T, backStack: NavBackStack) -> NavEntry<NavKey>,
)

inline fun <reified T : Destination.Compose> NavEntryProvider(
  noinline create: (key: T, backStack: NavBackStack) -> NavEntry<NavKey>,
): NavEntryProvider<T> {
  return NavEntryProvider(T::class, create)
}
