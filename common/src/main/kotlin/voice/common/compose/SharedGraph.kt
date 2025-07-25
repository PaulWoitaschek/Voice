package voice.common.compose

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import voice.common.pref.DarkThemeStore

@ContributesTo(AppScope::class)
interface SharedGraph {

  @get:DarkThemeStore
  val useDarkThemeStore: DataStore<Boolean>
}
