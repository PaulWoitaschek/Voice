package voice.data

import androidx.room.migration.Migration
import com.squareup.anvil.annotations.MergeComponent
import dagger.BindsInstance
import de.paulwoitaschek.flowpref.Pref
import de.paulwoitaschek.flowpref.inmemory.InMemoryPref
import voice.common.AppScope
import voice.common.pref.PrefKeys
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@MergeComponent(
  scope = AppScope::class,
)
interface TestComponent {

  val migrations: Set<@JvmSuppressWildcards Migration>

  @dagger.Component.Factory
  interface Factory {

    fun create(
      @BindsInstance
      @Named(PrefKeys.DARK_THEME)
      darkThemePref: Pref<Boolean>,
    ): TestComponent
  }
}

internal fun allMigrations(): Array<Migration> {
  return DaggerTestComponent.factory()
    .create(InMemoryPref(false))
    .migrations.toTypedArray()
}
