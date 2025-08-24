package voice.common.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey

fun interface NavEntryProvider {
  fun create(
    key: NavKey,
    backStack: NavBackStack,
  ): NavEntry<NavKey>?
}
