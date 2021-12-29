package voice.common.compose

import com.squareup.anvil.annotations.ContributesTo
import de.paulwoitaschek.flowpref.Pref
import de.ph1b.audiobook.AppScope
import de.ph1b.audiobook.common.pref.PrefKeys
import javax.inject.Named

@ContributesTo(AppScope::class)
interface SharedComponent {

  @get:[Named(PrefKeys.DARK_THEME)]
  val useDarkTheme: Pref<Boolean>
}
