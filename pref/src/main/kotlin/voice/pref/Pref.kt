package voice.pref

import androidx.datastore.core.DataStore
import kotlin.properties.ReadWriteProperty

abstract class Pref<T> :
  ReadWriteProperty<Any, T>,
  DataStore<T>
