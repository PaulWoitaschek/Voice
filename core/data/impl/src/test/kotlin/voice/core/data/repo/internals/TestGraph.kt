package voice.core.data.repo.internals

import androidx.room.migration.Migration
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraph

@SingleIn(AppScope::class)
@DependencyGraph(
  scope = AppScope::class,
)
internal interface TestGraph {

  val migrations: Set<@JvmSuppressWildcards Migration>
}

internal fun allMigrations(): Array<Migration> {
  return createGraph<TestGraph>().migrations.toTypedArray()
}
