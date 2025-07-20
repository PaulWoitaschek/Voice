package voice.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.room.migration.Migration
import androidx.test.core.app.ApplicationProvider
import dagger.BindsInstance
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.createGraphFactory
import voice.common.AppScope
import voice.common.pref.DarkThemeStore
import javax.inject.Singleton

@Singleton
@DependencyGraph(
  scope = AppScope::class,
)
interface TestComponent {

  val migrations: Set<@JvmSuppressWildcards Migration>

  @DependencyGraph.Factory
  interface Factory {

    fun create(
      @BindsInstance
      @DarkThemeStore
      darkThemeStore: DataStore<Boolean>,
      @BindsInstance
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
