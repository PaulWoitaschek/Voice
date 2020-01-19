package de.ph1b.audiobook.persistence.pref

import de.ph1b.audiobook.misc.latestAsFlow
import io.reactivex.Observable
import kotlinx.coroutines.flow.Flow
import kotlin.properties.ReadWriteProperty

abstract class Pref<T : Any> : ReadWriteProperty<Any, T> {

  @Suppress("LeakingThis")
  var value: T by this

  abstract val stream: Observable<T>

  val flow: Flow<T> get() = stream.latestAsFlow()
}
