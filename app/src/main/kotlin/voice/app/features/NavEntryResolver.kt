package voice.app.features

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import dev.zacsweers.metro.Inject
import voice.common.navigation.Destination
import voice.common.navigation.NavEntryProvider

@Inject
class NavEntryResolver(private val providers: Set<NavEntryProvider<*>>) {

  private val typedProviders = providers.associateBy { it.key }

  internal fun registeredClasses() = providers.map { it.key }

  fun <T : NavKey> create(
    key: T,
    backStack: NavBackStack,
  ): NavEntry<T> {
    key as Destination.Compose
    @Suppress("UNCHECKED_CAST")
    val provider = typedProviders[key::class]!! as NavEntryProvider<Destination.Compose>
    @Suppress("UNCHECKED_CAST")
    return provider.create(key, backStack) as NavEntry<T>
  }
}
