package voice.core.ui

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import voice.core.datastore.PrefsDataMigration
import voice.core.datastore.VoiceDataStoreFactory

@SingleIn(AppScope::class)
class GridModeStore(store: DataStore<GridMode>) : DataStore<GridMode> by store {

  @Inject
  constructor(
    factory: VoiceDataStoreFactory,
    sharedPreferences: SharedPreferences,
  ) : this(
    factory.create(
      GridMode.serializer(),
      GridMode.FOLLOW_DEVICE,
      "gridMode",
      migrations = listOf(
        PrefsDataMigration(
          sharedPreferences,
          key = "gridView",
          getFromSharedPreferences = {
            when (sharedPreferences.getString("gridView", null)) {
              "LIST" -> GridMode.LIST
              "GRID" -> GridMode.GRID
              else -> GridMode.FOLLOW_DEVICE
            }
          },
        ),
      ),
    ),
  )
}
