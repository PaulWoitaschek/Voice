package de.ph1b.audiobook.persistence.pref

import io.reactivex.Observable
import kotlin.properties.ReadWriteProperty

abstract class Pref<T : Any> : ReadWriteProperty<Any, T> {

  @Suppress("LeakingThis")
  var value: T by this

  abstract val stream: Observable<T>
}
