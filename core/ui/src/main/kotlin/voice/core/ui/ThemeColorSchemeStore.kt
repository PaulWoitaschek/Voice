package voice.core.ui

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import voice.core.datastore.VoiceDataStoreFactory

@SingleIn(AppScope::class)
class ThemeColorSchemeStore(store: DataStore<ThemeColorScheme>) : DataStore<ThemeColorScheme> by store {

  @Inject
  constructor(factory: VoiceDataStoreFactory) : this(
    factory.create(
      serializer = ThemeColorScheme.serializer(),
      fileName = "themeColorScheme",
      defaultValue = ThemeColorScheme.VoiceBlue,
    ),
  )
}
