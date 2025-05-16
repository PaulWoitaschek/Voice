package voice.common.compose

import com.squareup.anvil.annotations.ContributesTo
import voice.common.AppScope
import voice.common.pref.DarkThemeStore
import voice.pref.Pref

@ContributesTo(AppScope::class)
interface SharedComponent {

  @get:DarkThemeStore
  val useDarkThemeStore: Pref<Boolean>
}
