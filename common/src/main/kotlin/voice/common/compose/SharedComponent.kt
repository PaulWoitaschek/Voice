package voice.common.compose

import androidx.datastore.core.DataStore
import com.squareup.anvil.annotations.ContributesTo
import voice.common.AppScope
import voice.common.pref.DarkThemeStore

@ContributesTo(AppScope::class)
interface SharedComponent {

  @get:DarkThemeStore
  val useDarkThemeStore: DataStore<Boolean>
}
