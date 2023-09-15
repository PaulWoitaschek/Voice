package voice.common.compose

import com.squareup.anvil.annotations.ContributesTo
import de.paulwoitaschek.flowpref.Pref
import javax.inject.Named
import voice.common.AppScope
import voice.common.pref.PrefKeys

@ContributesTo(AppScope::class)
interface SharedComponent {

  @get:[
  Named(PrefKeys.DARK_THEME)
  ]
  val useDarkTheme: Pref<Boolean>
}
