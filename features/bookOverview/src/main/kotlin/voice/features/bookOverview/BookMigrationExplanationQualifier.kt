package voice.features.bookOverview

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Qualifier

@Qualifier
annotation class BookMigrationExplanationQualifier

typealias BookMigrationExplanationShown = DataStore<@JvmSuppressWildcards Boolean>
