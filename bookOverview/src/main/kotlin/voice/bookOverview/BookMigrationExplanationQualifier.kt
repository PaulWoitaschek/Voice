package voice.bookOverview

import androidx.datastore.core.DataStore
import javax.inject.Qualifier

@Qualifier
annotation class BookMigrationExplanationQualifier

typealias BookMigrationExplanationShown = DataStore<@JvmSuppressWildcards Boolean>
