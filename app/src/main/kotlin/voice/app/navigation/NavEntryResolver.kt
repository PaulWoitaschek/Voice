package voice.app.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.metadata
import dev.zacsweers.metro.Inject
import voice.navigation.Destination
import voice.navigation.NavEntryProvider

@Inject
class NavEntryResolver(private val providers: Set<NavEntryProvider<*>>) {

  private val typedProviders = providers.associateBy { it.key }

  internal fun registeredClasses() = providers.map { it.key }

  fun create(key: Destination.Compose): NavEntry<Destination.Compose> {
    @Suppress("UNCHECKED_CAST")
    val provider = typedProviders[key::class]!! as NavEntryProvider<Destination.Compose>
    val navEntry = provider.create(key)
    return NavEntry(
      key = key,
      contentKey = navEntry.contentKey,
      metadata = navEntry.metadata + metadata {
        put(DestinationMetadataKey, key)
      },
    ) {
      navEntry.Content()
    }
  }
}

object DestinationMetadataKey : NavMetadataKey<Destination.Compose>
