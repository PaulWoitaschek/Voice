package voice.pref

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlin.properties.ReadWriteProperty

abstract class Pref<T> :
  ReadWriteProperty<Any, T>,
  DataStore<T> {

  @Suppress("LeakingThis")
  var value: T by this

  abstract fun setAndCommit(value: T)

  abstract fun delete(commit: Boolean = false)

  override suspend fun updateData(transform: suspend (t: T) -> T): T {
    val transformed = transform(value)
    value = transformed
    return transformed
  }
}

@Suppress("unused")
val <T : Any> Pref<T?>.flowNotNull: Flow<T>
  get() = data.filterNotNull()
