package de.ph1b.audiobook.prefs

import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlin.properties.ReadWriteProperty

abstract class Pref<T : Any> : ReadWriteProperty<Any, T> {

  @Suppress("LeakingThis")
  var value: T by this

  abstract val stream: Observable<T>

  val flow: Flow<T> get() = stream.toFlowable(BackpressureStrategy.LATEST).asFlow()
}
