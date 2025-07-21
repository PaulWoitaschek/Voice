package voice.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.room.migration.Migration
import androidx.test.core.app.ApplicationProvider
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import voice.common.AppScope
import voice.common.pref.DarkThemeStore

@SingleIn(AppScope::class)
@DependencyGraph(
  scope = AppScope::class,
)
interface TestComponent {

  val migrations: Set<@JvmSuppressWildcards Migration>

  @DependencyGraph.Factory
  interface Factory {

    fun create(
      @Provides
      @DarkThemeStore
      darkThemeStore: DataStore<Boolean>,
      @Provides
      context: Context,
    ): TestComponent
  }
}

internal fun allMigrations(): Array<Migration> {
  return createGraphFactory<TestComponent.Factory>()
    .create(
      darkThemeStore = MemoryDataStore(false),
      context = ApplicationProvider.getApplicationContext(),
    )
    .migrations.toTypedArray()
}
