package voice.features.sibha

import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.navigation.Navigator

@ContributesTo(AppScope::class)
interface SibhaGraph {
  val sibhaCountStore: SibhaCountStore
  val sibhaReminderStore: SibhaReminderStore
  val navigator: Navigator
}

@BindingContainer
@ContributesTo(AppScope::class)
object SibhaProvider {

  @Provides
  @IntoSet
  fun sibhaNavEntryProvider(): NavEntryProvider<*> =
    NavEntryProvider<Destination.Sibha> { key ->
      NavEntry(key) {
        SibhaScreen()
      }
    }
}
